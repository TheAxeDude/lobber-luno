package za.co.oneeyesquared.lobber.models;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by Balitha on 2017-11-09.
 */
public class TradeUpdate {
    private double base;
    private double counter;
    private String orderID;

    public TradeUpdate(double base, double counter, String orderID) {
        this.base = base;
        this.counter = counter;
        this.orderID = orderID;
    }

    public TradeUpdate(){

    }

    public double getBase() {

        return base;
    }

    public void setBase(double base) {
        this.base = base;
    }

    public double getCounter() {
        return counter;
    }

    public void setCounter(double counter) {
        this.counter = counter;
    }

    public String getOrderID() {
        return orderID;
    }

    @JsonProperty("order_id")
    public void setOrderID(String orderID) {
        this.orderID = orderID;
    }
}
