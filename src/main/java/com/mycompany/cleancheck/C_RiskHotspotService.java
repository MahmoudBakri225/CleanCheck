package com.mycompany.cleancheck;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class C_RiskHotspotService {

    /*
     * =========================================================
     * RISK WEIGHTS
     * =========================================================
     *
     * Critical = 5
     * Warning  = 3
     * Info     = 1
     *
     * كلما زادت القيمة، زاد تركيز المشاكل في المكان.
     */

    private int getSeverityWeight(
            C_Warning warning
    ) {

        return switch (
                warning.getSeverity()
        ) {

            case CRITICAL -> 5;

            case WARNING -> 3;

            case INFO -> 1;
        };
    }

    /*
     * =========================================================
     * FILE HOTSPOTS
     * =========================================================
     */

    public void printFileHotspots(
            List<C_Warning> warnings
    ) {

        Map<String, Integer> fileRisk =
                new HashMap<>();

        Map<String, Integer> fileIssues =
                new HashMap<>();

        for (C_Warning warning :
                warnings) {

            String fileName =
                    warning.getFileName();

            int risk =
                    getSeverityWeight(
                            warning
                    );

            fileRisk.put(
                    fileName,
                    fileRisk.getOrDefault(
                            fileName,
                            0
                    ) + risk
            );

            fileIssues.put(
                    fileName,
                    fileIssues.getOrDefault(
                            fileName,
                            0
                    ) + 1
            );
        }

        List<String> files =
                new ArrayList<>(
                        fileRisk.keySet()
                );

        files.sort(
                Comparator
                        .comparingInt(
                                (String file)
                                        -> fileRisk.get(file)
                        )
                        .reversed()
        );

        System.out.println();

        System.out.println(
                "================ RISK HOTSPOTS ==================="
        );

        if (files.isEmpty()) {

            System.out.println(
                    "No risk hotspots detected."
            );

            System.out.println(
                    "=================================================="
            );

            return;
        }

        System.out.println(
                "FILE HOTSPOTS"
        );

        System.out.println(
                "--------------------------------------------------"
        );

        int rank = 1;

        for (String file :
                files) {

            System.out.println(
                    rank
                            + ". "
                            + file
                            + " | Issues: "
                            + fileIssues.get(file)
                            + " | Risk Score: "
                            + fileRisk.get(file)
            );

            rank++;

            
        }

        System.out.println(
                "=================================================="
        );
    }

    /*
     * =========================================================
     * RULE HOTSPOTS
     * =========================================================
     */

    public void printRuleHotspots(
            List<C_Warning> warnings
    ) {

        Map<String, Integer> ruleRisk =
                new HashMap<>();

        Map<String, Integer> ruleIssues =
                new HashMap<>();

        for (C_Warning warning :
                warnings) {

            String ruleName =
                    warning.getRuleName();

            int risk =
                    getSeverityWeight(
                            warning
                    );

            ruleRisk.put(
                    ruleName,
                    ruleRisk.getOrDefault(
                            ruleName,
                            0
                    ) + risk
            );

            ruleIssues.put(
                    ruleName,
                    ruleIssues.getOrDefault(
                            ruleName,
                            0
                    ) + 1
            );
        }

        List<String> rules =
                new ArrayList<>(
                        ruleRisk.keySet()
                );

        rules.sort(
                Comparator
                        .comparingInt(
                                (String rule)
                                        -> ruleRisk.get(rule)
                        )
                        .reversed()
        );

        System.out.println();

        System.out.println(
                "RULE HOTSPOTS"
        );

        System.out.println(
                "--------------------------------------------------"
        );

        if (rules.isEmpty()) {

            System.out.println(
                    "No rule hotspots detected."
            );

            System.out.println(
                    "=================================================="
            );

            return;
        }

        int rank = 1;

        for (String rule :
                rules) {

            System.out.println(
                    rank
                            + ". "
                            + rule
                            + " | Issues: "
                            + ruleIssues.get(rule)
                            + " | Risk Score: "
                            + ruleRisk.get(rule)
            );

            rank++;

            
        }

        System.out.println(
                "=================================================="
        );
    }

    /*
     * =========================================================
     * COMPLETE HOTSPOT REPORT
     * =========================================================
     */

    public void printRiskHotspots(
            List<C_Warning> warnings
    ) {

        printFileHotspots(
                warnings
        );

        printRuleHotspots(
                warnings
        );
    }
}