package za.co.oneeyesquared.lobber.actors;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.fasterxml.jackson.annotation.JsonProperty;
import za.co.oneeyesquared.lobber.models.LimitOrderBookMessage;
import za.co.oneeyesquared.lobber.models.Order;
import za.co.oneeyesquared.lobber.models.OrderBookUpdateMessage;
import za.co.oneeyesquared.lobber.models.TradeUpdate;

import java.util.ArrayList;
import java.util.Map;

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
    private ArrayList<Order> asks;
    private ArrayList<Order> bids;



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
                    if(orderBookUpdateMessage.getCreatedOrder() != null) {
                        log.info("order created: " + orderBookUpdateMessage.getCreatedOrder().toString());

                    }
                    if(orderBookUpdateMessage.getDeletedOrder() != null) {
                        log.info("order deleted: " + orderBookUpdateMessage.getDeletedOrder().toString());
                    }

                })
                .match(LimitOrderBookMessage.class, limitOrderBookMessage -> {
                    log.info("Limit order book received: " + limitOrderBookMessage.getStartingSequence());
                    log.debug(limitOrderBookMessage.toString());
                    this.setStartingSequence(limitOrderBookMessage.getStartingSequence());
                    this.setAsks(limitOrderBookMessage.getAsks());
                    this.setBids(limitOrderBookMessage.getBids());

                })
                .matchAny(o -> log.info("received unknown message"))
                .build();
    }

    public LimitOrderBook() {
    }

    public LimitOrderBook(long startingSequence, Map<Long, OrderBookUpdateMessage> appliedUpdates, ArrayList<Order> asks, ArrayList<Order> bids) {
        this.startingSequence = startingSequence;
        this.appliedUpdates = appliedUpdates;
        this.asks = asks;
        this.bids = bids;
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

    public ArrayList<Order> getAsks() {
        return asks;
    }

    public void setAsks(ArrayList<Order> asks) {
        this.asks = asks;
    }

    public ArrayList<Order> getBids() {
        return bids;
    }

    public void setBids(ArrayList<Order> bids) {
        this.bids = bids;
    }

    @Override
    public String toString() {
        return "LimitOrderBook{" +
                "startingSequence=" + startingSequence +
                ", appliedUpdates=" + appliedUpdates +
                ", asks=" + asks +
                ", bids=" + bids +
                '}';
    }
}
