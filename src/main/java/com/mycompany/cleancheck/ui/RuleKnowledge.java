package com.mycompany.cleancheck.ui;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Plain-language knowledge base for every CleanCheck rule: what is wrong, why it matters, how to fix it
 * and a before/after example. UI-free, so the Issues, Rules, Analyze and Class Mode screens all read from
 * one place and a test can check that no rule is missing.
 */
public final class RuleKnowledge {

    /** One rule, explained. {@code autoFix} mirrors {@code FixEngine.supportsAutomaticFix}. */
    public record Entry(
            String rule,
            String category,
            String problem,
            String why,
            List<String> steps,
            String badExample,
            String goodExample,
            boolean autoFix) {
    }

    public static final String CORRECTNESS = "Correctness";
    public static final String ERROR_HANDLING = "Error handling";
    public static final String DESIGN = "Design";
    public static final String MAINTAINABILITY = "Maintainability";
    public static final String READABILITY = "Readability";
    public static final String STYLE = "Style";
    public static final String SECURITY = "Security";

    private static final Map<String, Entry> ENTRIES = new LinkedHashMap<>();

    static {
        add(CORRECTNESS, "Infinite Loop",
                "A loop has no condition that can ever become false and no break or return inside it.",
                "The program hangs: the thread never finishes and the application can freeze.",
                List.of("Replace 'true' with a real condition, for example a 'running' flag.",
                        "Or add a break/return that is reachable on every path through the loop.",
                        "Make sure something inside the loop can change the condition."),
                "while (true) {\n}",
                "while (running) {\n    if (done) { break; }\n}", false);
        add(CORRECTNESS, "Empty If",
                "The if statement has an empty block, so the condition is checked but nothing happens.",
                "Usually a forgotten branch or a leftover from refactoring. It hides a bug and confuses readers.",
                List.of("Write the missing logic inside the block.",
                        "If the check is no longer needed, delete the whole if statement.",
                        "If only the else part matters, invert the condition and drop the empty block."),
                "if (age > 18) {\n}",
                "if (age > ADULT_AGE) {\n    allowAccess();\n}", false);
        add(CORRECTNESS, "Empty While",
                "A while loop has an empty body.",
                "It either does nothing or spins and burns CPU while waiting. It almost always means unfinished code.",
                List.of("Add the loop logic.",
                        "Remove the loop if it is not needed.",
                        "If you really wait for something, use a proper wait mechanism instead of spinning."),
                "while (!queue.isEmpty()) {\n}",
                "while (!queue.isEmpty()) {\n    process(queue.poll());\n}", false);
        add(CORRECTNESS, "Data Type",
                "A type is invalid as written (like 'string') or unsuitable for the data (like double for money).",
                "'string' does not compile in Java, and double gives rounding errors that are unacceptable for money.",
                List.of("Write String with a capital S, and int / char in lowercase.",
                        "Use BigDecimal for money values.",
                        "Compile again to confirm the type is accepted."),
                "public string name;\nprivate double salary;",
                "public String name;\nprivate BigDecimal salary;", true);
        add(CORRECTNESS, "String Comparison",
                "Strings are compared with == or !=, which compares object references, not text.",
                "Two strings with the same text can still be different objects, so the result is unpredictable.",
                List.of("Use equals() to compare text.",
                        "Put the literal first to avoid a NullPointerException.",
                        "Use equalsIgnoreCase() when the case does not matter."),
                "if (name == \"admin\") { ... }",
                "if (\"admin\".equals(name)) { ... }", true);
        add(CORRECTNESS, "Equals HashCode",
                "A class overrides equals() without hashCode() (or the other way round).",
                "HashMap and HashSet break: two equal objects can land in different buckets and be treated as different.",
                List.of("Override both methods using the same fields.",
                        "Use Objects.hash(...) to build hashCode().",
                        "Or turn the class into a record, which generates both correctly."),
                "@Override\npublic boolean equals(Object o) { ... }",
                "@Override\npublic boolean equals(Object o) { ... }\n@Override\npublic int hashCode() { return Objects.hash(id); }", false);
        add(CORRECTNESS, "Parse Error",
                "The file could not be parsed, so none of the other rules could check it.",
                "A file that does not compile cannot be analysed, so the results for it are incomplete.",
                List.of("Read the message: it gives the line of the syntax error.",
                        "Look for a missing brace, semicolon, parenthesis or a typo.",
                        "Run the analysis again."),
                "public void run( {",
                "public void run() {", false);

        add(ERROR_HANDLING, "Empty Catch",
                "A catch block swallows the exception and does nothing.",
                "The program carries on as if nothing happened, and the failure becomes almost impossible to debug.",
                List.of("Log the exception with a logger.",
                        "Rethrow it, or wrap it in a clearer exception.",
                        "If you really ignore it, handle it (default value) and add a comment saying why."),
                "catch (NumberFormatException e) {\n}",
                "catch (NumberFormatException e) {\n    LOGGER.warning(\"Bad number: \" + e.getMessage());\n}", false);
        add(ERROR_HANDLING, "Generic Catch",
                "The code catches Exception or Throwable, which includes errors it never planned for.",
                "Unrelated bugs (a NullPointerException, for example) get hidden inside the same handler.",
                List.of("Catch only the specific exceptions the code can throw.",
                        "Use several catch blocks, or a multi-catch, when the handling differs.",
                        "Keep a generic catch only at the outer edge of the program, where you log and stop."),
                "catch (Exception e) { ... }",
                "catch (IOException | NumberFormatException e) { ... }", false);
        add(ERROR_HANDLING, "Unclosed Resource",
                "A stream, reader or scanner is opened without try-with-resources.",
                "If an exception happens before close() is reached, the file or socket stays open (a resource leak).",
                List.of("Open the resource in a try-with-resources header.",
                        "Delete the manual close() call, Java now closes it for you.",
                        "Do not close System.in."),
                "FileReader r = new FileReader(file);\n...\nr.close();",
                "try (FileReader r = new FileReader(file)) {\n    ...\n}", false);

        add(DESIGN, "Encapsulation",
                "A field is visible to every other class, so anyone can change it directly.",
                "Other code can put the object into an invalid state (a negative age, for example), and you can no longer change how the data is stored without breaking callers.",
                List.of("Make the field private.",
                        "Add a getter, and a setter only if outside code really must change it.",
                        "Validate the value inside the setter."),
                "public String name;",
                "private String name;\npublic String getName() { return name; }", true);
        add(DESIGN, "Too Many Parameters",
                "A method takes more parameters than the limit (default 4).",
                "Long parameter lists are easy to mix up, hard to read and hard to test.",
                List.of("Group related values into a small class or record.",
                        "Pass that object instead of the separate values.",
                        "Keep only the values the method really needs."),
                "void setData(String name, int age, String address, String phone, String email)",
                "void setData(Person person)", false);
        add(DESIGN, "SOLID SRP",
                "A class has too many public behaviour methods (default limit 12), so it is probably doing several jobs (Single Responsibility Principle).",
                "A class with many responsibilities changes for many different reasons, is hard to test and breaks easily when one part is edited.",
                List.of("List what the class is responsible for and group the methods by job.",
                        "Move each group into its own small class.",
                        "Let the original class delegate to the new ones, or remove it."),
                "class OrderService {\n    void createOrder() { }\n    void sendEmail() { }\n    void printInvoice() { }\n    void saveToDatabase() { }\n    ...\n}",
                "class OrderService { void createOrder() { } }\nclass EmailSender { void sendEmail() { } }\nclass InvoicePrinter { void printInvoice() { } }\nclass OrderRepository { void save() { } }", false);
        add(DESIGN, "SOLID OCP",
                "A switch statement has too many branches (default limit 8), so adding a new case means editing this code again (Open/Closed Principle).",
                "Every new type or option forces a change in code that already works, which risks new bugs and spreads the same switch across the project.",
                List.of("Create an interface for the behaviour that differs.",
                        "Write one class per case (a strategy).",
                        "Pick the right class once, then call it instead of switching."),
                "switch (type) {\n    case CARD -> payByCard();\n    case CASH -> payByCash();\n    ... // many more cases\n}",
                "interface Payment { void pay(); }\nclass CardPayment implements Payment { ... }\nclass CashPayment implements Payment { ... }\npayment.pay();", false);
        add(DESIGN, "SOLID LSP",
                "An overriding method only throws UnsupportedOperationException, so the subclass cannot do what its parent promises (Liskov Substitution Principle).",
                "Code that works with the parent type will crash at run time when it receives this subclass, so the subclass cannot safely replace its parent.",
                List.of("Ask whether the subclass really is a kind of its parent.",
                        "If not, use a smaller interface or composition instead of inheritance.",
                        "If yes, implement the method so the parent's contract still holds."),
                "class Penguin extends Bird {\n    @Override void fly() {\n        throw new UnsupportedOperationException();\n    }\n}",
                "interface Bird { void eat(); }\ninterface FlyingBird extends Bird { void fly(); }\nclass Penguin implements Bird { ... }", false);
        add(DESIGN, "SOLID ISP",
                "An interface declares too many methods (default limit 10), so classes that implement it are forced to carry methods they do not need (Interface Segregation Principle).",
                "Implementers end up with empty or unsupported methods, and every change to the big interface affects classes that never use that part.",
                List.of("Group the methods by the role that uses them.",
                        "Split the interface into small role-focused interfaces.",
                        "Let each class implement only the roles it needs."),
                "interface Machine {\n    void print();\n    void scan();\n    void fax();\n    ... // many more methods\n}",
                "interface Printer { void print(); }\ninterface Scanner { void scan(); }\nclass SimplePrinter implements Printer { ... }", false);
        add(DESIGN, "SOLID DIP",
                "A field is created directly with new on a concrete class, so this class is tied to that exact implementation (Dependency Inversion Principle).",
                "You cannot swap the implementation or replace it with a fake in tests, and a change in the concrete class ripples into this one.",
                List.of("Declare the field with an interface type.",
                        "Receive the dependency through the constructor instead of creating it.",
                        "Create the concrete object in one place, at the start of the program."),
                "private MySqlDatabase db = new MySqlDatabase();",
                "private final Database db;\nOrderService(Database db) { this.db = db; }", false);

        add(MAINTAINABILITY, "Empty Method",
                "The method has no body.",
                "It usually means unfinished work or leftover code, and callers assume it does something.",
                List.of("Decide whether the method is still needed.",
                        "If yes, implement it (or throw UnsupportedOperationException to make the gap explicit).",
                        "If no, delete it together with its callers."),
                "public void save() {\n}",
                "public void save() {\n    repository.store(data);\n}", false);
        add(MAINTAINABILITY, "Unused Variable",
                "A local variable is assigned but never read.",
                "It is dead code, and sometimes a sign that a value was supposed to be used and a bug slipped in.",
                List.of("Delete the variable.",
                        "Or use it where the value was meant to be used.",
                        "If the initializer has a side effect, keep the call and drop only the variable."),
                "int x = Integer.parseInt(text);",
                "int count = Integer.parseInt(text);\ntotal += count;", true);
        add(MAINTAINABILITY, "Long Method",
                "A method is longer than the limit (default 40 lines).",
                "Long methods do several jobs, which makes them hard to read, test and change.",
                List.of("Find blocks that do a separate job.",
                        "Extract each block into a well-named method.",
                        "Leave the original method reading like a short story."),
                "void process() {\n    // 80 lines\n}",
                "void process() {\n    validate();\n    calculate();\n    save();\n}", false);
        add(MAINTAINABILITY, "Cyclomatic Complexity",
                "A method has too many branches (if, for, while, case, && and ||); the limit is 10 by default.",
                "Every branch multiplies the number of cases you must test, so bugs hide in the combinations.",
                List.of("Extract groups of branches into small methods.",
                        "Replace nested ifs with early returns.",
                        "Use a switch or a lookup map instead of long if-else chains."),
                "if (a) { if (b) { for (...) { if (c) { ... } } } }",
                "if (!a || !b) { return; }\nhandleItems(items);", false);
        add(MAINTAINABILITY, "Class Length",
                "A class is longer than the limit (default 500 lines).",
                "A class that big usually has several responsibilities and is hard to understand or change safely.",
                List.of("Group the methods by responsibility.",
                        "Move each group into its own class.",
                        "Aim for one reason to change per class."),
                "class App {\n    // 900 lines\n}",
                "class App {\n    OrderService orders;\n    ReportService reports;\n}", false);
        add(MAINTAINABILITY, "Duplicate Code",
                "Two methods have the same body.",
                "A bug fixed in one copy is easily forgotten in the other.",
                List.of("Extract the shared code into one method.",
                        "Call it from both places.",
                        "If the copies differ slightly, pass the difference as a parameter."),
                "int totalA() { ...same 6 lines... }\nint totalB() { ...same 6 lines... }",
                "int total(List<Item> items) { ...6 lines... }", false);

        add(READABILITY, "Magic Number",
                "A raw number appears in the code without a name.",
                "Readers cannot tell what the number means, and changing it later means hunting through the whole code.",
                List.of("Create a constant with a meaningful name.",
                        "Replace every use of the number with the constant.",
                        "Keep 0, 1 and -1 as they are, they are usually clear."),
                "if (age > 18) { ... }",
                "private static final int ADULT_AGE = 18;\nif (age > ADULT_AGE) { ... }", true);
        add(READABILITY, "Deep Nesting",
                "Blocks are nested deeper than the limit (default 4 levels).",
                "Code that is indented four or five levels deep is hard to follow and easy to break.",
                List.of("Use early returns (guard clauses) to remove one level.",
                        "Extract the inner block into its own method.",
                        "Combine conditions that are checked together."),
                "if (a) { if (b) { if (c) { run(); } } }",
                "if (!a || !b || !c) { return; }\nrun();", false);

        add(STYLE, "System Out",
                "Output goes straight to the console with System.out or System.err.",
                "You cannot control the level, format or destination, and real applications become noisy.",
                List.of("Create a Logger for the class.",
                        "Replace println with logger.info or logger.warning.",
                        "Delete debug prints that are no longer needed."),
                "System.out.println(name);",
                "LOGGER.info(\"name = \" + name);", true);
        add(STYLE, "Naming Convention",
                "A class, method or field name breaks the Java naming rules.",
                "Inconsistent names slow reading and make code look unprofessional.",
                List.of("Classes use PascalCase, methods and fields use camelCase.",
                        "Rename with your IDE's refactor tool so every use is updated.",
                        "Constants stay UPPER_SNAKE_CASE."),
                "class student { int Age; }",
                "class Student { int age; }", true);
        add(STYLE, "Missing Package",
                "The file has no package declaration.",
                "Classes without a package go into the default package, cannot be imported and clash easily.",
                List.of("Add a package line that matches the folder structure.",
                        "Keep folders and packages identical."),
                "public class Student { }",
                "package com.example.school;\npublic class Student { }", true);
        add(STYLE, "Unused Import",
                "An import is never used in the file.",
                "It clutters the file and suggests a dependency that does not exist.",
                List.of("Delete the import line.",
                        "Or let your IDE organize imports automatically."),
                "import java.util.List;   // never used",
                "// import removed", true);
        for (com.mycompany.cleancheck.security.SecurityCatalog.Info info
                : com.mycompany.cleancheck.security.SecurityCatalog.all()) {
            add(SECURITY, info.rule(),
                    info.problem() + " (" + info.cwe() + ", " + info.owasp() + ")",
                    info.attack(), info.steps(), info.bad(), info.good(),
                    com.mycompany.cleancheck.fix.FixEngine.supportsAutomaticFix(info.rule(), null));
        }
    }

