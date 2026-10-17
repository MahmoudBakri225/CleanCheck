package com.mycompany.cleancheck.database.service;

import com.mycompany.cleancheck.application.ConsoleOutput;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class IssueComparisonService {

    private static final String DOUBLE_LINE = "=========================================================";
    private static final String SINGLE_LINE = "---------------------------------------------------------";

    public ComparisonResult compareScans(int previousScanId, int currentScanId) {
        return compareIssues(IssueLoader.loadIssues(previousScanId), IssueLoader.loadIssues(currentScanId));
    }

    /** Compares two issue lists, prints the report and returns the counters. */
    static ComparisonResult compareIssues(List<IssueData> previousIssues, List<IssueData> currentIssues) {
        Outcome outcome = compare(previousIssues, currentIssues);
        printReport(previousIssues.size(), currentIssues.size(), outcome);
        return outcome.toResult();
    }

    /** Pure matching and classification: no database access and no printing. */
    static Outcome compare(List<IssueData> previousIssues, List<IssueData> currentIssues) {
        boolean[] currentMatched = new boolean[currentIssues.size()];
        int[] pairedCurrentIndex = matchIssues(previousIssues, currentIssues, currentMatched);
        Map<String, Integer> fileLineShift = IssueMatcher.detectFileLineShift(previousIssues, currentIssues);
        Outcome outcome = new Outcome();
        classifyMatched(previousIssues, currentIssues, pairedCurrentIndex, fileLineShift, outcome);
        collectUnmatched(previousIssues, currentIssues, pairedCurrentIndex, currentMatched, outcome);
        return outcome;
    }

    /** Step 1: pair every previous issue with its best current issue (-1 when none). */
    private static int[] matchIssues(List<IssueData> previousIssues, List<IssueData> currentIssues, boolean[] currentMatched) {
        int[] pairedCurrentIndex = new int[previousIssues.size()];
        Arrays.fill(pairedCurrentIndex, -1);
        for (int previousIndex = 0; previousIndex < previousIssues.size(); previousIndex++) {
            int currentIndex = IssueMatcher.findBestMatchingIssue(previousIssues.get(previousIndex), currentIssues, currentMatched);
            if (currentIndex != -1) {
                currentMatched[currentIndex] = true;
                pairedCurrentIndex[previousIndex] = currentIndex;
            }
        }
        return pairedCurrentIndex;
    }

    /** Steps 2-3: a matched pair is changed (severity), unchanged (same line or shifted file) or moved. */
    private static void classifyMatched(List<IssueData> previousIssues, List<IssueData> currentIssues, int[] pairedCurrentIndex,
            Map<String, Integer> fileLineShift, Outcome outcome) {
        for (int previousIndex = 0; previousIndex < previousIssues.size(); previousIndex++) {
            int currentIndex = pairedCurrentIndex[previousIndex];
            if (currentIndex == -1) {
                continue;
            }
            IssueData previousIssue = previousIssues.get(previousIndex);
            IssueData currentIssue = currentIssues.get(currentIndex);
            if (!previousIssue.severity.equals(currentIssue.severity)) {
                outcome.changed.add(new IssueChange(previousIssue, currentIssue));
            } else if (previousIssue.lineNumber == currentIssue.lineNumber) {
                outcome.unchanged++;
            } else if (isFileWideShift(previousIssue, currentIssue, fileLineShift)) {
                outcome.unchanged++;
            } else {
                outcome.moved.add(new IssueChange(previousIssue, currentIssue));
            }
        }
    }

    /** A line difference equal to the file-wide shift is not a real move (a line was inserted above). */
    private static boolean isFileWideShift(IssueData previousIssue, IssueData currentIssue, Map<String, Integer> fileLineShift) {
        Integer detectedShift = fileLineShift.get(previousIssue.fileName);
        return detectedShift != null && currentIssue.lineNumber - previousIssue.lineNumber == detectedShift;
    }

    /** Steps 4-5: unmatched previous issues are resolved, unmatched current issues are new. */
    private static void collectUnmatched(List<IssueData> previousIssues, List<IssueData> currentIssues, int[] pairedCurrentIndex,
            boolean[] currentMatched, Outcome outcome) {
        for (int previousIndex = 0; previousIndex < previousIssues.size(); previousIndex++) {
            if (pairedCurrentIndex[previousIndex] == -1) {
                outcome.resolved.add(previousIssues.get(previousIndex));
            }
        }
        for (int currentIndex = 0; currentIndex < currentIssues.size(); currentIndex++) {
            if (!currentMatched[currentIndex]) {
                IssueData currentIssue = currentIssues.get(currentIndex);
                outcome.added.add(currentIssue);
                if ("CRITICAL".equalsIgnoreCase(currentIssue.severity)) {
                    outcome.newCritical++;
                }
            }
        }
    }

    private static void printReport(int previousCount, int currentCount, Outcome outcome) {
        ConsoleOutput.out().println();
        ConsoleOutput.out().println(DOUBLE_LINE);
        ConsoleOutput.out().println("              ISSUE COMPARISON");
        ConsoleOutput.out().println(DOUBLE_LINE);
        ConsoleOutput.out().println("Previous Issues : " + previousCount);
        ConsoleOutput.out().println("Current Issues  : " + currentCount);
        ConsoleOutput.out().println(SINGLE_LINE);
        ConsoleOutput.out().println("Resolved Issues : " + outcome.resolved.size());
        ConsoleOutput.out().println("New Issues      : " + outcome.added.size());
        ConsoleOutput.out().println("Unchanged Issues: " + outcome.unchanged);
        ConsoleOutput.out().println("Moved Issues    : " + outcome.moved.size());
        ConsoleOutput.out().println("Changed Issues  : " + outcome.changed.size());
        ConsoleOutput.out().println("New Critical    : " + outcome.newCritical);
        ConsoleOutput.out().println(DOUBLE_LINE);
        printNewIssues(outcome.added);
        printMovedIssues(outcome.moved);
        printChangedIssues(outcome.changed);
    }

    private static void printNewIssues(List<IssueData> newIssues) {
        if (newIssues.isEmpty()) {
            return;
        }
        ConsoleOutput.out().println();
        ConsoleOutput.out().println("NEW ISSUES");
        for (IssueData issue : newIssues) {
            ConsoleOutput.out().println(issue.fileName + " | " + issue.ruleName + " | Line " + issue.lineNumber + " | " + issue.message);
        }
    }

    private static void printMovedIssues(List<IssueChange> movedIssues) {
        if (movedIssues.isEmpty()) {
            return;
        }
        ConsoleOutput.out().println();
        ConsoleOutput.out().println("MOVED ISSUES");
        for (IssueChange change : movedIssues) {
            ConsoleOutput.out().println(change.previousIssue.fileName + " | " + change.previousIssue.ruleName + " | Line "
                    + change.previousIssue.lineNumber + " -> " + change.currentIssue.lineNumber + " | " + change.currentIssue.message);
        }
    }

    private static void printChangedIssues(List<IssueChange> changedIssues) {
        if (changedIssues.isEmpty()) {
            return;
        }
        ConsoleOutput.out().println();
        ConsoleOutput.out().println("CHANGED ISSUES");
        for (IssueChange change : changedIssues) {
            ConsoleOutput.out().println(change.previousIssue.fileName + " | " + change.previousIssue.ruleName + " | Line "
                    + change.currentIssue.lineNumber + " | Severity " + change.previousIssue.severity + " -> "
                    + change.currentIssue.severity + " | " + change.currentIssue.message);
        }
    }

    /** Raw classification of one comparison. */
    static final class Outcome {
        final List<IssueData> resolved = new ArrayList<>();
        final List<IssueData> added = new ArrayList<>();
        final List<IssueChange> moved = new ArrayList<>();
        final List<IssueChange> changed = new ArrayList<>();
        int unchanged;
        int newCritical;

        ComparisonResult toResult() {
            return new ComparisonResult(added.size(), resolved.size(), unchanged, moved.size(), changed.size(), newCritical);
        }
    }

    public static class ComparisonResult {
        private final int newIssues;
        private final int resolvedIssues;
        private final int unchangedIssues;
        private final int movedIssues;
        private final int changedIssues;
        private final int newCriticalIssues;

        public ComparisonResult(int newIssues, int resolvedIssues, int unchangedIssues, int movedIssues, int changedIssues, int newCriticalIssues) {
            this.newIssues = newIssues;
            this.resolvedIssues = resolvedIssues;
            this.unchangedIssues = unchangedIssues;
            this.movedIssues = movedIssues;
            this.changedIssues = changedIssues;
            this.newCriticalIssues = newCriticalIssues;
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
}
