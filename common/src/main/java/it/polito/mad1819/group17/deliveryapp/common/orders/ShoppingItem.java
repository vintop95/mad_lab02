package it.polito.mad1819.group17.deliveryapp.common.orders;

import java.io.Serializable;

public class ShoppingItem implements Serializable {
    private String id = "";
    private String name = "";
    private Double price = 0.0;
    private int quantity = 0;

    public ShoppingItem() {
    }

    public ShoppingItem(String id, String name, Double price, int quantity) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.quantity = quantity;
    }

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }

    public int getQuantity() {
        return quantity;
    }
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public Double getPrice() {
        return price;
    }
    public void setPrice(Double price) {
        this.price = price;
    }

    public void addOne(){this.quantity = this.quantity + 1;}
    public void add(int i){this.quantity = this.quantity + i;}

    public void subtractOne(){if(this.quantity>0)this.quantity = this.quantity - 1;}
    public void subtract(int i){
        this.quantity = this.quantity - i;
        if(this.quantity <0)this.quantity = 0;
    }

    // The equals method does something a little dirty here, it only
    // compares the item names and item costs. Technically, this is
    // not the way that equals was intended to work.
    public boolean equals(Object other)
    {
        if (this == other) return true;

        if (!(other instanceof ShoppingItem))
            return false;

        ShoppingItem otherItem =
                (ShoppingItem) other;

        return (id.equals(otherItem.id));
    }

}

