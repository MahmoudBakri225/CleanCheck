package com.mycompany.cleancheck.database.repository;

public class C_ProjectRepositoryTest {

    public static void main(String[] args) {

        C_ProjectRepository repository = new C_ProjectRepository();

        int projectId = repository.saveProject(
                "CleanCheck",
                "D:\\Depi_C\\PG\\CleanCheck"
        );

        System.out.println("Saved Project ID: " + projectId);
    }
}