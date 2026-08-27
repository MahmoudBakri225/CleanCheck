/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */


package com.mycompany.cleancheck;

import java.util.List;

/**
 * Defines the common contract that every CleanCheck rule must follow.
 * Each rule must provide a name and a method for analyzing Java source code
 * and returning any warnings it detects.
 *
 * @author KA
 */
public interface L_Rule {

    /**
     * Returns the name of the rule.
     * This name is used when displaying warnings.
     *
     * @return the rule name
     */
    String getName();

    /**
     * Analyzes the given Java source code and detects issues according to this rule.
     *
     * @param fileName the name of the source file being analyzed
     * @param source   the full source code content as a string
     * @return a list of warnings found by this rule (empty list if no issues)
     */
    List<C_Warning> analyze(String fileName, String source);
}