package com.mycompany.cleancheck.database;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class C_DatabaseHistoryReset {

    public static void main(String[] args) {

        int projectId = 3;

        String deleteIssuesSql =
                "DELETE FROM Issue " +
                "WHERE ScanId IN " +
                "(SELECT ScanId FROM Scan WHERE ProjectId = ?)";

        String deleteScansSql =
                "DELETE FROM Scan " +
                "WHERE ProjectId = ?";

        try (
                Connection connection =
                        C_DatabaseConnection.getConnection()
        ) {

            connection.setAutoCommit(false);

            try (
                    PreparedStatement deleteIssues =
                            connection.prepareStatement(
                                    deleteIssuesSql
                            );

                    PreparedStatement deleteScans =
                            connection.prepareStatement(
                                    deleteScansSql
                            )
            ) {

                deleteIssues.setInt(
                        1,
                        projectId
                );

                int issuesDeleted =
                        deleteIssues.executeUpdate();

                deleteScans.setInt(
                        1,
                        projectId
                );

                int scansDeleted =
                        deleteScans.executeUpdate();

                connection.commit();

                System.out.println();
                System.out.println(
                        "=============================================="
                );

                System.out.println(
                        "       DATABASE HISTORY RESET"
                );

                System.out.println(
                        "=============================================="
                );

                System.out.println(
                        "Project ID     : "
                                + projectId
                );

                System.out.println(
                        "Issues Deleted : "
                                + issuesDeleted
                );

                System.out.println(
                        "Scans Deleted  : "
                                + scansDeleted
                );

                System.out.println(
                        "Project        : KEPT"
                );

                System.out.println(
                        "=============================================="
                );
            }

        } catch (Exception e) {

            e.printStackTrace();
        }
    }
}