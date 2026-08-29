package com.mycompany.cleancheck;

public class Student {

    // ===== Encapsulation problems =====
    public int age;                    // مش private
    protected String address;          // مش private
    String phone;                      // package-private (مش private)
    private String name;               // صح

    // ===== Naming Convention + Variable Name problems =====
    private String MyName;             // يبدأ بحرف كبير
    private int Score;                 // يبدأ بحرف كبير
    public double Price;               // يبدأ بحرف كبير + فلوس

    // ===== Data Type problems =====
    private float height;              // استخدام float
    private double salary;             // double مع فلوس
    private double balance;            // double مع فلوس

    // ===== Data Type Syntax problems =====
    private Int wrongInt;              // Int بدل int
    private Boolean isActive;          // Boolean بدل boolean
    private string wrongString;        // string بدل String

    // Constructor فاضي (Empty Method)
    public Student() {
    }

    // Method فيها parameters كتير (Too Many Parameters)
    public void setData(String name, int age, String address, String phone, String email, String city) {
        this.name = name;
        this.age = age;
    }

    // Empty Method
    public void emptyMethod() {
    }

    // Empty If
    public void checkAge() {
        if (age > 18) {
        }
    }

    // Infinite Loop - while(true)
    public void badLoop1() {
        while (true) {
            // مفيش break
        }
    }

    // Infinite Loop - for(;;)
    public void badLoop2() {
        for (;;) {
            // مفيش break
        }
    }

    // Method عادية (صح)
    public void printInfo() {
        System.out.println("Name: " + name);
    }
}
