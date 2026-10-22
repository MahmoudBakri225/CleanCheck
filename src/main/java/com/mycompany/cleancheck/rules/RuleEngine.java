package com.mycompany.cleancheck.rules;

import com.mycompany.cleancheck.analysis.JavaAst;
import com.mycompany.cleancheck.analysis.Severity;
import com.mycompany.cleancheck.analysis.Warning;
import com.mycompany.cleancheck.config.CleanCheckConfig;
import com.mycompany.cleancheck.config.RuleConfig;
import com.mycompany.cleancheck.security.SecurityRules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class RuleEngine {

    /** Reported when a file cannot be parsed, so a broken file never looks clean. */
    public static final String PARSE_ERROR_RULE = "Parse Error";

    private final CleanCheckConfig config;
    private final List<Rule> rules;
    private final List<ProjectRule> projectRules;

    public RuleEngine() {
        this(RuleConfig.load());
    }

    /** @param disabledRuleNames lower-case rule names to skip (see cleancheck.properties). */
    public RuleEngine(Set<String> disabledRuleNames) {
        this(CleanCheckConfig.withDisabled(disabledRuleNames));
    }

    public RuleEngine(CleanCheckConfig config) {
        this.config = config;
        List<Rule> allRules = new ArrayList<>(List.of(
                new EncapsulationRule(),
                new EmptyMethodRule(),
                new EmptyIfRule(),
                new EmptyWhileRule(),
                new InfiniteLoopRule(),
                new TooManyParametersRule(config.threshold(CleanCheckConfig.METHOD_MAX_PARAMETERS)),
                new NameConventionRule(),
                new DataTypeRule(),
                new EmptyCatchRule(),
                new MagicNumberRule(),
                new LongMethodRule(config.threshold(CleanCheckConfig.METHOD_MAX_LINES)),
                new SystemOutRule(),
                new MissingPackageRule(),
                new UnusedImportRule(),
                new UnusedVariableRule(),
                new CyclomaticComplexityRule(config.threshold(CleanCheckConfig.METHOD_MAX_COMPLEXITY)),
                new DeepNestingRule(config.threshold(CleanCheckConfig.NESTING_MAX_DEPTH)),
                new StringComparisonRule(),
                new GenericCatchRule(),
                new UnclosedResourceRule(),
                new EqualsHashCodeRule(),
                new ClassLengthRule(config.threshold(CleanCheckConfig.CLASS_MAX_LINES)),
                new SolidSrpRule(config.threshold(CleanCheckConfig.SOLID_SRP_MAX_PUBLIC_METHODS)),
                new SolidOcpRule(config.threshold(CleanCheckConfig.SOLID_OCP_MAX_SWITCH_ENTRIES)),
                new SolidLspRule(),
                new SolidIspRule(config.threshold(CleanCheckConfig.SOLID_ISP_MAX_METHODS)),
                new SolidDipRule()
        ));
        allRules.addAll(SecurityRules.all());
        this.rules = allRules.stream()
                .filter(rule -> isEnabled(rule.getName()))
                .toList();
        this.projectRules = List.<ProjectRule>of(
                new DuplicateCodeRule(config.threshold(CleanCheckConfig.DUPLICATE_MIN_STATEMENTS))
        ).stream()
                .filter(rule -> isEnabled(rule.getName()))
                .toList();
    }

    private boolean isEnabled(String ruleName) {
        return !config.disabled().contains(ruleName.toLowerCase(Locale.ROOT));
    }

    public CleanCheckConfig config() {
        return config;
    }

    /** The per-file rules (project-wide rules such as Duplicate Code are listed by {@link #getProjectRules()}). */
    public List<Rule> getRules() {
        return rules;
    }

    public List<ProjectRule> getProjectRules() {
        return projectRules;
    }

    public List<Warning> analyze(String fileName, String source) {
        JavaAst.beginFile(source);
        try {
            List<Warning> found = new ArrayList<>();

            if (isEnabled(PARSE_ERROR_RULE)) {
                JavaAst.parseError(source).ifPresent(error -> {
                    int bar = error.indexOf('|');
                    int line = Integer.parseInt(error.substring(0, bar));
                    found.add(new Warning(
                            fileName,
                            line,
                            PARSE_ERROR_RULE,
                            Severity.CRITICAL,
                            "The file could not be analyzed: " + error.substring(bar + 1),
                            "Fix the syntax error. The other rules cannot check this file until it compiles."
                    ));
                });
            }
            for (Rule rule : rules) {
                found.addAll(rule.analyze(fileName, source));
            }
            return finish(found, Map.of(fileName, source));
        } finally {
            JavaAst.endFile();
        }
    }

    /** Runs the rules that compare files with each other. */
    public List<Warning> analyzeProject(Map<String, String> sourcesByFile) {
        List<Warning> found = new ArrayList<>();
        for (ProjectRule rule : projectRules) {
            found.addAll(rule.analyze(sourcesByFile));
        }
        return finish(found, sourcesByFile);
    }

    /** Applies "// cleancheck:ignore" comments and severity overrides from the configuration. */
    private List<Warning> finish(List<Warning> found, Map<String, String> sources) {
        Map<String, IgnoreDirectives> directives = new HashMap<>();
        List<Warning> result = new ArrayList<>();
        for (Warning warning : found) {
            IgnoreDirectives ignore = directives.computeIfAbsent(
                    warning.getFileName(),
                    file -> IgnoreDirectives.parse(sources.get(file)));
            if (ignore.suppresses(warning.getLineNumber(), warning.getRuleName())) {
                continue;
            }
            Severity severity = config.severityFor(warning.getRuleName(), warning.getSeverity());
            result.add(severity == warning.getSeverity()
                    ? warning
                    : new Warning(warning.getFileName(), warning.getLineNumber(), warning.getRuleName(),
                            severity, warning.getMessage(), warning.getSuggestion()));
        }
        return List.copyOf(result);
    }
}
