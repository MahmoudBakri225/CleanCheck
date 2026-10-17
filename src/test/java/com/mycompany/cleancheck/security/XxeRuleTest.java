package com.mycompany.cleancheck.security;

import com.mycompany.cleancheck.analysis.Warning;
import com.mycompany.cleancheck.rules.Rule;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XxeRuleTest {

    @Test
    void detectsDefaultDocumentBuilderFactory() {
        String source = """
                import javax.xml.parsers.DocumentBuilderFactory;
                public class X {
                    public Object createParser() throws Exception {
                        return DocumentBuilderFactory.newInstance().newDocumentBuilder();
                    }
                }
                """;
        Rule rule = SecurityRules.all().stream()
                .filter(r -> r.getName().toLowerCase().contains("xml"))
                .findFirst()
                .orElseThrow();
        List<Warning> found = rule.analyze("X.java", source);
        assertFalse(found.isEmpty(), "Expected XXE finding");
    }

    @Test
    void skipsHardenedFactory() {
        String source = """
                import javax.xml.parsers.DocumentBuilderFactory;
                import javax.xml.XMLConstants;
                public class X {
                    public Object createParser() throws Exception {
                        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
                        f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
                        f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
                        return f.newDocumentBuilder();
                    }
                }
                """;
        Rule rule = SecurityRules.all().stream()
                .filter(r -> r.getName().toLowerCase().contains("xml"))
                .findFirst()
                .orElseThrow();
        assertTrue(rule.analyze("X.java", source).isEmpty());
    }
}

