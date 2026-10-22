package com.mycompany.cleancheck.security;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.mycompany.cleancheck.analysis.JavaAst;
import com.mycompany.cleancheck.analysis.Severity;
import com.mycompany.cleancheck.analysis.Warning;
import com.mycompany.cleancheck.rules.Rule;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * The security rules ("preliminary security review"). Each one looks for a known dangerous pattern in a single
 * file with the Java AST. They do not follow data between files, so they can raise false alarms and can miss
 * a flaw that crosses class boundaries; findings are leads to check, not proof.
 */
public final class SecurityRules {

    private SecurityRules() {
    }

    public static List<Rule> all() {
        return List.of(
                new SqlInjectionRule(),
                new HardcodedSecretRule(),
                new WeakCryptoRule(),
                new InsecureRandomRule(),
                new CommandInjectionRule(),
                new PathTraversalRule(),
                new UnsafeDeserializationRule(),
                new CertificateCheckRule(),
                new InformationLeakRule(),
                new XxeRule());
    }

    // ---------------------------------------------------------------------

    private abstract static class Base implements Rule {

        private final String name;

        Base(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public List<Warning> analyze(String fileName, String source) {
            List<Warning> found = new ArrayList<>();
            JavaAst.parse(source).ifPresent(cu -> scan(cu, fileName, found));
            return found;
        }

        abstract void scan(CompilationUnit cu, String fileName, List<Warning> out);

        /** Same as add(), but with a fix sentence that fits this exact finding instead of the rule default. */
        final void add(List<Warning> out, String fileName, Node at, Severity severity, String message, String fix) {
            String cwe = SecurityCatalog.info(name).map(SecurityCatalog.Info::cwe).orElse("CWE");
            out.add(new Warning(fileName, JavaAst.line(at), name, severity, "[" + cwe + "] " + message, fix));
        }

        final void add(List<Warning> out, String fileName, Node at, Severity severity, String message) {
            String cwe = SecurityCatalog.info(name).map(SecurityCatalog.Info::cwe).orElse("CWE");
            String fix = SecurityCatalog.info(name).map(SecurityCatalog.Info::fix).orElse("");
            out.add(new Warning(fileName, JavaAst.line(at), name, severity, "[" + cwe + "] " + message, fix));
        }
    }

    private static String scopeText(MethodCallExpr call) {
        return call.getScope().map(Expression::toString).orElse("");
    }

    private static boolean scopeIs(MethodCallExpr call, String simpleName) {
        String scope = scopeText(call);
        return scope.equals(simpleName) || scope.endsWith("." + simpleName);
    }

    private static Optional<String> stringArg(MethodCallExpr call, int index) {
        if (call.getArguments().size() > index && call.getArgument(index).isStringLiteralExpr()) {
            return Optional.of(call.getArgument(index).asStringLiteralExpr().asString());
        }
        return Optional.empty();
    }

    // ---------------------------------------------------------------------
    // CWE-89
    // ---------------------------------------------------------------------

    private static final class SqlInjectionRule extends Base {

        private static final Set<String> SQL_CALLS = Set.of(
                "executeQuery", "executeUpdate", "execute", "executeLargeUpdate", "prepareStatement",
                "prepareCall", "createQuery", "createNativeQuery", "addBatch", "queryForObject", "queryForList");
        private static final Set<String> SQL_RECEIVER_TYPES = Set.of(
                "Statement", "PreparedStatement", "CallableStatement", "Connection",
                "JdbcTemplate", "NamedParameterJdbcTemplate", "JdbcOperations",
                "EntityManager", "Query", "SQLQuery", "DSLContext");

        SqlInjectionRule() {
            super(SecurityCatalog.SQL_INJECTION);
        }

        @Override
        void scan(CompilationUnit cu, String fileName, List<Warning> out) {
            for (MethodCallExpr call : cu.findAll(MethodCallExpr.class)) {
                if (!SQL_CALLS.contains(call.getNameAsString()) || call.getArguments().isEmpty()
                        || !hasSqlReceiver(call, cu)) {
                    continue;
                }
                Expression query = call.getArgument(0);
                if (!SecurityAst.isBuiltDynamically(query, call)) {
                    continue;
                }
                Optional<String> source = new SecurityAst.Flow(SecurityAst.callableOf(call)).sourceIn(query);
                // Dynamic SQL is always reported; parameter/user taint makes the message clearer.
                String message = "The query passed to " + call.getNameAsString()
                        + "() is built by joining strings"
                        + source.map(s -> ", and untrusted input (" + s + ") reaches it.")
                                .orElse(" (use a PreparedStatement with placeholders).");
                add(out, fileName, call, Severity.CRITICAL, message);
            }
        }

