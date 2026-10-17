
package com.mycompany.cleancheck;

public class Employee {

    private String employeeName;

    private double salary;

    public Employee() {
    }

    public void calculateSalary() {
        if (salary > 0) {
            // TODO: Review empty if block
        }
    }

    public void emptyMethod() {
        // TODO: Review empty method
    }

    public void badLoop() {
        while (true) {
            // TODO [CleanCheck]: Possible infinite loop — add a clear exit condition
        }
    }
}

