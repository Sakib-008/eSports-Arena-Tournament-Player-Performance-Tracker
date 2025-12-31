package com.esports.arena.model;

public class User {
    public String email;
    public String role;

    public User() {} 

    public User(String email, String role) {
        this.email = email;
        this.role = role;
    }
}