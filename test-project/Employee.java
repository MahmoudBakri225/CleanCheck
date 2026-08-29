package com.mycompany.cleancheck;

public class Employee {

    // ===== Encapsulation problems =====
    public int id;                      // مش private
    protected double Salary;            // مش private + اسم يبدأ بحرف كبير + فلوس
    String department;                  // package-private

    private String name;                // صح
    private int _code;                  // يبدأ بـ underscore

    // ===== Data Type problems =====
    private float rating;               // float
    private double cost;                // double مع فلوس

    // ===== Data Type Syntax problems =====
    private Double wrongDouble;         // Double بدل double
    private Char grade;                 // Char بدل char

    // ===== سطر فيه أقواس متوازنة (open == close) =====
    // ده بيكشف الـ bug في VariableNameRule: السطر بيتعمله continue
    // وميتفحصش خالص عشان بيحتوي على "(" و ")" بنفس العدد
    private String label = getDefaultLabel();

    // Constructor فاضي (Empty Method)
    public Employee() {
    }

    // Method فيها parameters كتير (Too Many Parameters)
    public void configure(String name, int id, String department, double salary, String email, String phone) {
        this.name = name;
        this.id = id;
    }

    // Empty Method
    public void reset() {
    }

    // Empty If
    public void validate() {
        if (id > 0) {
        }
    }

    
    public void badLoop1() {
        while (true) {
            // مفيش break
        }
    }

    
    public void badLoop2() {
        for (;;) {
            // مفيش break
        }
    }

    private String getDefaultLabel() {
        return "N/A";
    }

    
    public void printInfo() {
        System.out.println("Employee: " + name);
    }
}
