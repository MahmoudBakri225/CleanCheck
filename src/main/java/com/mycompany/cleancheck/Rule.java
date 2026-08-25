/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */


package com.mycompany.cleancheck;

import java.util.List;

public interface Rule {
    String getName();
    List<Warning> analyze(String fileName, String source);
}
