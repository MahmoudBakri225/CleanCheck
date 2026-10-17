package com.mycompany.cleancheck.risk;

import com.mycompany.cleancheck.application.ConsoleOutput;
import com.mycompany.cleancheck.analysis.Severity;
import com.mycompany.cleancheck.analysis.Warning;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Critical=5, Warning=3, Info=1
 */
public class RiskHotspotService {

    private int weight(Warning warning) {
        return switch (warning.getSeverity()) {
            case CRITICAL -> 5;
            case WARNING -> 3;
            case INFO -> 1;
        };
    }

    public void printFileHotspots(List<Warning> warnings) {
        Map<String, Integer> risk = new HashMap<>();
        Map<String, Integer> issues = new HashMap<>();

        for (Warning warning : warnings) {
            String file = warning.getFileName();
            risk.put(file, risk.getOrDefault(file, 0) + weight(warning));
            issues.put(file, issues.getOrDefault(file, 0) + 1);
        }

        List<String> files = new ArrayList<>(risk.keySet());
        files.sort(Comparator.comparingInt(risk::get).reversed());

        ConsoleOutput.out().println();
        ConsoleOutput.out().println("================ RISK HOTSPOTS ===================");

        if (files.isEmpty()) {
            ConsoleOutput.out().println("No risk hotspots detected.");
            ConsoleOutput.out().println("==================================================");
            return;
        }

        ConsoleOutput.out().println("FILE HOTSPOTS");
        ConsoleOutput.out().println("--------------------------------------------------");

        int rank = 1;
        for (String file : files) {
            ConsoleOutput.out().println(
                    rank + ". " + file
                            + " | Issues: " + issues.get(file)
                            + " | Risk Score: " + risk.get(file)
            );
            rank++;
        }

        ConsoleOutput.out().println("==================================================");
    }

    public void printRuleHotspots(List<Warning> warnings) {
        Map<String, Integer> risk = new HashMap<>();
        Map<String, Integer> issues = new HashMap<>();

        for (Warning warning : warnings) {
            String rule = warning.getRuleName();
            risk.put(rule, risk.getOrDefault(rule, 0) + weight(warning));
            issues.put(rule, issues.getOrDefault(rule, 0) + 1);
        }

        List<String> rules = new ArrayList<>(risk.keySet());
        rules.sort(Comparator.comparingInt(risk::get).reversed());

        ConsoleOutput.out().println();
        ConsoleOutput.out().println("RULE HOTSPOTS");
        ConsoleOutput.out().println("--------------------------------------------------");

        if (rules.isEmpty()) {
            ConsoleOutput.out().println("No rule hotspots detected.");
            ConsoleOutput.out().println("==================================================");
            return;
        }

        int rank = 1;
        for (String rule : rules) {
            ConsoleOutput.out().println(
                    rank + ". " + rule
                            + " | Issues: " + issues.get(rule)
                            + " | Risk Score: " + risk.get(rule)
            );
            rank++;
        }

        ConsoleOutput.out().println("==================================================");
    }

    public void printRiskHotspots(List<Warning> warnings) {
        printFileHotspots(warnings);
        printRuleHotspots(warnings);
    }
}
