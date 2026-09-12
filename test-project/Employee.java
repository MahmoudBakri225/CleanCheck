package com.mycompany.cleancheck;

public class Employee {

    public int id;
    protected double Salary;
    String department;

    private String name;
    private int _code;

    private float rating;
    private double cost;

    private Double wrongDouble;
    private Char grade;

    private String label = getDefaultLabel();

    public Employee() {
    }

    public void configure(String name, int id, String department, double salary, String email, String phone) {
        this.name = name;
        this.id = id;
    }

    public void reset() {
    }

    public void validate() {
        if (id > 0) {
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

    private String getDefaultLabel() {
        return "N/A";
    }

    public void printInfo() {
        System.out.println("Employee: " + name);
    }
}
