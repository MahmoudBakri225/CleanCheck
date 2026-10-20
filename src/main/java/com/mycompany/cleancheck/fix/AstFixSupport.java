package com.mycompany.cleancheck.fix;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;

/**
 * AST-based automatic fixes. Prefer these over line/regex edits when the file parses.
 * Lexical preservation keeps original formatting where JavaParser allows it.
 */
public final class AstFixSupport {

    private AstFixSupport() {}

    public static Optional<String> removeImportAtLine(String source, int lineNumber) {
        try {
            CompilationUnit cu = parse(source);
            boolean removed = false;
            for (ImportDeclaration imp : cu.getImports()) {
                if (lineOf(imp) == lineNumber) {
                    imp.remove();
                    removed = true;
                    break;
                }
            }
            return removed ? print(cu) : Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public static Optional<String> ensurePackage(String source, Path javaFile) {
        try {
            CompilationUnit cu = parse(source);
            if (cu.getPackageDeclaration().isPresent()) {
                return Optional.empty();
            }
            String pkg = packageFromPath(javaFile);
            if (pkg == null || pkg.isBlank()) {
                return Optional.empty();
            }
            cu.setPackageDeclaration(new PackageDeclaration(StaticJavaParser.parseName(pkg)));
            return print(cu);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public static Optional<String> fixStringEqualityAtLine(String source, int lineNumber) {
        try {
            CompilationUnit cu = parse(source);
            boolean changed = false;
            for (BinaryExpr bin : cu.findAll(BinaryExpr.class)) {
                if (lineOf(bin) != lineNumber) {
                    continue;
                }
                if (bin.getOperator() != BinaryExpr.Operator.EQUALS
                        && bin.getOperator() != BinaryExpr.Operator.NOT_EQUALS) {
                    continue;
                }
                Expression left = bin.getLeft();
                Expression right = bin.getRight();
                StringLiteralExpr lit = null;
                Expression other = null;
                if (left.isStringLiteralExpr()) {
                    lit = left.asStringLiteralExpr();
                    other = right;
                } else if (right.isStringLiteralExpr()) {
                    lit = right.asStringLiteralExpr();
                    other = left;
                }
                if (lit == null || other == null) {
                    continue;
                }
                MethodCallExpr equalsCall = new MethodCallExpr(lit.clone(), "equals");
                equalsCall.addArgument(other.clone());
                if (bin.getOperator() == BinaryExpr.Operator.NOT_EQUALS) {
                    bin.replace(new com.github.javaparser.ast.expr.UnaryExpr(
                            equalsCall,
                            com.github.javaparser.ast.expr.UnaryExpr.Operator.LOGICAL_COMPLEMENT));
                } else {
                    bin.replace(equalsCall);
                }
                changed = true;
            }
            return changed ? print(cu) : Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /** Removes a System.out / System.err call statement on the given line. */
    public static Optional<String> removeSystemOutAtLine(String source, int lineNumber) {
        try {
            CompilationUnit cu = parse(source);
            boolean removed = false;
            for (MethodCallExpr call : cu.findAll(MethodCallExpr.class)) {
                if (lineOf(call) != lineNumber) {
                    continue;
                }
                if (!isSystemOutOrErr(call)) {
                    continue;
                }
                Optional<Statement> stmt = call.findAncestor(Statement.class);
                if (stmt.isPresent()) {
                    stmt.get().remove();
                    removed = true;
                    break;
                }
            }
            return removed ? print(cu) : Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /** Replaces e.printStackTrace() on the line with a java.util.logging call. */
    public static Optional<String> replacePrintStackTraceAtLine(String source, int lineNumber) {
        try {
            CompilationUnit cu = parse(source);
            boolean changed = false;
            for (MethodCallExpr call : cu.findAll(MethodCallExpr.class)) {
                if (lineOf(call) != lineNumber) {
                    continue;
                }
                if (!"printStackTrace".equals(call.getNameAsString()) || !call.getArguments().isEmpty()) {
                    continue;
                }
                Expression scope = call.getScope().orElse(null);
                if (scope == null || !scope.isNameExpr()) {
                    continue;
                }
                // LOGGER.log(Level.SEVERE, "Unexpected error", e);
                NameExpr logger = new NameExpr("LOGGER");
                MethodCallExpr logCall = new MethodCallExpr(logger, "log");
                logCall.addArgument(new FieldAccessExpr(new NameExpr("Level"), "SEVERE"));
                logCall.addArgument(new StringLiteralExpr("Unexpected error"));
                logCall.addArgument(scope.clone());
                call.replace(logCall);
                // ensure imports
                ensureImport(cu, "java.util.logging.Level");
                ensureImport(cu, "java.util.logging.Logger");
                changed = true;
                break;
            }
            return changed ? print(cu) : Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /** Makes a field on the given line private (keeps static/final). */
    public static Optional<String> makeFieldPrivateAtLine(String source, int lineNumber) {
        try {
            CompilationUnit cu = parse(source);
            for (FieldDeclaration field : cu.findAll(FieldDeclaration.class)) {
                if (lineOf(field) != lineNumber) {
                    continue;
                }
                boolean isStatic = field.hasModifier(Modifier.Keyword.STATIC);
                boolean isFinal = field.hasModifier(Modifier.Keyword.FINAL);
                field.setModifiers(Modifier.Keyword.PRIVATE);
                if (isStatic) {
                    field.addModifier(Modifier.Keyword.STATIC);
                }
                if (isFinal) {
                    field.addModifier(Modifier.Keyword.FINAL);
                }
                return print(cu);
            }
            return Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /** Fixes common mistyped built-in type names on a variable/field at the line (Int→int, etc.). */
    public static Optional<String> fixBuiltinTypeAtLine(String source, int lineNumber) {
        try {
            CompilationUnit cu = parse(source);
            boolean changed = false;
            for (VariableDeclarator var : cu.findAll(VariableDeclarator.class)) {
                if (lineOf(var) != lineNumber && lineOf(var.getParentNode().orElse(null)) != lineNumber) {
                    continue;
                }
                Type t = var.getType();
                Type fixed = mapMistyped(t);
                if (fixed != null) {
                    var.setType(fixed);
                    changed = true;
                }
            }
            for (FieldDeclaration field : cu.findAll(FieldDeclaration.class)) {
                if (lineOf(field) != lineNumber) {
                    continue;
                }
                Type t = field.getElementType();
                Type fixed = mapMistyped(t);
                if (fixed != null) {
                    field.setAllTypes(fixed);
                    changed = true;
                }
            }
            return changed ? print(cu) : Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /** Removes a simple local variable declaration on the line when it is an ExpressionStmt / VariableDeclarationExpr parent. */
    public static Optional<String> removeLocalVariableAtLine(String source, int lineNumber) {
        try {
            CompilationUnit cu = parse(source);
            for (com.github.javaparser.ast.expr.VariableDeclarationExpr vde :
                    cu.findAll(com.github.javaparser.ast.expr.VariableDeclarationExpr.class)) {
                if (lineOf(vde) != lineNumber) {
                    continue;
                }
                Optional<Statement> stmt = vde.findAncestor(Statement.class);
                if (stmt.isPresent() && stmt.get() instanceof ExpressionStmt) {
                    ExpressionStmt es = (ExpressionStmt) stmt.get();
                    // If initializer has side effects (method call / new), keep the call as a statement
                    if (vde.getVariables().size() == 1) {
                        VariableDeclarator vd = vde.getVariable(0);
                        if (vd.getInitializer().isPresent()) {
                            Expression init = vd.getInitializer().get();
                            if (init.isMethodCallExpr() || init.isObjectCreationExpr()) {
                                es.setExpression(init.clone());
                                return print(cu);
                            }
                        }
                    }
                    es.remove();
                    return print(cu);
                }
            }
            return Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /** Renames a field whose declaration starts on the line: first letter to lower-case (Java field convention). */
    public static Optional<String> renameFieldFirstLetterAtLine(String source, int lineNumber) {
        try {
            CompilationUnit cu = parse(source);
            for (FieldDeclaration field : cu.findAll(FieldDeclaration.class)) {
                if (lineOf(field) != lineNumber) {
                    continue;
                }
                for (VariableDeclarator vd : field.getVariables()) {
                    String name = vd.getNameAsString();
                    if (name.isEmpty() || Character.isLowerCase(name.charAt(0))) {
                        continue;
                    }
                    String newName = Character.toLowerCase(name.charAt(0)) + name.substring(1);
                    // rename declaration
                    vd.setName(newName);
                    // rename simple name usages in this compilation unit
                    for (NameExpr ne : cu.findAll(NameExpr.class)) {
                        if (ne.getNameAsString().equals(name)) {
                            ne.setName(newName);
                        }
                    }
                    for (FieldAccessExpr fae : cu.findAll(FieldAccessExpr.class)) {
                        if (fae.getNameAsString().equals(name)
                                && fae.getScope().isThisExpr()) {
                            fae.setName(newName);
                        }
                    }
                    return print(cu);
                }
            }
            return Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /** Extracts a simple integer literal on the line into a private static final int constant. */
    public static Optional<String> extractMagicIntAtLine(String source, int lineNumber) {
        try {
            CompilationUnit cu = parse(source);
            IntegerLiteralExpr target = null;
            for (IntegerLiteralExpr lit : cu.findAll(IntegerLiteralExpr.class)) {
                if (lineOf(lit) == lineNumber) {
                    String v = lit.getValue();
                    // skip single-digit / common allowed values
                    if (v.length() < 2) {
                        continue;
                    }
                    target = lit;
                    break;
                }
            }
            if (target == null) {
                return Optional.empty();
            }
            String value = target.getValue();
            String constName = "VALUE_" + value.replace("-", "NEG_");
            // avoid duplicate constant names
            int suffix = 1;
            String base = constName;
            while (nameExists(cu, constName)) {
                constName = base + "_" + suffix++;
            }
            target.replace(new NameExpr(constName));
            // insert constant as first member of the primary type
            var typeOpt = cu.getTypes().stream().findFirst();
            if (typeOpt.isEmpty()) {
                return Optional.empty();
            }
            FieldDeclaration fd = StaticJavaParser.parseBodyDeclaration(
                    "private static final int " + constName + " = " + value + ";").asFieldDeclaration();
            typeOpt.get().getMembers().add(0, fd);
            return print(cu);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    // ---- helpers ----

    private static CompilationUnit parse(String source) {
        CompilationUnit cu = StaticJavaParser.parse(source);
        LexicalPreservingPrinter.setup(cu);
        return cu;
    }

    private static Optional<String> print(CompilationUnit cu) {
        try {
            return Optional.of(LexicalPreservingPrinter.print(cu));
        } catch (Exception e) {
            return Optional.of(cu.toString());
        }
    }

    private static int lineOf(com.github.javaparser.ast.Node node) {
        if (node == null) {
            return -1;
        }
        return node.getBegin().map(p -> p.line).orElse(-1);
    }

    private static boolean isSystemOutOrErr(MethodCallExpr call) {
        return call.getScope()
                .map(s -> {
                    String t = s.toString();
                    return t.equals("System.out") || t.equals("System.err")
                            || t.endsWith(".out") && t.contains("System")
                            || t.endsWith(".err") && t.contains("System");
                })
                .orElse(false);
    }

    private static void ensureImport(CompilationUnit cu, String qualified) {
        boolean exists = cu.getImports().stream()
                .anyMatch(i -> i.getNameAsString().equals(qualified));
        if (!exists) {
            cu.addImport(qualified);
        }
    }

    private static Type mapMistyped(Type t) {
        String raw = t.asString();
        return switch (raw) {
            case "Int" -> PrimitiveType.intType();
            case "Char" -> PrimitiveType.charType();
            case "string" -> new ClassOrInterfaceType(null, "String");
            case "bool", "Bool" -> PrimitiveType.booleanType();
            default -> null;
        };
    }

    private static boolean nameExists(CompilationUnit cu, String name) {
        return cu.findAll(VariableDeclarator.class).stream()
                .anyMatch(v -> v.getNameAsString().equals(name));
    }

    private static String packageFromPath(Path javaFile) {
        Path parent = javaFile.toAbsolutePath().normalize().getParent();
        if (parent == null) {
            return null;
        }
        String path = parent.toString().replace('\\', '/');
        int idx = path.lastIndexOf("/src/main/java/");
        if (idx < 0) {
            idx = path.lastIndexOf("/src/test/java/");
        }
        if (idx < 0) {
            idx = path.lastIndexOf("/java/");
            if (idx >= 0) {
                idx += "/java/".length() - 1;
            }
        } else if (path.contains("/src/main/java/")) {
            idx = path.lastIndexOf("/src/main/java/") + "/src/main/java/".length() - 1;
        } else {
            idx = path.lastIndexOf("/src/test/java/") + "/src/test/java/".length() - 1;
        }
        if (idx < 0 || idx + 1 >= path.length()) {
            return null;
        }
        String rel = path.substring(idx + 1);
        if (rel.isBlank()) {
            return null;
        }
        return rel.replace('/', '.');
    }
}
