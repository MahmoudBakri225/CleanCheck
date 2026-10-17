package com.mycompany.cleancheck.rules;

/** Short learning notes shown next to each finding. */
public final class RuleEducation {

    private RuleEducation() {
    }

    public static String forRule(String ruleName) {
        if (ruleName == null || ruleName.isBlank()) {
            return "Select an issue to see the related principle.";
        }

        return switch (ruleName.trim()) {
            case "Encapsulation" -> note(
                    "Encapsulation",
                    "Non-private fields can be changed from outside the class and break object state.",
                    "private String name;"
            );
            case "Empty Method" -> note(
                    "No dead code",
                    "An empty method usually means unfinished work or unused code.",
                    "Implement the logic or remove the method."
            );
            case "Empty If" -> note(
                    "Clear control flow",
                    "An empty if-block hides incomplete logic.",
                    "if (ready) { process(); }"
            );
            case "Empty While" -> note(
                    "Clear loops",
                    "An empty while-loop is incomplete or useless.",
                    "while (hasNext) { process(); }"
            );
            case "Infinite Loop" -> note(
                    "Safe loops",
                    "while(true) without a clear exit can freeze the program.",
                    "while (running) { if (done) break; }"
            );
            case "Too Many Parameters" -> note(
                    "Parameter Object",
                    "Too many parameters make methods hard to read and test.",
                    "Group related values into a small object."
            );
            case "Naming Convention" -> note(
                    "Meaningful names",
                    "Clear names reduce reading time and mistakes.",
                    "ClassName / methodName / fieldName"
            );
            case "Data Type" -> note(
                    "Type safety",
                    "Invalid or unsuitable types cause bugs or weak precision.",
                    "Use String (not string). Prefer BigDecimal for money."
            );
            case "Empty Catch" -> note(
                    "Exception handling",
                    "An empty catch hides failures and makes debugging hard.",
                    "Log the error or handle it properly."
            );
            case "Magic Number" -> note(
                    "Named constants",
                    "Unnamed numbers hide business meaning.",
                    "private static final int MAX_RETRIES = 3;"
            );
            case "Long Method" -> note(
                    "Single Responsibility",
                    "Long methods usually do more than one job.",
                    "Split into smaller methods with clear names."
            );
            case "System Out" -> note(
                    "Logging",
                    "System.out is fine for demos, not for real applications.",
                    "Use a logger instead of println."
            );
            case "Missing Package" -> note(
                    "Packages",
                    "Without a package, organization and naming are weaker.",
                    "package com.example.app;"
            );
            default -> com.mycompany.cleancheck.security.SecurityCatalog.info(ruleName)
                    .map(info -> note(info.cwe() + "  \u00B7  " + info.owasp(), info.attack(), info.fix()))
                    .orElse("No learning note for this rule yet.");
        };
    }

    private static String note(String principle, String why, String example) {
        return "Principle: " + principle
                + "\nWhy: " + why
                + "\nExample: " + example;
    }
}