        private static boolean hasSqlReceiver(MethodCallExpr call, CompilationUnit unit) {
            if (call.getScope().isEmpty()) {
                return false;
            }
            Expression scope = call.getScope().get();
            if (scope.isMethodCallExpr()) {
                String factory = scope.asMethodCallExpr().getNameAsString();
                if (factory.equals("createStatement") || factory.equals("prepareStatement")
                        || factory.equals("prepareCall")) {
                    return true;
                }
            }
            String variable = null;
            if (scope.isNameExpr()) {
                variable = scope.asNameExpr().getNameAsString();
            } else if (scope.isFieldAccessExpr()) {
                variable = scope.asFieldAccessExpr().getNameAsString();
            }
            if (variable == null) {
                return false;
            }
            final String receiverName = variable;
            boolean declaredSqlVariable = unit.findAll(VariableDeclarator.class).stream()
                    .filter(declaration -> declaration.getNameAsString().equals(receiverName))
                    .anyMatch(declaration -> isSqlReceiverType(declaration.getType().asString()));
            if (declaredSqlVariable) {
                return true;
            }
            return unit.findAll(Parameter.class).stream()
                    .filter(parameter -> parameter.getNameAsString().equals(receiverName))
                    .anyMatch(parameter -> isSqlReceiverType(parameter.getType().asString()));
        }

        private static boolean isSqlReceiverType(String type) {
            int genericStart = type.indexOf('<');
            String erased = genericStart < 0 ? type : type.substring(0, genericStart);
            int packageSeparator = erased.lastIndexOf('.');
            String simpleName = packageSeparator < 0 ? erased : erased.substring(packageSeparator + 1);
            return SQL_RECEIVER_TYPES.contains(simpleName);
        }
    }

    // ---------------------------------------------------------------------
    // CWE-798
    // ---------------------------------------------------------------------

    private static final class HardcodedSecretRule extends Base {

        private static final List<String> SUFFIXES = List.of(
                "password", "passwd", "pwd", "passphrase", "secret", "secretkey", "apikey", "accesskey",
                "privatekey", "token", "credentials");
        private static final Pattern AWS_KEY = Pattern.compile("AKIA[0-9A-Z]{16}");
        private static final Pattern PRIVATE_KEY = Pattern.compile("-----BEGIN ([A-Z]+ )?PRIVATE KEY-----");

        HardcodedSecretRule() {
            super(SecurityCatalog.HARDCODED_SECRET);
        }

        private static boolean secretName(String name) {
            String normalized = name.toLowerCase(Locale.ROOT).replace("_", "").replace("-", "");
            for (String suffix : SUFFIXES) {
                if (normalized.endsWith(suffix)) {
                    return true;
                }
            }
            return false;
        }

        private static boolean realValue(Expression value) {
            if (!value.isStringLiteralExpr()) {
                return false;
            }
            String text = value.asStringLiteralExpr().asString();
            return !text.isBlank() && !text.contains(" ")
                    && !text.startsWith("${") && !text.startsWith("<") && !text.startsWith("{") && !text.startsWith("%");
        }

