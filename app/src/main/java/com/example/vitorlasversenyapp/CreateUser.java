package com.example.vitorlasversenyapp;
public class CreateUser
{
    public CreateUser()
    {

    }
    public String name;

    public CreateUser(String name, String email, String password, String code, String issharing, String lat, String lng, String imageUrl,String userid) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.code = code;
        this.issharing = issharing;
        this.lat = lat;
        this.lng = lng;
        this.imageUrl = imageUrl;
        this.userid = userid;
    }

    public String email;
    public String password;
    public String code;
    public String issharing;
    public String lat;
    public String lng;
    public String imageUrl;
    public String userid;
}
