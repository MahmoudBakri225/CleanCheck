package com.mycompany.cleancheck.database.repository;

public class C_IssueRepositoryTest {

    public static void main(String[] args) {

        C_IssueRepository repository = new C_IssueRepository();

        repository.saveIssue(
                1,
                "Employee.java",
                10,
                "Encapsulation",
                "CRITICAL",
                "Field should be private",
                "Make the field private"
        );

        System.out.println("Issue saved successfully!");
    }
}