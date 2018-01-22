package za.co.oneeyesquared.lobber.actors;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sun.org.apache.xpath.internal.operations.Or;
import za.co.oneeyesquared.lobber.models.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Collectors;

/**
 * Created by Balitha on 2017-11-11.
 */
public class LimitOrderBook extends AbstractActor {

    static public Props props() {
        return Props.create(LimitOrderBook.class, () -> new LimitOrderBook());
    }

    private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    private long startingSequence;
    private Map<Long, OrderBookUpdateMessage> appliedUpdates;

    private double spread;
    private double marketPrice;

    public double getSpread() {
        return spread;
    }

    public void setSpread(double spread) {
        this.spread = spread;
    }

    public double getMarketPrice() {
        return marketPrice;
    }

    public void setMarketPrice(double marketPrice) {
        this.marketPrice = marketPrice;
    }
    //    private Map<String, Vector<Order>> orders;

    //String is one of (ASKS|BIDS) -> <OrderID, Order>
    private Map<String, ConcurrentSkipListMap<String, Order>> orders;

//    private Map<String, Map<>>
    private Map<String, TreeSet<Order>> ordersGroupedByPriceAndSorted;
    @Override
    public Receive createReceive() {

        return receiveBuilder()
                .match(OrderBookUpdateMessage.class, orderBookUpdateMessage -> {
                    log.info("Order book update received: " + orderBookUpdateMessage.getSequence());
                    log.debug(orderBookUpdateMessage.toString());
                    if(orderBookUpdateMessage.getTrade_updates() != null) {
                        for (TradeUpdate tradeUpdate : orderBookUpdateMessage.getTrade_updates()) {
                            String updateSide = "ASKS";
                            log.info("Trade update: " + tradeUpdate.getOrderID());

                            Optional<Order> order = this.orders.get(updateSide).values().stream().filter(o -> o.getOrderID().equals(tradeUpdate.getOrderID())).findFirst();
                            if(!order.isPresent()){
                                updateSide = "BIDS";
                                order = this.orders.get(updateSide).values().stream().filter(o -> o.getOrderID().equals(tradeUpdate.getOrderID())).findFirst();
                            }

                            if(order.isPresent()){
                                double newVolume = order.get().getVolume() - tradeUpdate.getBase();
                                ordersGroupedByPriceAndSorted.get(updateSide).remove(order.get());
                                order.get().setVolume(newVolume);

                                //dont return to order book if the volume is zero
                                if(newVolume > 0d)
                                    ordersGroupedByPriceAndSorted.get(updateSide).add(order.get());

                            }
                        }
                    }


                    /*NOTE: IT IS Very important that the "create" occurs after the "update". If it is before, then partial fills will be incorrect.
                    *SEE: https://www.luno.com/en/api#streaming-websocket
                    * */
                    if(orderBookUpdateMessage.getCreatedOrder() != null) {
                        String orderType = orderBookUpdateMessage.getCreatedOrder().getType()+"S";
                        log.info("order to be created: " + orderBookUpdateMessage.getCreatedOrder().toString());
                        log.debug(orderBookUpdateMessage.getCreatedOrder().toString());
                        log.debug("BEFORE: {}S size: {}", orderBookUpdateMessage.getCreatedOrder().getType()  , this.orders.get(orderBookUpdateMessage.getCreatedOrder().getType()+"S").size());
                        this.orders.get(orderType).put(orderBookUpdateMessage.getCreatedOrder().getOrderID(), orderBookUpdateMessage.getCreatedOrder());
                        log.debug("AFTER {}S size: {}", orderBookUpdateMessage.getCreatedOrder().getType(), this.orders.get(orderBookUpdateMessage.getCreatedOrder().getType()+"S").size());
                        log.debug("SORTED BEFORE: {}S size: {}", orderBookUpdateMessage.getCreatedOrder().getType()  , this.orders.get(orderBookUpdateMessage.getCreatedOrder().getType()+"S").size());
                        this.ordersGroupedByPriceAndSorted.get(orderType).add(orderBookUpdateMessage.getCreatedOrder());
                        log.debug("SORTED AFTER {}S size: {}", orderBookUpdateMessage.getCreatedOrder().getType(), this.orders.get(orderBookUpdateMessage.getCreatedOrder().getType()+"S").size());

                    }
                    if(orderBookUpdateMessage.getDeletedOrder() != null) {
                        log.debug(orderBookUpdateMessage.getDeletedOrder().toString());
                        log.info("order to be deleted: " + orderBookUpdateMessage.getDeletedOrder().toString());
                        log.debug("BEFORE: {} BIDS / {} ASKS", this.orders.get("BIDS").size(), this.orders.get("ASKS").size());
                        this.orders.get("ASKS").remove(orderBookUpdateMessage.getDeletedOrder().getOrderID());
                        this.orders.get("BIDS").remove(orderBookUpdateMessage.getDeletedOrder().getOrderID());
                        log.debug("AFTER: {} BIDS / {} ASKS", this.orders.get("BIDS").size(), this.orders.get("ASKS").size());

                        log.debug("SORTED BEFORE: {} BIDS / {} ASKS", this.ordersGroupedByPriceAndSorted.get("BIDS").size(), this.ordersGroupedByPriceAndSorted.get("ASKS").size());
                        this.ordersGroupedByPriceAndSorted.get("ASKS").removeIf(o -> o.getOrderID().equals(orderBookUpdateMessage.getDeletedOrder().getOrderID()));
                        this.ordersGroupedByPriceAndSorted.get("BIDS").removeIf(o -> o.getOrderID().equals(orderBookUpdateMessage.getDeletedOrder().getOrderID()));
                        log.debug("SORTEDAFTER: {} BIDS / {} ASKS", this.ordersGroupedByPriceAndSorted.get("BIDS").size(), this.ordersGroupedByPriceAndSorted.get("ASKS").size());
                    }
                    //appliedUpdates.put(orderBookUpdateMessage.getSequence(), orderBookUpdateMessage);
                    log.info("Spread calc: {} - {} = {}", ordersGroupedByPriceAndSorted.get("ASKS").first().getPrice(), ordersGroupedByPriceAndSorted.get("BIDS").last().getPrice(), ordersGroupedByPriceAndSorted.get("ASKS").first().getPrice() - ordersGroupedByPriceAndSorted.get("BIDS").last().getPrice());
                    this.setSpread(ordersGroupedByPriceAndSorted.get("ASKS").first().getPrice() - ordersGroupedByPriceAndSorted.get("BIDS").last().getPrice());
                    this.setMarketPrice((ordersGroupedByPriceAndSorted.get("ASKS").first().getPrice() + ordersGroupedByPriceAndSorted.get("BIDS").last().getPrice())/2);
                    log.info("Spread: {}", this.getSpread());
                    log.info("Market Price: {}", this.getMarketPrice());
                    getSender().tell(new UpdateCompletedMessage(orderBookUpdateMessage.getSequence()), getSelf());
                })
                .match(LimitOrderBookMessage.class, limitOrderBookMessage -> {
                    log.info("Limit order book received: " + limitOrderBookMessage.getStartingSequence());
                    log.debug(limitOrderBookMessage.toString());
                    this.setStartingSequence(limitOrderBookMessage.getStartingSequence());
                    this.setAsks(limitOrderBookMessage.getAsks());
                    this.setBids(limitOrderBookMessage.getBids());

                    getSender().tell(new UpdateCompletedMessage(this.getStartingSequence()), getSelf());
                })
                .matchAny(o -> log.info("received unknown message"))
                .build();
    }

