
package com.mycompany.cleancheck.database.service;

import com.mycompany.cleancheck.database.C_DatabaseConnection;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class C_IssueComparisonService {

    public ComparisonResult compareScans(
            int previousScanId,
            int currentScanId
    ) {

        List<IssueData> previousIssues =
                loadIssues(previousScanId);

        List<IssueData> currentIssues =
                loadIssues(currentScanId);

        boolean[] previousMatched =
                new boolean[previousIssues.size()];

        boolean[] currentMatched =
                new boolean[currentIssues.size()];

        int resolvedCount = 0;
        int newCount = 0;
        int unchangedCount = 0;
        int movedCount = 0;
        int changedCount = 0;

        int newCriticalCount = 0;

        List<IssueData> resolvedIssues =
                new ArrayList<>();

        List<IssueData> newIssues =
                new ArrayList<>();

        List<IssueChange> movedIssues =
                new ArrayList<>();

        List<IssueChange> changedIssues =
                new ArrayList<>();

        /*
         * =========================================================
         * STEP 1
         * MATCH PREVIOUS ISSUES WITH CURRENT ISSUES
         * =========================================================
         */

        for (int previousIndex = 0;
             previousIndex < previousIssues.size();
             previousIndex++) {

            IssueData previousIssue =
                    previousIssues.get(previousIndex);

            int currentIndex =
                    findBestMatchingIssue(
                            previousIssue,
                            currentIssues,
                            currentMatched
                    );

            if (currentIndex == -1) {
                continue;
            }

            IssueData currentIssue =
                    currentIssues.get(currentIndex);

            previousMatched[previousIndex] =
                    true;

            currentMatched[currentIndex] =
                    true;
        }

        /*
         * =========================================================
         * STEP 2
         * DETECT LINE SHIFT PER FILE
         * =========================================================
         *
         * Example:
         *
         * Previous:
         * Line 21
         * Line 22
         * Line 23
         *
         * Current:
         * Line 22
         * Line 23
         * Line 24
         *
         * Shift = +1
         *
         * These are NOT moved issues.
         *
         * They are the same issues affected by a new line
         * inserted before them.
         */

        Map<String, Integer> fileLineShift =
                detectFileLineShift(
                        previousIssues,
                        currentIssues
                );

        /*
         * =========================================================
         * STEP 3
         * CLASSIFY MATCHED ISSUES
         * =========================================================
         */

        for (int previousIndex = 0;
             previousIndex < previousIssues.size();
             previousIndex++) {

            if (!previousMatched[previousIndex]) {
                continue;
            }

            IssueData previousIssue =
                    previousIssues.get(previousIndex);

            int currentIndex =
                    findMatchedCurrentIssue(
                            previousIssue,
                            currentIssues,
                            currentMatched
                    );

            /*
             * currentMatched[] is already true for all matched
             * issues, so we need to locate the corresponding
             * current issue again.
             */

            if (currentIndex == -1) {
                continue;
            }

            IssueData currentIssue =
                    currentIssues.get(currentIndex);

            /*
             * -----------------------------------------------------
             * CHANGED SEVERITY
             * -----------------------------------------------------
             */

            if (!previousIssue.severity.equals(
                    currentIssue.severity)) {

                changedCount++;

                changedIssues.add(
                        new IssueChange(
                                previousIssue,
                                currentIssue
                        )
                );

                continue;
            }

            /*
             * -----------------------------------------------------
             * SAME LINE
             * -----------------------------------------------------
             */

            if (previousIssue.lineNumber ==
                    currentIssue.lineNumber) {

                unchangedCount++;

                continue;
            }

            /*
             * -----------------------------------------------------
             * LINE SHIFT
             * -----------------------------------------------------
             *
             * If the line difference equals the detected
             * file-wide shift, this is NOT a moved issue.
             */

            int lineDifference =
                    currentIssue.lineNumber
                            - previousIssue.lineNumber;

            Integer detectedShift =
                    fileLineShift.get(
                            previousIssue.fileName
                    );

            if (detectedShift != null
                    && lineDifference == detectedShift) {

                unchangedCount++;

                continue;
            }

            /*
             * -----------------------------------------------------
             * REAL MOVED ISSUE
             * -----------------------------------------------------
             */

            movedCount++;

            movedIssues.add(
                    new IssueChange(
                            previousIssue,
                            currentIssue
                    )
            );
        }

        /*
         * =========================================================
         * STEP 4
         * RESOLVED ISSUES
         * =========================================================
         */

        for (int previousIndex = 0;
             previousIndex < previousIssues.size();
             previousIndex++) {

            if (previousMatched[previousIndex]) {
                continue;
            }

            IssueData previousIssue =
                    previousIssues.get(previousIndex);

            resolvedCount++;

            resolvedIssues.add(
                    previousIssue
            );
        }

        /*
         * =========================================================
         * STEP 5
         * NEW ISSUES
         * =========================================================
         */

        for (int currentIndex = 0;
             currentIndex < currentIssues.size();
             currentIndex++) {

            if (currentMatched[currentIndex]) {
                continue;
            }

            IssueData currentIssue =
                    currentIssues.get(currentIndex);

            newCount++;

            newIssues.add(
                    currentIssue
            );

            /*
             * -----------------------------------------------------
             * NEW CRITICAL ISSUE
             * -----------------------------------------------------
             */

            if ("CRITICAL".equalsIgnoreCase(
                    currentIssue.severity)) {

                newCriticalCount++;
            }
        }

        /*
         * =========================================================
         * OUTPUT
         * =========================================================
         */

        System.out.println();

        System.out.println(
                "========================================================="
        );

        System.out.println(
                "              ISSUE COMPARISON"
        );

        System.out.println(
                "========================================================="
        );

        System.out.println(
                "Previous Issues : "
                        + previousIssues.size()
        );

        System.out.println(
                "Current Issues  : "
                        + currentIssues.size()
        );

        System.out.println(
                "---------------------------------------------------------"
        );

        System.out.println(
                "Resolved Issues : "
                        + resolvedCount
        );

        System.out.println(
                "New Issues      : "
                        + newCount
        );

        System.out.println(
                "Unchanged Issues: "
                        + unchangedCount
        );

        System.out.println(
                "Moved Issues    : "
                        + movedCount
        );

        System.out.println(
                "Changed Issues  : "
                        + changedCount
        );

        System.out.println(
                "New Critical    : "
                        + newCriticalCount
        );

        System.out.println(
                "========================================================="
        );

        /*
         * =========================================================
         * NEW ISSUE DETAILS
         * =========================================================
         */

        if (!newIssues.isEmpty()) {

            System.out.println();

            System.out.println(
                    "NEW ISSUES"
            );

            for (IssueData issue :
                    newIssues) {

                System.out.println(
                        issue.fileName
                                + " | "
                                + issue.ruleName
                                + " | Line "
                                + issue.lineNumber
                                + " | "
                                + issue.message
                );
            }
        }

        /*
         * =========================================================
         * MOVED ISSUE DETAILS
         * =========================================================
         */

        if (!movedIssues.isEmpty()) {

            System.out.println();

            System.out.println(
                    "MOVED ISSUES"
            );

            for (IssueChange change :
                    movedIssues) {

                System.out.println(
                        change.previousIssue.fileName
                                + " | "
                                + change.previousIssue.ruleName
                                + " | Line "
                                + change.previousIssue.lineNumber
                                + " -> "
                                + change.currentIssue.lineNumber
                                + " | "
                                + change.currentIssue.message
                );
            }
        }

        /*
         * =========================================================
         * CHANGED ISSUE DETAILS
         * =========================================================
         */

        if (!changedIssues.isEmpty()) {

            System.out.println();

            System.out.println(
                    "CHANGED ISSUES"
            );

            for (IssueChange change :
                    changedIssues) {

                System.out.println(
                        change.previousIssue.fileName
                                + " | "
                                + change.previousIssue.ruleName
                                + " | Line "
                                + change.currentIssue.lineNumber
                                + " | Severity "
                                + change.previousIssue.severity
                                + " -> "
                                + change.currentIssue.severity
                                + " | "
                                + change.currentIssue.message
                );
            }
        }

        return new ComparisonResult(
                newCount,
                resolvedCount,
                unchangedCount,
                movedCount,
                changedCount,
                newCriticalCount
        );
    }

    /*
     * =========================================================
     * DETECT FILE LINE SHIFT
     * =========================================================
     *
     * We count line differences between matching issues.
     *
     * Example:
     *
     * +1 appears 10 times
     *  0 appears 12 times
     *
     * The dominant shift is detected per file.
     *
     * A shift is only accepted when it appears at least
     * twice. This prevents one real moved issue from becoming
     * a file-wide shift.
     */

    private Map<String, Integer> detectFileLineShift(
            List<IssueData> previousIssues,
            List<IssueData> currentIssues
    ) {

        Map<String, Map<Integer, Integer>> shiftCounters =
                new HashMap<>();

        boolean[] currentUsed =
                new boolean[currentIssues.size()];

        /*
         * Match each previous issue with a current issue
         * temporarily for line-shift analysis.
         */

        for (IssueData previousIssue :
                previousIssues) {

            int bestIndex = -1;

            int smallestDistance =
                    Integer.MAX_VALUE;

            for (int currentIndex = 0;
                 currentIndex < currentIssues.size();
                 currentIndex++) {

                if (currentUsed[currentIndex]) {
                    continue;
                }

                IssueData currentIssue =
                        currentIssues.get(currentIndex);

                if (!previousIssue.fingerprint.equals(
                        currentIssue.fingerprint)) {

                    continue;
                }

                int distance =
                        Math.abs(
                                previousIssue.lineNumber
                                        - currentIssue.lineNumber
                        );

                if (distance < smallestDistance) {

                    smallestDistance =
                            distance;

                    bestIndex =
                            currentIndex;
                }
            }

            if (bestIndex == -1) {
                continue;
            }

            IssueData currentIssue =
                    currentIssues.get(bestIndex);

            currentUsed[bestIndex] =
                    true;

            int difference =
                    currentIssue.lineNumber
                            - previousIssue.lineNumber;

            Map<Integer, Integer> counters =
                    shiftCounters.computeIfAbsent(
                            previousIssue.fileName,
                            key -> new HashMap<>()
                    );

            counters.put(
                    difference,
                    counters.getOrDefault(
                            difference,
                            0
                    ) + 1
            );
        }

        /*
         * Find dominant shift per file.
         */

        Map<String, Integer> result =
                new HashMap<>();

        for (Map.Entry<String, Map<Integer, Integer>> entry :
                shiftCounters.entrySet()) {

            String fileName =
                    entry.getKey();

            Map<Integer, Integer> counters =
                    entry.getValue();

            int bestShift = 0;
            int bestCount = 0;

            for (Map.Entry<Integer, Integer> shiftEntry :
                    counters.entrySet()) {

                int shift =
                        shiftEntry.getKey();

                int count =
                        shiftEntry.getValue();

                /*
                 * We only consider a non-zero shift if it
                 * appears at least twice.
                 */

                if (shift != 0
                        && count >= 2
                        && count > bestCount) {

                    bestShift =
                            shift;

                    bestCount =
                            count;
                }
            }

            if (bestCount >= 2) {

                result.put(
                        fileName,
                        bestShift
                );
            }
        }

        return result;
    }

    /*
     * =========================================================
     * FIND MATCHED CURRENT ISSUE
     * =========================================================
     *
     * Finds the current issue that was already matched with
     * the supplied previous issue.
     */

    private int findMatchedCurrentIssue(
            IssueData previousIssue,
            List<IssueData> currentIssues,
            boolean[] currentMatched
    ) {

        int bestIndex = -1;

        int smallestDistance =
                Integer.MAX_VALUE;

        for (int currentIndex = 0;
             currentIndex < currentIssues.size();
             currentIndex++) {

            if (!currentMatched[currentIndex]) {
                continue;
            }

            IssueData currentIssue =
                    currentIssues.get(currentIndex);

            if (!previousIssue.fingerprint.equals(
                    currentIssue.fingerprint)) {

                continue;
            }

            int distance =
                    Math.abs(
                            previousIssue.lineNumber
                                    - currentIssue.lineNumber
                    );

            if (distance < smallestDistance) {

                smallestDistance =
                        distance;

                bestIndex =
                        currentIndex;
            }
        }

        return bestIndex;
    }

    /*
     * =========================================================
     * FIND BEST MATCHING ISSUE
     * =========================================================
     *
     * Matching priority:
     *
     * 1. Same fingerprint + same line
     *
     * 2. Same fingerprint + same severity
     *    + closest line
     *
     * 3. Same fingerprint
     *    + closest line
     */

    private int findBestMatchingIssue(
            IssueData previousIssue,
            List<IssueData> currentIssues,
            boolean[] currentMatched
    ) {

        int bestIndex = -1;

        /*
         * ---------------------------------------------------------
         * STEP 1
         * Same fingerprint + same line
         * ---------------------------------------------------------
         */

        for (int currentIndex = 0;
             currentIndex < currentIssues.size();
             currentIndex++) {

            if (currentMatched[currentIndex]) {
                continue;
            }

            IssueData currentIssue =
                    currentIssues.get(currentIndex);

            if (!previousIssue.fingerprint.equals(
                    currentIssue.fingerprint)) {

                continue;
            }

            if (previousIssue.lineNumber ==
                    currentIssue.lineNumber) {

                return currentIndex;
            }
        }

        /*
         * ---------------------------------------------------------
         * STEP 2
         * Same fingerprint + same severity
         * + closest line
         * ---------------------------------------------------------
         */

        int smallestDistance =
                Integer.MAX_VALUE;

        for (int currentIndex = 0;
             currentIndex < currentIssues.size();
             currentIndex++) {

            if (currentMatched[currentIndex]) {
                continue;
            }

            IssueData currentIssue =
                    currentIssues.get(currentIndex);

            if (!previousIssue.fingerprint.equals(
                    currentIssue.fingerprint)) {

                continue;
            }

            if (!previousIssue.severity.equals(
                    currentIssue.severity)) {

                continue;
            }

            int distance =
                    Math.abs(
                            previousIssue.lineNumber
                                    - currentIssue.lineNumber
                    );

            if (distance < smallestDistance) {

                smallestDistance =
                        distance;

                bestIndex =
                        currentIndex;
            }
        }

        if (bestIndex != -1) {
            return bestIndex;
        }

        /*
         * ---------------------------------------------------------
         * STEP 3
         * Same fingerprint + closest line
         *
         * Allows Changed Severity detection.
         * ---------------------------------------------------------
         */

        smallestDistance =
                Integer.MAX_VALUE;

        bestIndex = -1;

        for (int currentIndex = 0;
             currentIndex < currentIssues.size();
             currentIndex++) {

            if (currentMatched[currentIndex]) {
                continue;
            }

            IssueData currentIssue =
                    currentIssues.get(currentIndex);

            if (!previousIssue.fingerprint.equals(
                    currentIssue.fingerprint)) {

                continue;
            }

            int distance =
                    Math.abs(
                            previousIssue.lineNumber
                                    - currentIssue.lineNumber
                    );

            if (distance < smallestDistance) {

                smallestDistance =
                        distance;

                bestIndex =
                        currentIndex;
            }
        }

        return bestIndex;
    }

    /*
     * =========================================================
     * CREATE FINGERPRINT
     * =========================================================
     *
     * Line number is intentionally NOT included.
     *
     * Severity is intentionally NOT included.
     */

    private String createFingerprint(
            String fileName,
            String ruleName,
            String message
    ) {

        String rawValue =
                fileName
                        + "|"
                        + ruleName
                        + "|"
                        + message;

        try {

            MessageDigest digest =
                    MessageDigest.getInstance(
                            "SHA-256"
                    );

            byte[] hash =
                    digest.digest(
                            rawValue.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );

            StringBuilder hex =
                    new StringBuilder();

            for (byte value :
                    hash) {

                hex.append(
                        String.format(
                                "%02x",
                                value
                        )
                );
            }

            return hex.toString();

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to create issue fingerprint!",
                    e
            );
        }
    }

    /*
     * =========================================================
     * LOAD ISSUES
     * =========================================================
     */

    private List<IssueData> loadIssues(
            int scanId
    ) {

        List<IssueData> issues =
                new ArrayList<>();

        String sql =
                "SELECT FileName, "
                        + "LineNumber, "
                        + "RuleName, "
                        + "Severity, "
                        + "Message, "
                        + "Suggestion "
                        + "FROM Issue "
                        + "WHERE ScanId = ? "
                        + "ORDER BY FileName, "
                        + "RuleName, "
                        + "LineNumber";

        try (
                Connection connection =
                        new C_DatabaseConnection()
                                .getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setInt(
                    1,
                    scanId
            );

            try (
                    ResultSet resultSet =
                            statement.executeQuery()
            ) {

                while (resultSet.next()) {

                    IssueData issue =
                            new IssueData();

                    issue.fileName =
                            resultSet.getString(
                                    "FileName"
                            );

                    issue.lineNumber =
                            resultSet.getInt(
                                    "LineNumber"
                            );

                    issue.ruleName =
                            resultSet.getString(
                                    "RuleName"
                            );

                    issue.severity =
                            resultSet.getString(
                                    "Severity"
                            );

                    issue.message =
                            resultSet.getString(
                                    "Message"
                            );

                    issue.suggestion =
                            resultSet.getString(
                                    "Suggestion"
                            );

                    issue.fingerprint =
                            createFingerprint(
                                    issue.fileName,
                                    issue.ruleName,
                                    issue.message
                            );

                    issues.add(
                            issue
                    );
                }
            }

        } catch (Exception e) {

            e.printStackTrace();
        }

        return issues;
    }

    /*
     * =========================================================
     * COMPARISON RESULT
     * =========================================================
     */

    public static class ComparisonResult {

        private final int newIssues;
        private final int resolvedIssues;
        private final int unchangedIssues;
        private final int movedIssues;
        private final int changedIssues;
        private final int newCriticalIssues;

        public ComparisonResult(
                int newIssues,
                int resolvedIssues,
                int unchangedIssues,
                int movedIssues,
                int changedIssues,
                int newCriticalIssues
        ) {

            this.newIssues =
                    newIssues;

            this.resolvedIssues =
                    resolvedIssues;

            this.unchangedIssues =
                    unchangedIssues;

            this.movedIssues =
                    movedIssues;

            this.changedIssues =
                    changedIssues;

            this.newCriticalIssues =
                    newCriticalIssues;
        }

        public int getNewIssues() {
            return newIssues;
        }

        public int getResolvedIssues() {
            return resolvedIssues;
        }

        public int getUnchangedIssues() {
            return unchangedIssues;
        }

        public int getMovedIssues() {
            return movedIssues;
        }

        public int getChangedIssues() {
            return changedIssues;
        }

        public int getNewCriticalIssues() {
            return newCriticalIssues;
        }
    }

    /*
     * =========================================================
     * ISSUE DATA
     * =========================================================
     */

    private static class IssueData {

        String fileName;
        int lineNumber;
        String ruleName;
        String severity;
        String message;
        String suggestion;
        String fingerprint;
    }

    /*
     * =========================================================
     * ISSUE CHANGE
     * =========================================================
     */

    private static class IssueChange {

        IssueData previousIssue;
        IssueData currentIssue;

        IssueChange(
                IssueData previousIssue,
                IssueData currentIssue
        ) {

            this.previousIssue =
                    previousIssue;

            this.currentIssue =
                    currentIssue;
        }
    }
}

