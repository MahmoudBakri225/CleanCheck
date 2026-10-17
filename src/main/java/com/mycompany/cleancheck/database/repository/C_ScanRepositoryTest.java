package com.mycompany.cleancheck.database.repository;

public class C_ScanRepositoryTest {

    public static void main(String[] args) {

        C_ScanRepository repository = new C_ScanRepository();

        int scanId = repository.saveScan(
                1,   // ProjectId
                15,  // QualityScore
                35,  // TotalIssues
                7,   // CriticalCount
                22,  // WarningCount
                6    // InfoCount
        );

        System.out.println("Saved Scan ID: " + scanId);
    }
}