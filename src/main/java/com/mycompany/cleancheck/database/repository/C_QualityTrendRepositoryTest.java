package com.mycompany.cleancheck.database.repository;

public class C_QualityTrendRepositoryTest {

    public static void main(String[] args) {

        C_ScanHistoryRepository repository =
                new C_ScanHistoryRepository();

        repository.printQualityTrend(3);
    }
}