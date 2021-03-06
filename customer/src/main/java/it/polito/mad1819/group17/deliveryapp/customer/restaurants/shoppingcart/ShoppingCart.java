package it.polito.mad1819.group17.deliveryapp.customer.restaurants.shoppingcart;

import android.util.Log;

import java.util.HashMap;
import java.util.Vector;

import it.polito.mad1819.group17.deliveryapp.common.orders.ShoppingItem;

public class ShoppingCart {

    private Vector<ShoppingItem> items;
    private int counter;
    private double total_price;

    public int getCounter() {
        return counter;
    }

    public double getTotal_price() {
        return total_price;
    }

    public ShoppingCart() {
        items = new Vector<ShoppingItem>();
        counter=0;
        total_price=0;
    }

    public boolean add(ShoppingItem newItem){
        //if new element return TRUE
        //if old element return FALSE
        int currIndex = this.items.indexOf(newItem);
        boolean retValue=false;

        if(currIndex == -1){
            Log.d("if_else","if");
            items.addElement(newItem);
            retValue = true;
        } else {
            Log.d("if_else","else");
            ShoppingItem currItem = items.elementAt(currIndex);
            currItem.addOne();
            retValue = false;
        }
        Log.d("price",newItem.getPrice().toString());
        total_price += newItem.getPrice();
        counter += 1;
        Log.d("price_tot_and_quantity", total_price +";"+ counter);
        return retValue;
    }

    public void remove(ShoppingItem oldItem){
        int currIndex = items.indexOf(oldItem);
        if (currIndex == -1){
            return;
        }else{
            ShoppingItem currItem = items.elementAt(currIndex);
            total_price -= oldItem.getPrice();
            currItem.subtractOne();
            counter -= 1;
            if (currItem.getQuantity() == 0) {
                items.removeElementAt(currIndex);
            }
        }
    }

    public HashMap<String,ShoppingItem> getItemsMap() {
        HashMap<String,ShoppingItem> retValue = new HashMap<>();
        for (ShoppingItem item : this.items){
            retValue.put(item.getId(), item);
        }
        return retValue;
    }

    public void clear(){
        items.clear();
        total_price=0;
        counter=0;
    }
}
