package com.mycompany.cleancheck.security;

import com.mycompany.cleancheck.analysis.Warning;
import com.mycompany.cleancheck.rules.Rule;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityRulesSmokeTest {

    @Test
    void detectsSqlInjectionAndHardcodedSecret() {
        String source = """
                public class Bad {
                    private static final String API_KEY = "sk-live-demo-not-real-000";
                    public void q(java.sql.Connection c, String name) throws Exception {
                        c.createStatement().executeQuery("SELECT * FROM t WHERE n = '" + name + "'");
                    }
                }
                """;
        List<Warning> all = new ArrayList<>();
        for (Rule r : SecurityRules.all()) {
            all.addAll(r.analyze("Bad.java", source));
        }
        assertTrue(all.stream().anyMatch(w -> w.getRuleName().toLowerCase().contains("sql")));
        assertTrue(all.stream().anyMatch(w -> w.getRuleName().toLowerCase().contains("secret")
                || w.getRuleName().toLowerCase().contains("hardcoded")));
    }
}

