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

public class C_DatabaseTest {

    public static void main(String[] args) {

        try (Connection connection = C_DatabaseConnection.getConnection()) {

            System.out.println("Database connected successfully!");

        } catch (Exception e) {

            System.out.println("Database connection failed!");
            e.printStackTrace();
        }
    }
    
}
