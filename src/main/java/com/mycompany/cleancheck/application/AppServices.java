package com.mycompany.cleancheck.application;

import com.mycompany.cleancheck.rules.RuleEngine;
import com.mycompany.cleancheck.scanner.JavaFileScanner;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Lightweight service registry (simple DI without a framework).
 * Stateless / config-sensitive services are created fresh; others may be cached.
 */
public final class AppServices {

    private static final AppServices INSTANCE = new AppServices();

    /** Types that must not be cached as singletons (project-specific config). */
    private static final Set<Class<?>> PROTOTYPE = Set.of(ProjectAnalyzer.class, RuleEngine.class);

    private final ConcurrentHashMap<Class<?>, Object> instances = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Class<?>, Supplier<?>> factories = new ConcurrentHashMap<>();

    private AppServices() {
        registerFactory(JavaFileScanner.class, JavaFileScanner::new);
        registerFactory(SourceDiscovery.class, () -> new SourceDiscovery(get(JavaFileScanner.class)));
        registerFactory(RuleEngine.class, RuleEngine::new);
        // ProjectAnalyzer loads cleancheck.properties for the selected project root.
        registerFactory(ProjectAnalyzer.class, ProjectAnalyzer::new);
    }

    public static AppServices getInstance() {
        return INSTANCE;
    }

    public <T> void register(Class<T> type, T instance) {
        instances.put(Objects.requireNonNull(type), Objects.requireNonNull(instance));
    }

    public <T> void registerFactory(Class<T> type, Supplier<T> factory) {
        factories.put(Objects.requireNonNull(type), Objects.requireNonNull(factory));
    }

    @SuppressWarnings("unchecked")
    public <T> T get(Class<T> type) {
        if (!PROTOTYPE.contains(type)) {
            Object existing = instances.get(type);
            if (existing != null) {
                return (T) existing;
            }
        }
        Supplier<?> factory = factories.get(type);
        if (factory == null) {
            throw new IllegalStateException("No service registered for " + type.getName());
        }
        T created = (T) factory.get();
        if (!PROTOTYPE.contains(type)) {
            instances.put(type, created);
        }
        return created;
    }

    public void resetInstances() {
        instances.clear();
    }
}
