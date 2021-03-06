package it.polito.mad1819.group17.deliveryapp.common;

import java.io.Serializable;

public class Customer implements Serializable {
    private String id = "";
    private String image_path = "";
    private String name = "";
    private String phone = "";
    private String mail = "";
    private String address = "";
    private String bio = "";

    public Customer() {

    }

    public Customer(String id, String image_path, String name, String phone, String mail, String address, String bio) {
        this.id = id;
        this.image_path = image_path;
        this.name = name;
        this.phone = phone;
        this.mail = mail;
        this.address = address;
        this.bio = bio;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getImage_path() {
        return image_path;
    }

    public void setImage_path(String image_path) {
        this.image_path = image_path;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getMail() {
        return mail;
    }

    public void setMail(String mail) {
        this.mail = mail;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }
}
