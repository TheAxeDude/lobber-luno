package za.co.oneeyesquared.lobber.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;

/**
 * Created by Balitha on 2017-11-09.
 */
public class OrderBookUpdateMessage {
    @Override
    public String toString() {
        return "OrderBookUpdateMessage{" +
                "sequence=" + sequence +
                ", trade_updates=" + Arrays.toString(trade_updates) +
                ", createdOrder=" + createdOrder +
                ", deletedOrder=" + deletedOrder +
                ", timestamp=" + timestamp +
                '}';
    }

    public OrderBookUpdateMessage(){

    }

    public OrderBookUpdateMessage(int sequence, TradeUpdate[] trade_updates, Order createdOrder, Order deletedOrder, long timestamp) {
        this.sequence = sequence;
        this.trade_updates = trade_updates;
        this.createdOrder = createdOrder;
        this.deletedOrder = deletedOrder;
        this.timestamp = timestamp;
    }

    public int getSequence() {
        return sequence;
    }

    public void setSequence(int sequence) {
        this.sequence = sequence;
    }

    public TradeUpdate[] getTrade_updates() {
        return trade_updates;
    }

    @JsonProperty("trade_updates")
    public void setTrade_updates(TradeUpdate[] trade_updates) {
        this.trade_updates = trade_updates;
    }

    public Order getCreatedOrder() {
        return createdOrder;
    }

    @JsonProperty("create_update")
    public void setCreatedOrder(Order createdOrder) {
        this.createdOrder = createdOrder;
    }

    public Order getDeletedOrder() {
        return deletedOrder;
    }

    @JsonProperty("delete_update")
    public void setDeletedOrder(Order deletedOrder) {
        this.deletedOrder = deletedOrder;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    private int
            sequence;
    private TradeUpdate
            [] trade_updates;
    private Order
            createdOrder;
    private Order deletedOrder;

    private long timestamp;


}