        @Override
        void scan(CompilationUnit cu, String fileName, List<Warning> out) {
            Set<Integer> reported = new HashSet<>();

            for (VariableDeclarator declarator : cu.findAll(VariableDeclarator.class)) {
                if (secretName(declarator.getNameAsString()) && declarator.getInitializer().isPresent()
                        && realValue(declarator.getInitializer().get()) && reported.add(JavaAst.line(declarator))) {
                    add(out, fileName, declarator, Severity.CRITICAL, "'" + declarator.getNameAsString()
                            + "' is set to a literal value in the source code (value hidden).");
                }
            }
            for (AssignExpr assign : cu.findAll(AssignExpr.class)) {
                String target = assign.getTarget().isNameExpr()
                        ? assign.getTarget().asNameExpr().getNameAsString()
                        : assign.getTarget().isFieldAccessExpr()
                                ? assign.getTarget().asFieldAccessExpr().getNameAsString() : "";
                if (!target.isEmpty() && secretName(target) && realValue(assign.getValue())
                        && reported.add(JavaAst.line(assign))) {
                    add(out, fileName, assign, Severity.CRITICAL,
                            "'" + target + "' is assigned a literal value in the source code (value hidden).");
                }
            }
            for (MethodCallExpr call : cu.findAll(MethodCallExpr.class)) {
                String name = call.getNameAsString();
                boolean connection = name.equals("getConnection") && call.getArguments().size() == 3
                        && realValue(call.getArgument(2));
                boolean setter = name.startsWith("set") && secretName(name.substring(3))
                        && call.getArguments().size() == 1 && realValue(call.getArgument(0));
                if ((connection || setter) && reported.add(JavaAst.line(call))) {
                    add(out, fileName, call, Severity.CRITICAL,
                            "A literal secret is passed to " + name + "() (value hidden).");
                }
            }
            for (StringLiteralExpr literal : cu.findAll(StringLiteralExpr.class)) {
                String text = literal.asString();
                if ((AWS_KEY.matcher(text).find() || PRIVATE_KEY.matcher(text).find())
                        && reported.add(JavaAst.line(literal))) {
                    add(out, fileName, literal, Severity.CRITICAL,
                            "The text looks like a cloud access key or a private key (value hidden).");
                }
            }
        }
    }

    // ---------------------------------------------------------------------
    // CWE-327
    // ---------------------------------------------------------------------

    private static final class WeakCryptoRule extends Base {

        private static final Set<String> WEAK_HASHES = Set.of("MD2", "MD4", "MD5", "SHA-1", "SHA1");
        private static final Set<String> WEAK_CIPHERS = Set.of("DES", "DESEDE", "RC2", "RC4", "ARCFOUR", "BLOWFISH");

        WeakCryptoRule() {
            super(SecurityCatalog.WEAK_CRYPTO);
        }

        @Override
        void scan(CompilationUnit cu, String fileName, List<Warning> out) {
            for (MethodCallExpr call : cu.findAll(MethodCallExpr.class)) {
                if (!call.getNameAsString().equals("getInstance")) {
                    continue;
                }
                Optional<String> argument = stringArg(call, 0);
                if (argument.isEmpty()) {
                    continue;
                }
                String value = argument.get();
                String upper = value.toUpperCase(Locale.ROOT);
                String algorithm = upper.split("/")[0];
                if (scopeIs(call, "MessageDigest") && WEAK_HASHES.contains(upper)) {
                    add(out, fileName, call, Severity.WARNING,
                            value + " is a broken hash: collisions can be created on purpose.");
                } else if ((scopeIs(call, "Cipher") || scopeIs(call, "KeyGenerator")
                        || scopeIs(call, "SecretKeyFactory")) && WEAK_CIPHERS.contains(algorithm)) {
                    add(out, fileName, call, Severity.WARNING,
                            value + " is a weak or obsolete cipher.",
                            "Use AES with an authenticated mode, for example AES/GCM/NoPadding.");
                } else if (scopeIs(call, "Cipher") && upper.contains("/ECB")) {
                    add(out, fileName, call, Severity.WARNING,
                            "ECB mode (" + value + ") encrypts equal blocks to equal output and leaks patterns.",
                            "Use an authenticated mode such as AES/GCM/NoPadding with a fresh random IV.");
                } else if (scopeIs(call, "Cipher") && upper.equals("AES")) {
                    add(out, fileName, call, Severity.WARNING,
                            "\"AES\" without a mode defaults to ECB. Name the mode explicitly, for example AES/GCM/NoPadding.",
                            "Replace \"AES\" with \"AES/GCM/NoPadding\" and use a fresh random IV for every encryption.");
                }
            }
        }
    }

    // ---------------------------------------------------------------------
    // CWE-338
    // ---------------------------------------------------------------------

    private static final class InsecureRandomRule extends Base {

        private static final Pattern SECURITY_CONTEXT = Pattern.compile(
                "token|password|passwd|secret|session|nonce|salt|csrf|apikey|api_key|privatekey|verification",
                Pattern.CASE_INSENSITIVE);