    private RuleKnowledge() {
    }

    private static void add(String category, String rule, String problem, String why,
            List<String> steps, String bad, String good, boolean autoFix) {
        ENTRIES.put(rule.toLowerCase(Locale.ROOT),
                new Entry(rule, category, problem, why, List.copyOf(steps), bad, good, autoFix));
    }

    /** Never null: an unknown rule gets a generic explanation. */
    public static Entry forRule(String ruleName) {
        String name = ruleName == null ? "" : ruleName.trim();
        Entry entry = ENTRIES.get(name.toLowerCase(Locale.ROOT));
        if (entry != null) {
            return entry;
        }
        return new Entry(name.isEmpty() ? "Unknown rule" : name, MAINTAINABILITY,
                "CleanCheck found something worth a second look here.",
                "Findings like this make the code harder to read, test or change.",
                List.of("Read the message and the suggested fix.",
                        "Change the code, then run the analysis again to confirm it is gone."),
                "", "", false);
    }

    public static boolean isKnown(String ruleName) {
        return ruleName != null && ENTRIES.containsKey(ruleName.trim().toLowerCase(Locale.ROOT));
    }

    /** Every rule, grouped by category in display order. */
    public static List<Entry> all() {
        return List.copyOf(ENTRIES.values());
    }

    /**
     * Rules that CleanCheck runs, for display counts. Parse Error is listed so it can be explained, but it is
     * a result of a file failing to parse, not a rule that is run, so it is not counted.
     */
    public static long ruleCount() {
        return ENTRIES.values().stream().filter(e -> !"Parse Error".equalsIgnoreCase(e.rule())).count();
    }

