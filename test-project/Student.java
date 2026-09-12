package com.mycompany.cleancheck;

public class Student {

    public int age;
    protected String address;
    String phone;
    private String name;

    private String MyName;
    private int Score;
    public double Price;

    private float height;
    private double salary;
    private double balance;

    private Int wrongInt;
    private Boolean isActive;
    private string wrongString;

    // Constructor فاضي (Empty Method)
    public Student() {
    }

    public void setData(String name, int age, String address, String phone, String email, String city) {
        this.name = name;
        this.age = age;
    }

    public void emptyMethod() {
    }

    // Empty If
    public void checkAge() {
        if (age > 18) {
        }
    }

    public void badLoop1() {
        while (true) {

        }
    }

    public void badLoop2() {
        for (;;) {

        }
    }

    public void printInfo() {
        System.out.println("Name: " + name);
    }
}
