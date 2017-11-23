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
    private Map<String, Vector<Order>> orders;

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(OrderBookUpdateMessage.class, orderBookUpdateMessage -> {
                    log.info("Order book update received: " + orderBookUpdateMessage.getSequence());
                    log.debug(orderBookUpdateMessage.toString());
                    if(orderBookUpdateMessage.getTrade_updates() != null) {
                        for (TradeUpdate tradeUpdate : orderBookUpdateMessage.getTrade_updates()) {
                            log.info("Trade update: " + tradeUpdate.getOrderID());
                        }
                    }

                    /*NOTE: IT IS Very important that the "create" occurs after the "update". If it is before, then partial fills will be incorrect.
                    *SEE: https://www.luno.com/en/api#streaming-websocket
                    * */
                    if(orderBookUpdateMessage.getCreatedOrder() != null) {
                        log.info("order to be created: " + orderBookUpdateMessage.getCreatedOrder().toString());
                        log.debug(orderBookUpdateMessage.getCreatedOrder().toString());
                        log.debug("BEFORE: {}S size: {}", orderBookUpdateMessage.getCreatedOrder().getType()  , this.orders.get(orderBookUpdateMessage.getCreatedOrder().getType()+"S").size());
                        this.orders.get(orderBookUpdateMessage.getCreatedOrder().getType()+"S").add(orderBookUpdateMessage.getCreatedOrder());
                        log.debug("AFTER {}S size: {}", orderBookUpdateMessage.getCreatedOrder().getType(), this.orders.get(orderBookUpdateMessage.getCreatedOrder().getType()+"S").size());

                    }
                    if(orderBookUpdateMessage.getDeletedOrder() != null) {
                        log.debug(orderBookUpdateMessage.getDeletedOrder().toString());
                        log.info("order to be deleted: " + orderBookUpdateMessage.getDeletedOrder().toString());
                        log.debug("BEFORE: {} BIDS / {} ASKS", this.orders.get("BIDS").size(), this.orders.get("ASKS").size());
                        this.orders.get("ASKS").removeIf(i -> i.getOrderID().equals(orderBookUpdateMessage.getDeletedOrder().getOrderID()));
                        this.orders.get("BIDS").removeIf(i -> i.getOrderID().equals(orderBookUpdateMessage.getDeletedOrder().getOrderID()));
                        log.debug("AFTER: {} BIDS / {} ASKS", this.orders.get("BIDS").size(), this.orders.get("ASKS").size());
                    }
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
        this.orders.put("BIDS", new Vector<>());
        this.orders.put("ASKS", new Vector<>());

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
        this.orders.get("ASKS").addAll(asks);
    }
    public void setBids(ArrayList<Order> bids) {
        this.orders.get("BIDS").addAll(bids);
    }

    public void setOrders(ArrayList<Order> orders, String type) {
        this.orders.get(type).addAll(orders);
    }
}