    /** Number of counted rules in one category (Parse Error is not counted, see {@link #ruleCount()}). */
    public static long countInCategory(String category) {
        return ENTRIES.values().stream()
                .filter(e -> e.category().equals(category))
                .filter(e -> !"Parse Error".equalsIgnoreCase(e.rule()))
                .count();
    }

    public static List<String> categories() {
        List<String> categories = new ArrayList<>();
        for (Entry entry : ENTRIES.values()) {
            if (!categories.contains(entry.category())) {
                categories.add(entry.category());
            }
        }
        return categories;
    }

    public static List<Entry> inCategory(String category) {
        List<Entry> result = new ArrayList<>();
        for (Entry entry : ENTRIES.values()) {
            if (entry.category().equals(category)) {
                result.add(entry);
            }
        }
        return result;
    }

    /** Names of the rules CleanCheck can fix automatically (after verification). */
    public static List<String> autoFixRules() {
        List<String> names = new ArrayList<>();
        for (Entry entry : ENTRIES.values()) {
            if (entry.autoFix()) {
                names.add(entry.rule());
            }
        }
        return names;
    }

    /** What a severity means in practice, in one sentence. */
    public static String severityMeaning(String severity) {
        if (severity == null) {
            return "";
        }
        return switch (severity.trim().toUpperCase(Locale.ROOT)) {
            case "CRITICAL" -> "Critical: likely a bug or crash. Fix this first.";
            case "WARNING" -> "Warning: a design or maintenance risk. Fix it soon.";
            case "INFO" -> "Info: polish. Fix it when convenient.";
            default -> "";
        };
    }
}