        InsecureRandomRule() {
            super(SecurityCatalog.INSECURE_RANDOM);
        }

        private static String contextOf(Node node) {
            StringBuilder context = new StringBuilder();
            Node current = node;
            while (current != null) {
                if (current instanceof VariableDeclarator declarator) {
                    context.append(' ').append(declarator.getNameAsString());
                } else if (current instanceof MethodDeclaration method) {
                    context.append(' ').append(method.getNameAsString());
                } else if (current instanceof com.github.javaparser.ast.body.TypeDeclaration<?> type) {
                    context.append(' ').append(type.getNameAsString());
                } else if (current instanceof AssignExpr assign && assign.getTarget().isNameExpr()) {
                    context.append(' ').append(assign.getTarget().asNameExpr().getNameAsString());
                }
                current = current.getParentNode().orElse(null);
            }
            return context.toString();
        }

        @Override
        void scan(CompilationUnit cu, String fileName, List<Warning> out) {
            for (ObjectCreationExpr creation : cu.findAll(ObjectCreationExpr.class)) {
                if (creation.getType().getNameAsString().equals("Random")
                        && SECURITY_CONTEXT.matcher(contextOf(creation)).find()) {
                    add(out, fileName, creation, Severity.WARNING,
                            "java.util.Random is predictable, but this looks like a security value (token, password, salt...).");
                }
            }
            for (MethodCallExpr call : cu.findAll(MethodCallExpr.class)) {
                boolean math = call.getNameAsString().equals("random") && scopeIs(call, "Math");
                boolean threadLocal = call.getNameAsString().equals("current") && scopeIs(call, "ThreadLocalRandom");
                if ((math || threadLocal) && SECURITY_CONTEXT.matcher(contextOf(call)).find()) {
                    add(out, fileName, call, Severity.WARNING,
                            (math ? "Math.random()" : "ThreadLocalRandom")
                                    + " is predictable, but this looks like a security value (token, password, salt...).");
                }
            }
        }
    }

    // ---------------------------------------------------------------------
    // CWE-78
    // ---------------------------------------------------------------------

    private static final class CommandInjectionRule extends Base {

        CommandInjectionRule() {
            super(SecurityCatalog.COMMAND_INJECTION);
        }

        private void check(List<Warning> out, String fileName, Node at, List<Expression> arguments, String what) {
            SecurityAst.Flow flow = new SecurityAst.Flow(SecurityAst.callableOf(at));
            boolean built = false;
            boolean dynamic = false;
            Optional<String> source = Optional.empty();
            for (Expression argument : arguments) {
                if (SecurityAst.isDynamic(argument)) {
                    dynamic = true;
                }
                if (SecurityAst.isBuiltDynamically(argument, at)) {
                    built = true;
                }
                if (source.isEmpty()) {
                    source = flow.sourceIn(argument);
                }
            }
            if (source.isPresent() || built) {
                add(out, fileName, at, Severity.CRITICAL, what + " runs a command assembled from variable data"
                        + source.map(s -> " that includes user input (" + s + ").").orElse("."));
            } else if (dynamic) {
                add(out, fileName, at, Severity.WARNING, what
                        + " runs a command taken from a variable. Make sure it can never contain user input.");
            }
        }

        @Override
        void scan(CompilationUnit cu, String fileName, List<Warning> out) {
            for (MethodCallExpr call : cu.findAll(MethodCallExpr.class)) {
                if (call.getNameAsString().equals("exec") && scopeText(call).endsWith("getRuntime()")
                        && !call.getArguments().isEmpty()) {
                    Expression first = call.getArgument(0);
                    List<Expression> arguments = new ArrayList<>();
                    if (first.isArrayCreationExpr() && first.asArrayCreationExpr().getInitializer().isPresent()) {
                        arguments.addAll(first.asArrayCreationExpr().getInitializer().get().getValues());
                    } else if (first.isArrayInitializerExpr()) {
                        arguments.addAll(first.asArrayInitializerExpr().getValues());
                    } else {
                        arguments.add(first);
                    }
                    check(out, fileName, call, arguments, "Runtime.exec()");
                }
            }
            for (ObjectCreationExpr creation : cu.findAll(ObjectCreationExpr.class)) {
                if (creation.getType().getNameAsString().equals("ProcessBuilder")
                        && !creation.getArguments().isEmpty()) {
                    check(out, fileName, creation, new ArrayList<>(creation.getArguments()), "ProcessBuilder");
                }
            }
        }
    }

