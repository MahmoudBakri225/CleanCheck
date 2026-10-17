package com.mycompany.cleancheck.database.repository;

public class C_ScanHistoryRepositoryTest {

    public static void main(String[] args) {

        C_ScanHistoryRepository repository =
                new C_ScanHistoryRepository();

        repository.printScanHistory(3);
    }
}