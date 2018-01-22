package za.co.oneeyesquared.lobber.models;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by Balitha on 2017-11-09.
 */
public class TradeUpdate {
    private double base;
    private double counter;
    private String orderID;
    private String takerOrderID;
    private String makerOrderID;

    public TradeUpdate(double base, double counter, String orderID, String takerOrderID, String makerOrderID) {
        this.base = base;
        this.counter = counter;
        this.orderID = orderID;
        this.takerOrderID = takerOrderID;
        this.makerOrderID = makerOrderID;
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

    public String getMakerOrderID() {
        return makerOrderID;
    }

    @JsonProperty("maker_order_id")
    public void setMakerOrderID(String makerOrderID) {
        this.makerOrderID = makerOrderID;
    }

    public String getTakerOrderID() {
        return takerOrderID;
    }


    @JsonProperty("taker_order_id")
    public void setTakerOrderID(String takerOrderID) {
        this.takerOrderID = takerOrderID;
    }
}
