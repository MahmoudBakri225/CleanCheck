package com.mycompany.cleancheck.security;

import com.mycompany.cleancheck.rules.Rule;

import java.util.List;

/**
 * Factory for all preliminary security rules.
 * Each rule lives in its own class for maintainability.
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
}
