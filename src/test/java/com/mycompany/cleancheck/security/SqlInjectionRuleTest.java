package com.mycompany.cleancheck.security;

import com.mycompany.cleancheck.analysis.Warning;
import com.mycompany.cleancheck.rules.Rule;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqlInjectionRuleTest {

    @Test
    void detectsConcatenatedQueryWithParameter() {
        String source = """
                import java.sql.Connection;
                import java.sql.ResultSet;
                import java.sql.Statement;
                public class UserService {
                    public ResultSet findUser(Connection con, String name) throws Exception {
                        Statement st = con.createStatement();
                        return st.executeQuery("SELECT * FROM users WHERE name = '" + name + "'");
                    }
                }
                """;
        Rule rule = SecurityRules.all().stream()
                .filter(r -> r.getName().toLowerCase().contains("sql"))
                .findFirst()
                .orElseThrow();
        List<Warning> found = rule.analyze("UserService.java", source);
        assertFalse(found.isEmpty(), "Expected SQL Injection for string-built executeQuery");
    }

    @Test
    void detectsUserInputAppendedToAStringBuilderQuery() {
        String source = """
                import java.sql.Connection;
                import java.sql.ResultSet;
                import java.sql.Statement;
                public class UserService {
                    public ResultSet findUser(Connection con, String name) throws Exception {
                        StringBuilder query = new StringBuilder("SELECT * FROM users WHERE name = '");
                        query.append(name).append("'");
                        Statement st = con.createStatement();
                        return st.executeQuery(query.toString());
                    }
                }
                """;

        List<Warning> found = sqlRule().analyze("UserService.java", source);

        assertTrue(found.stream().anyMatch(w -> w.getRuleName().contains("SQL Injection")),
                "Expected SQL Injection for user input appended to a StringBuilder query");
    }

    @Test
    void doesNotFlagAStringBuilderQueryMadeOnlyFromLiterals() {
        String source = """
                import java.sql.Connection;
                import java.sql.ResultSet;
                import java.sql.Statement;
                public class UserService {
                    public ResultSet findUser(Connection con) throws Exception {
                        StringBuilder query = new StringBuilder("SELECT * ");
                        query.append("FROM users");
                        Statement st = con.createStatement();
                        return st.executeQuery(query.toString());
                    }
                }
                """;

        List<Warning> found = sqlRule().analyze("UserService.java", source);

        assertFalse(found.stream().anyMatch(w -> w.getRuleName().contains("SQL Injection")),
                "A literal-only query builder should not trigger SQL Injection");
    }

    @Test
    void detectsGenericExecuteOnAStatementButIgnoresUnrelatedExecuteMethods() {
        String sqlSource = """
                import java.sql.Statement;
                class QueryService {
                    void run(Statement statement, String userInput) throws Exception {
                        statement.execute("SELECT * FROM users WHERE name='" + userInput + "'");
                    }
                }
                """;
        String unrelatedSource = """
                class Task {
                    void execute(String[] arguments) { }
                }
                class Application {
                    void run(String[] arguments) {
                        new Task().execute(arguments);
                    }
                }
                """;

        List<Warning> sqlFindings = sqlRule().analyze("QueryService.java", sqlSource);
        List<Warning> unrelatedFindings = sqlRule().analyze("Application.java", unrelatedSource);

        assertTrue(sqlFindings.stream().anyMatch(w -> w.getRuleName().contains("SQL Injection")));
        assertFalse(unrelatedFindings.stream().anyMatch(w -> w.getRuleName().contains("SQL Injection")));
    }

    private Rule sqlRule() {
        return SecurityRules.all().stream()
                .filter(rule -> rule.getName().toLowerCase().contains("sql"))
                .findFirst()
                .orElseThrow();
    }
}

