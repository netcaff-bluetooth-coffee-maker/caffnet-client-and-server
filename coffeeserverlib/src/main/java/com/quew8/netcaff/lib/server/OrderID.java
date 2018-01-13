package com.quew8.netcaff.lib.server;

/**
 * @author Quew8
 */

public class OrderID extends Struct {
    private int orderId;

    OrderID(int orderId) {
        this.orderId = orderId;
    }

    public OrderID() {
        this(0);
    }

    public boolean isEmpty() {
        return orderId == 0;
    }

    protected int getId() {
        return orderId;
    }

    @Override
    protected String check() {
        if(this.orderId < 0) {
            return "Negative order id";
        }
        if(this.isEmpty()) {
            return "Empty order id";
        }
        if(this.orderId >= (2 << 4)) {
            return "Order id greater than representable range";
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;

        OrderID orderID = (OrderID) o;

        return orderId == orderID.orderId;
    }

    public String getPrettyString() {
        return Integer.toString(orderId);
    }

    @Override
    public int hashCode() {
        return orderId;
    }

    @Override
    public String toString() {
        return "OrderID{" +
                "orderId=" + orderId +
                '}';
    }
}