    public LimitOrderBook() {
        this.orders = new ConcurrentHashMap<>();
        this.orders.put("ASKS", new ConcurrentSkipListMap<>());
        this.orders.put("BIDS", new ConcurrentSkipListMap<>());

        this.ordersGroupedByPriceAndSorted = new ConcurrentHashMap<>();
        this.ordersGroupedByPriceAndSorted.put("ASKS", new TreeSet<>());
        this.ordersGroupedByPriceAndSorted.put("BIDS", new TreeSet<>());

    }

    public long getStartingSequence() {
        return startingSequence;
    }

    @JsonProperty("sequence")
    public void setStartingSequence(long startingSequence) {
        this.startingSequence = startingSequence;
    }

    public Map<Long, OrderBookUpdateMessage> getAppliedUpdates() {
        return appliedUpdates;
    }

    public void setAppliedUpdates(Map<Long, OrderBookUpdateMessage> appliedUpdates) {
        this.appliedUpdates = appliedUpdates;
    }

    @Override
    public String toString() {
        return "LimitOrderBook{" +
                "startingSequence=" + startingSequence +
                ", appliedUpdates=" + appliedUpdates +
                ", asks=" + orders.get("ASKS") +
                ", bids=" + orders.get("BIDS") +
                '}';
    }

    public void setAsks(ArrayList<Order> asks) {
        asks.stream().forEach(a -> {
                this.orders.get("ASKS").put(a.getOrderID(), a);
                this.ordersGroupedByPriceAndSorted.get("ASKS").add(a);
            });

    }
    public void setBids(ArrayList<Order> bids) {
        bids.stream().forEach(b -> {
            this.orders.get("BIDS").put(b.getOrderID(), b);
            this.ordersGroupedByPriceAndSorted.get("BIDS").add(b);
        });
    }
}
