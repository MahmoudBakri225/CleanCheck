package com.mycompany.cleancheck.database.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Matching of issues between two scans (line shifts, best match, fingerprints). */
final class IssueMatcher {

    private IssueMatcher() {
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

    static Map<String, Integer> detectFileLineShift(
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

    static int findBestMatchingIssue(
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

    static String createFingerprint(
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
}
