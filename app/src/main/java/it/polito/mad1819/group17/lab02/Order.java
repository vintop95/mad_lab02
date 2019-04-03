package it.polito.mad1819.group17.lab02;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

public class Order implements Comparable, Serializable {

    public final static String STATE1 = "Accepted";
    public final static String STATE2 = "In preparation";
    public final static String STATE3 = "Delivering";

    private int number;
    private String customer_name;
    private String customer_phone;
    private String delivery_date;
    private String delivery_time;
    private HashMap<String, String> state_stateTime;
    private HashMap<String, Integer> item_itemQuantity;



    private Date delivery_timestamp;




    public Order(int number, String customer_name, String customer_phone, String delivery_time, String delivery_date, HashMap<String, String> state_stateTime, HashMap<String, Integer> item_itemQuantity) {
        this.number = number;
        this.customer_name = customer_name;
        this.customer_phone = customer_phone;
        this.delivery_time = delivery_time;
        this.delivery_date = delivery_date;
        this.state_stateTime = state_stateTime;
        this.item_itemQuantity = item_itemQuantity;

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        try {
            this.delivery_timestamp = simpleDateFormat.parse(delivery_date + " " + delivery_time);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public void setItem_itemQuantity(HashMap<String, Integer> item_itemQuantity) {
        this.item_itemQuantity = item_itemQuantity;
    }

    public Date getDelivery_timestamp() {
        return delivery_timestamp;
    }

    public HashMap<String, String> getState_stateTime() {
        return state_stateTime;
    }

    public void setState_stateTime(HashMap<String, String> state_stateTime) {
        this.state_stateTime = state_stateTime;
    }

    public HashMap<String, Integer> getItem_itemQuantity() {
        return item_itemQuantity;
    }

    /*
    public void setItem_itemQuantity(HashMap<String, Integer> item_itemQuantity) {
        this.item_itemQuantity = item_itemQuantity;
    }*/


    public String getCurrentState() {
        if (state_stateTime.get(STATE3) != null)
            return STATE3;
        else if (state_stateTime.get(STATE2) != null)
            return STATE2;
        else
            return STATE1;
    }

    public boolean moveToNextState() {
        if (getCurrentState() == STATE3)
            return false;
        else {
            SimpleDateFormat formatter = new SimpleDateFormat("HH:mm");
            String currentTime = formatter.format(new Date());

            if (getCurrentState() == STATE1)
                state_stateTime.put(STATE2, currentTime);
            else if (getCurrentState() == STATE2)
                state_stateTime.put(STATE3, currentTime);
            return true;
        }
    }

    public int getTotalItemsQuantity() {
        int totOrderedItems = 0;
        for (String orderedItem : item_itemQuantity.keySet())
            totOrderedItems += item_itemQuantity.get(orderedItem);
        return totOrderedItems;
    }

    /*public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }*/

    public String getCustomer_name() {
        return customer_name;
    }

    public void setCustomer_name(String customer_name) {
        this.customer_name = customer_name;
    }

    public String getDelivery_time() {
        return delivery_time;
    }

    public void setDelivery_time(String delivery_time) {
        this.delivery_time = delivery_time;
    }

    public String getDelivery_date() {
        return delivery_date;
    }

    public void setDelivery_date(String delivery_date) {
        this.delivery_date = delivery_date;
    }

    /*public HashMap<String, Integer> getOrderedItems_orderedQty() {
        return orderedItems_orderedQty;
    }

    public void setOrderedItems_orderedQty(HashMap<String, Integer> orderedItems_orderedQty) {
        this.orderedItems_orderedQty = orderedItems_orderedQty;
    }

    public HashMap<String, String> getState_stateTime() {
        return state_stateTime;
    }

    public void setState_stateTime(HashMap<String, String> state_stateTime) {
        this.state_stateTime = state_stateTime;
    }*/

    public Order(int number, String user_phone) {
        this.number = number;
        this.customer_phone = user_phone;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public String getCustomer_phone() {
        return customer_phone;
    }


    public void setCustomer_phone(String customer_phone) {
        this.customer_phone = customer_phone;
    }


    @Override
    public int compareTo(Object o) {
        if (this.getCurrentState() == STATE3)
            return +1;
        else
            return delivery_timestamp.compareTo(((Order) o).getDelivery_timestamp());
    }

    public String getStateHistoryToString(){
        String state_history = "";
        switch (getCurrentState()) {
            case STATE1:
                state_history = getState_stateTime().get(Order.STATE1) + " " + Order.STATE1;
                state_history += System.lineSeparator() + "--:--" + " " + Order.STATE2;
                state_history += System.lineSeparator() + "--:--" + " " + Order.STATE3;
                break;
            case STATE2:
                state_history = getState_stateTime().get(Order.STATE1) + "  " + Order.STATE1;
                state_history += System.lineSeparator() + getState_stateTime().get(Order.STATE2) + "  " + Order.STATE2;
                state_history += System.lineSeparator() + "--:--" + "  " + Order.STATE3;
                break;
            case STATE3:
                state_history = getState_stateTime().get(Order.STATE1) + "  " + Order.STATE1;
                state_history += System.lineSeparator() + getState_stateTime().get(Order.STATE2) + "  " + Order.STATE2;
                state_history += System.lineSeparator() + getState_stateTime().get(Order.STATE3) + "  " + Order.STATE3;
                break;
        }
        return state_history;
    }
}
