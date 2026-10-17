/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.cleancheck.database;

/**
 *
 * @author MahmoudBakri
 */
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class C_DatabaseConnection {

    private static final String DATABASE_URL =
            "jdbc:sqlite:database/cleancheck.db";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DATABASE_URL);
    }
    
}
