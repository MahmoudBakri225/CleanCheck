
package com.mycompany.cleancheck;

public class Student {

    private String name;

    private int age;

    public Student() {
    }
        // TODO [CleanCheck]: Too many parameters — consider a parameter object or redesign

    public void setData(
            String name,
            int age,
            String address,
            String phone,
            String email) {
        this.name = name;
        this.age = age;
    }

    public void emptyMethod() {
        // TODO: Review empty method
    }

    public void checkAge() {
        if (age > 18) {
            // TODO: Review empty if block
        }
    }
}

