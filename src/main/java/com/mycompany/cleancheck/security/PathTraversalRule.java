package com.mycompany.cleancheck.security;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.mycompany.cleancheck.analysis.JavaAst;
import com.mycompany.cleancheck.analysis.Severity;
import com.mycompany.cleancheck.analysis.Warning;

import java.util.*;
import java.util.regex.Pattern;

public final class PathTraversalRule extends SecurityRuleBase {


    private static final Set<String> FILE_TYPES = Set.of(
            "FileInputStream", "FileOutputStream", "FileReader", "FileWriter", "RandomAccessFile");
    private static final Set<String> FILE_IO_METHODS = Set.of(
            "readString", "readAllBytes", "readAllLines", "newInputStream", "newOutputStream",
            "newBufferedReader", "newBufferedWriter", "write", "writeString", "delete", "deleteIfExists",
            "copy", "move");
    private static final Pattern USER_PARAMETER = Pattern.compile(
            "^(?:(?i:user|input|upload|requested|filename|directory|resource)(?:$|[A-Z_].*))");
    private static final Pattern PARAMETER_SOURCE = Pattern.compile("parameter '([^']+)'");

    public PathTraversalRule() {
        super(SecurityCatalog.PATH_TRAVERSAL);
    }

    private void check(List<Warning> out, String fileName, Node at, List<Expression> arguments) {
        Node scope = SecurityAst.callableOf(at);
        String text = scope.toString();
        boolean resolvesPath = text.contains(".normalize(") || text.contains("toRealPath(")
                || text.contains("getCanonical");
        boolean checksContainment = text.contains(".startsWith(");
        if (resolvesPath && checksContainment) {
            return;
        }
        SecurityAst.Flow flow = new SecurityAst.Flow(scope);
        for (Expression argument : arguments) {
            Optional<String> source = flow.sourceIn(argument);
            if (source.filter(PathTraversalRule::isUntrustedPathSource).isPresent()) {
                add(out, fileName, at, Severity.WARNING,
                        "A file path is built from user input (" + source.get()
                                + ") and the method never checks that it stays inside a base folder.");
                return;
            }
            if (scope instanceof MethodDeclaration method) {
                for (NameExpr name : argument.findAll(NameExpr.class)) {
                    for (Parameter parameter : method.getParameters()) {
                        if (parameter.getNameAsString().equals(name.getNameAsString())
                                && USER_PARAMETER.matcher(parameter.getNameAsString()).find()) {
                            add(out, fileName, at, Severity.WARNING, "A file path is built from the parameter '"
                                    + parameter.getNameAsString()
                                    + "' and the method never checks that it stays inside a base folder.");
                            return;
                        }
                    }
                }
            }
        }
    }

    private static boolean isUntrustedPathSource(String source) {
        if (source.startsWith("a command-line argument")) {
            return false; // CLI paths are explicit operator choices, not requests crossing a trust boundary.
        }
        var parameter = PARAMETER_SOURCE.matcher(source);
        return !parameter.find() || USER_PARAMETER.matcher(parameter.group(1)).find();
    }

    @Override
    protected void scan(CompilationUnit cu, String fileName, List<Warning> out) {
        for (ObjectCreationExpr creation : cu.findAll(ObjectCreationExpr.class)) {
            if (FILE_TYPES.contains(creation.getType().getNameAsString())
                    && !creation.getArguments().isEmpty()) {
                check(out, fileName, creation, new ArrayList<>(creation.getArguments()));
            }
        }
        for (MethodCallExpr call : cu.findAll(MethodCallExpr.class)) {
            boolean fileIo = SecurityHelpers.scopeIs(call, "Files") && FILE_IO_METHODS.contains(call.getNameAsString());
            if (fileIo && !call.getArguments().isEmpty()) {
                check(out, fileName, call, new ArrayList<>(call.getArguments()));
            }
        }
    }

}