    // ---------------------------------------------------------------------
    // CWE-22
    // ---------------------------------------------------------------------

    private static final class PathTraversalRule extends Base {

        private static final Set<String> FILE_TYPES = Set.of(
                "FileInputStream", "FileOutputStream", "FileReader", "FileWriter", "RandomAccessFile");
        private static final Set<String> FILE_IO_METHODS = Set.of(
                "readString", "readAllBytes", "readAllLines", "newInputStream", "newOutputStream",
                "newBufferedReader", "newBufferedWriter", "write", "writeString", "delete", "deleteIfExists",
                "copy", "move");
        private static final Pattern USER_PARAMETER = Pattern.compile(
                "^(?:(?i:user|input|upload|requested|filename|directory|resource)(?:$|[A-Z_].*))");
        private static final Pattern PARAMETER_SOURCE = Pattern.compile("parameter '([^']+)'");

        PathTraversalRule() {
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
        void scan(CompilationUnit cu, String fileName, List<Warning> out) {
            for (ObjectCreationExpr creation : cu.findAll(ObjectCreationExpr.class)) {
                if (FILE_TYPES.contains(creation.getType().getNameAsString())
                        && !creation.getArguments().isEmpty()) {
                    check(out, fileName, creation, new ArrayList<>(creation.getArguments()));
                }
            }
            for (MethodCallExpr call : cu.findAll(MethodCallExpr.class)) {
                boolean fileIo = scopeIs(call, "Files") && FILE_IO_METHODS.contains(call.getNameAsString());
                if (fileIo && !call.getArguments().isEmpty()) {
                    check(out, fileName, call, new ArrayList<>(call.getArguments()));
                }
            }
        }
    }

    // ---------------------------------------------------------------------
    // CWE-502
    // ---------------------------------------------------------------------

    private static final class UnsafeDeserializationRule extends Base {

        UnsafeDeserializationRule() {
            super(SecurityCatalog.UNSAFE_DESERIALIZATION);
        }

        @Override
        void scan(CompilationUnit cu, String fileName, List<Warning> out) {
            String text = cu.toString();
            boolean filtered = text.contains("ObjectInputFilter");
            for (ObjectCreationExpr creation : cu.findAll(ObjectCreationExpr.class)) {
                String type = creation.getType().getNameAsString();
                if (type.equals("ObjectInputStream") && !filtered) {
                    boolean restrictsClasses = creation.getAnonymousClassBody().isPresent()
                            && creation.getAnonymousClassBody().get().toString().contains("resolveClass");
                    if (!restrictsClasses) {
                        add(out, fileName, creation, Severity.CRITICAL,
                                "ObjectInputStream reads Java objects without a class filter. Untrusted bytes can run code.");
                    }
                } else if (type.equals("XMLDecoder")) {
                    add(out, fileName, creation, Severity.CRITICAL,
                            "XMLDecoder can instantiate any class named in the XML. Untrusted XML can run code.");
                }
            }
        }
    }

    // ---------------------------------------------------------------------
    // CWE-295
    // ---------------------------------------------------------------------

    private static final class CertificateCheckRule extends Base {

        private static final Set<String> TRUST_METHODS = Set.of(
                "checkServerTrusted", "checkClientTrusted", "checkTrusted");
        private static final Set<String> TRUST_ALL_NAMES = Set.of(
                "ALLOW_ALL_HOSTNAME_VERIFIER", "NoopHostnameVerifier", "TrustAllStrategy",
                "InsecureTrustManagerFactory");

        CertificateCheckRule() {
            super(SecurityCatalog.CERTIFICATE_CHECK);
        }

        private static boolean returnsOnlyTrue(Statement body) {
            if (body.isExpressionStmt()) {
                Expression expression = body.asExpressionStmt().getExpression();
                return expression.isBooleanLiteralExpr() && expression.asBooleanLiteralExpr().getValue();
            }
            List<ReturnStmt> returns = body.findAll(ReturnStmt.class);
            if (returns.isEmpty()) {
                return false;
            }
            for (ReturnStmt statement : returns) {
                if (statement.getExpression().isEmpty()) {
                    return false;
                }
                Expression value = statement.getExpression().get();
                if (!(value.isBooleanLiteralExpr() && value.asBooleanLiteralExpr().getValue())) {
                    return false;
                }
            }
            return true;
        }

