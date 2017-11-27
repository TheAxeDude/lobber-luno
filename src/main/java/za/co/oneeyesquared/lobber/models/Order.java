package za.co.oneeyesquared.lobber.models;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by Balitha on 2017-11-09.
 */
public class Order implements Comparable{
    private String orderID;
    private String type;
    private double price;
    private double volume;

    public Order(){


    }

    public Order(String orderID, String type, double price, double volume) {
        this.orderID = orderID;
        this.type = type;
        this.price = price;
        this.volume = volume;
    }

    public String getOrderID() {
        return orderID;
    }

    @JsonProperty("order_id")
    public void setOrderID(String orderID) {
        this.orderID = orderID;
    }


    @JsonProperty("id")
    public void setOrderID_id(String id) {
        this.orderID = id;
    }


    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public double getVolume() {
        return volume;
    }

    public void setVolume(double volume) {
        this.volume = volume;
    }

    @Override
    public String toString() {
        return "Order{" +
                "orderID='" + orderID + '\'' +
                ", type='" + type + '\'' +
                ", price=" + price +
                ", volume=" + volume +
                '}';
    }

    @Override
    public int compareTo(Object o) {
        if (this.getPrice() < ((Order)o).getPrice())
            return -1;

        else if (this.getPrice() > ((Order)o).getPrice())
            return 1;

        else
            return 0;
    }
}
