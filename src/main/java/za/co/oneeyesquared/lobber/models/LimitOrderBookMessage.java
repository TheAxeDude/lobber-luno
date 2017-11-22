package za.co.oneeyesquared.lobber.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Map;

/**
 * Created by Balitha on 2017-11-11.
 */
public class LimitOrderBookMessage {

    private long startingSequence;
    private ArrayList<Order> asks;
    private ArrayList<Order> bids;



    public LimitOrderBookMessage() {
    }

    public LimitOrderBookMessage(long startingSequence, ArrayList<Order> asks, ArrayList<Order> bids) {
        this.startingSequence = startingSequence;
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
                ", asks=" + asks +
                ", bids=" + bids +
                '}';
    }
}