        @Override
        void scan(CompilationUnit cu, String fileName, List<Warning> out) {
            for (MethodDeclaration method : cu.findAll(MethodDeclaration.class)) {
                if (TRUST_METHODS.contains(method.getNameAsString()) && method.getBody().isPresent()
                        && method.getBody().get().getStatements().isEmpty()) {
                    add(out, fileName, method, Severity.CRITICAL, method.getNameAsString()
                            + "() is empty, so this TrustManager accepts every certificate.");
                } else if (method.getNameAsString().equals("verify") && method.getParameters().size() == 2
                        && method.getParameter(1).getType().asString().endsWith("SSLSession")
                        && method.getBody().isPresent() && returnsOnlyTrue(method.getBody().get())) {
                    add(out, fileName, method, Severity.CRITICAL,
                            "This HostnameVerifier always returns true, so any host name is accepted.");
                }
            }
            for (MethodCallExpr call : cu.findAll(MethodCallExpr.class)) {
                boolean verifierSetter = call.getNameAsString().equals("setHostnameVerifier")
                        || call.getNameAsString().equals("setDefaultHostnameVerifier");
                if (verifierSetter && !call.getArguments().isEmpty() && call.getArgument(0).isLambdaExpr()) {
                    LambdaExpr lambda = call.getArgument(0).asLambdaExpr();
                    if (returnsOnlyTrue(lambda.getBody())) {
                        add(out, fileName, call, Severity.CRITICAL,
                                "This HostnameVerifier always returns true, so any host name is accepted.");
                    }
                }
            }
            for (NameExpr name : cu.findAll(NameExpr.class)) {
                if (TRUST_ALL_NAMES.contains(name.getNameAsString())) {
                    add(out, fileName, name, Severity.CRITICAL,
                            name.getNameAsString() + " turns certificate or host name checking off.");
                }
            }
            for (FieldAccessExpr field : cu.findAll(FieldAccessExpr.class)) {
                if (TRUST_ALL_NAMES.contains(field.getNameAsString())) {
                    add(out, fileName, field, Severity.CRITICAL,
                            field.getNameAsString() + " turns certificate or host name checking off.");
                }
            }
            for (ObjectCreationExpr creation : cu.findAll(ObjectCreationExpr.class)) {
                if (TRUST_ALL_NAMES.contains(creation.getType().getNameAsString())) {
                    add(out, fileName, creation, Severity.CRITICAL,
                            creation.getType().getNameAsString() + " turns certificate or host name checking off.");
                }
            }
        }
    }

    // ---------------------------------------------------------------------
    // CWE-209
    // ---------------------------------------------------------------------

    private static final class InformationLeakRule extends Base {

        private static final Set<String> RESPONSE_CALLS = Set.of("println", "print", "write", "sendError", "append");

        InformationLeakRule() {
            super(SecurityCatalog.INFORMATION_LEAK);
        }

        @Override
        void scan(CompilationUnit cu, String fileName, List<Warning> out) {
            for (MethodCallExpr call : cu.findAll(MethodCallExpr.class)) {
                if (call.getNameAsString().equals("printStackTrace") && call.getArguments().isEmpty()) {
                    add(out, fileName, call, Severity.WARNING,
                            "printStackTrace() writes the full stack trace to the console instead of a log.");
                    continue;
                }
                String scope = scopeText(call).toLowerCase(Locale.ROOT);
                boolean toUser = RESPONSE_CALLS.contains(call.getNameAsString())
                        && (scope.contains("getwriter") || scope.contains("response")
                                || scope.contains("resp") || scope.contains("getoutputstream"));
                if (!toUser) {
                    continue;
                }
                Optional<CatchClause> clause = call.findAncestor(CatchClause.class);
                if (clause.isEmpty()) {
                    continue;
                }
                String exception = clause.get().getParameter().getNameAsString();
                boolean leaked = false;
                for (Expression argument : call.getArguments()) {
                    for (NameExpr name : argument.findAll(NameExpr.class)) {
                        if (name.getNameAsString().equals(exception)) {
                            leaked = true;
                        }
                    }
                }
                if (leaked) {
                    add(out, fileName, call, Severity.WARNING,
                            "The exception '" + exception + "' is written to the response the user sees.");
                }
            }
        }
    }

