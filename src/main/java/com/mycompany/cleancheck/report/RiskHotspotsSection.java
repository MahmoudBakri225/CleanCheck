package com.mycompany.cleancheck.report;

import com.mycompany.cleancheck.analysis.Warning;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Risk hotspot tables of the HTML report. */
final class RiskHotspotsSection {

    private RiskHotspotsSection() {
    }

    /*
     * =========================================================
     * RISK HOTSPOTS
     * =========================================================
     */
    static void appendRiskHotspots(
            StringBuilder html,
            List<Warning> warnings
    ) {

        Map<String, Long> fileCounts
                = warnings.stream()
                        .collect(
                                Collectors.groupingBy(
                                        Warning::getFileName,
                                        Collectors.counting()
                                )
                        );

        Map<String, Long> ruleCounts
                = warnings.stream()
                        .collect(
                                Collectors.groupingBy(
                                        Warning::getRuleName,
                                        Collectors.counting()
                                )
                        );

        html.append("""
                <section class="section">
                    <h2>
                        Risk Hotspots
                    </h2>
                    <p class="subtitle">
                        Files and rules with the highest
                        concentration of issues
                    </p>

                    <h3>
                        File Hotspots
                    </h3>
                """);

        fileCounts.entrySet()
                .stream()
                .sorted(
                        Map.Entry
                                .<String, Long>comparingByValue()
                                .reversed()
                )
                .forEach(entry -> {

                    int risk
                            = calculateRisk(
                                    entry.getKey(),
                                    warnings
                            );

                    html.append("""
                            <div class="hotspot">
                                <span class="hotspot-title">
                            """);

                    html.append(
                            ReportFormat.escapeHtml(
                                    entry.getKey()
                            )
                    );

                    html.append("""
                                </span>

                                <span>
                            """);

                    html.append(entry.getValue());
                    html.append(" issues");

                    html.append("""
                                </span>

                                <span class="risk">
                                    Risk:
                            """);

                    html.append(risk);

                    html.append("""
                                </span>
                            </div>
                            """);
                });

        /*
         * =====================================================
         * RULE HOTSPOTS
         * =====================================================
         */
        html.append("""
                    <h3>
                        Rule Hotspots
                    </h3>

                    <table>
                        <thead>
                            <tr>
                                <th>Rule</th>
                                <th>Issues</th>
                                <th>Risk Score</th>
                            </tr>
                        </thead>
                        <tbody>
                """);

        ruleCounts.keySet()
                .stream()
                .sorted(
                        (rule1, rule2) -> {

                            int risk1
                            = calculateRuleRisk(
                                    rule1,
                                    warnings
                            );

                            int risk2
                            = calculateRuleRisk(
                                    rule2,
                                    warnings
                            );

                            /*
                             * Sort by Risk Score DESC
                             */
                            if (risk1 != risk2) {

                                return Integer.compare(
                                        risk2,
                                        risk1
                                );
                            }

                            /*
                             * If risk is equal,
                             * sort by rule name
                             */
                            return rule1.compareTo(
                                    rule2
                            );
                        }
                )
                .forEach(ruleName -> {

                    long issueCount
                            = ruleCounts.get(
                                    ruleName
                            );

                    int risk
                            = calculateRuleRisk(
                                    ruleName,
                                    warnings
                            );

                    html.append("""
                            <tr>
                                <td>%s</td>
                                <td>%d</td>
                                <td>%d</td>
                            </tr>
                            """.formatted(
                            ReportFormat.escapeHtml(ruleName),
                            issueCount,
                            risk
                    ));
                });

        html.append("""
                        </tbody>
                    </table>
                </section>
                """);
    }

    /*
     * =========================================================
     * FILE RISK
     * =========================================================
     */
    static int calculateRisk(
            String fileName,
            List<Warning> warnings
    ) {

        return warnings.stream()
                .filter(w
                        -> w.getFileName()
                        .equals(fileName)
                )
                .mapToInt(
                        ReportFormat::getSeverityWeight
                )
                .sum();
    }

    /*
     * =========================================================
     * RULE RISK
     * =========================================================
     */
    static int calculateRuleRisk(
            String ruleName,
            List<Warning> warnings
    ) {

        return warnings.stream()
                .filter(w
                        -> w.getRuleName()
                        .equals(ruleName)
                )
                .mapToInt(
                        ReportFormat::getSeverityWeight
                )
                .sum();
    }
}
