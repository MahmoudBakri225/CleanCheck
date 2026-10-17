package com.mycompany.cleancheck.database.service;

public class C_ScanComparisonServiceTest {

    public static void main(String[] args) {

        C_ScanComparisonService service =
                new C_ScanComparisonService();

        service.compareLatestScans(3);
    }
}