    // ---------------------------------------------------------------------
    // CWE-611
    // ---------------------------------------------------------------------

    private static final class XxeRule extends Base {

        private static final Set<String> FACTORIES = Set.of(
                "DocumentBuilderFactory", "SAXParserFactory", "XMLInputFactory", "TransformerFactory",
                "SchemaFactory", "XMLReaderFactory");
        private static final Set<String> PARSER_BUILDERS = Set.of(
                "newDocumentBuilder", "newSAXParser", "createXMLStreamReader", "createXMLEventReader",
                "newTransformer", "createXMLReader");
        private static final List<String> HARDENING = List.of(
                "disallow-doctype-decl", "FEATURE_SECURE_PROCESSING", "IS_SUPPORTING_EXTERNAL_ENTITIES",
                "ACCESS_EXTERNAL_DTD", "SUPPORT_DTD", "external-general-entities",
                "http://apache.org/xml/features/disallow-doctype-decl",
                "http://xml.org/sax/features/external-general-entities");

        XxeRule() {
            super(SecurityCatalog.XXE);
        }

        @Override
        void scan(CompilationUnit cu, String fileName, List<Warning> out) {
            // One finding per method (or initializer), not one per call in the chain
            // DocumentBuilderFactory.newInstance().newDocumentBuilder() must not produce two rows.
            Set<String> reported = new HashSet<>();

            for (MethodDeclaration method : cu.findAll(MethodDeclaration.class)) {
                String body = method.toString();
                if (HARDENING.stream().anyMatch(body::contains)) {
                    continue;
                }
                MethodCallExpr hit = null;
                for (MethodCallExpr call : method.findAll(MethodCallExpr.class)) {
                    String name = call.getNameAsString();
                    if (PARSER_BUILDERS.contains(name) && (isFactoryChain(call) || isFactory(call))) {
                        hit = call; // prefer the builder call at the end of the chain
                        break;
                    }
                    if (hit == null && "newInstance".equals(name) && isFactory(call)) {
                        hit = call;
                    }
                }
                if (hit != null) {
                    String key = fileName + ":" + JavaAst.line(hit) + ":" + getName();
                    if (reported.add(key)) {
                        add(out, fileName, hit, Severity.CRITICAL,
                                "XML parser created with default settings (XXE / CWE-611). "
                                        + "Disable DOCTYPE and external entities before parse.");
                    }
                }
            }

            for (MethodCallExpr call : cu.findAll(MethodCallExpr.class)) {
                if (call.findAncestor(MethodDeclaration.class).isPresent()) {
                    continue;
                }
                String name = call.getNameAsString();
                boolean match = ("newInstance".equals(name) && isFactory(call))
                        || (PARSER_BUILDERS.contains(name) && (isFactoryChain(call) || isFactory(call)));
                if (!match) {
                    continue;
                }
                String key = fileName + ":" + JavaAst.line(call) + ":" + getName();
                if (reported.add(key)) {
                    add(out, fileName, call, Severity.CRITICAL,
                            "XML parser factory used with default settings (XXE / CWE-611).");
                }
            }
        }

        private static boolean isFactory(MethodCallExpr call) {
            if (FACTORIES.stream().anyMatch(factory -> scopeIs(call, factory))) {
                return true;
            }
            // DocumentBuilderFactory.newInstance() as TypeExpr scope
            return call.getScope().map(scope -> {
                String s = scope.toString();
                return FACTORIES.stream().anyMatch(f -> s.equals(f) || s.endsWith("." + f));
            }).orElse(false);
        }

        private static boolean isFactoryChain(MethodCallExpr call) {
            return call.getScope().map(scope -> {
                if (scope.isMethodCallExpr()) {
                    MethodCallExpr inner = scope.asMethodCallExpr();
                    return "newInstance".equals(inner.getNameAsString()) && isFactory(inner);
                }
                return FACTORIES.stream().anyMatch(f -> scope.toString().contains(f));
            }).orElse(false);
        }
    }
}
