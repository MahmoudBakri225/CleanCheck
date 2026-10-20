package com.mycompany.cleancheck.ui;

import javafx.scene.Node;
import javafx.scene.layout.StackPane;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Holds page nodes and switches the visible page inside a StackPane content area.
 * Extracted from the main GUI to keep navigation logic small and reusable.
 */
public final class PageNavigator {

    private final StackPane content;
    private final Map<String, Node> pages = new LinkedHashMap<>();
    private String current = PageId.HOME.label();
    private Consumer<String> onNavigated;

    public PageNavigator(StackPane content) {
        this.content = Objects.requireNonNull(content, "content");
    }

    public void register(String name, Node page) {
        pages.put(name, page);
    }

    public void register(PageId id, Node page) {
        register(id.label(), page);
    }

    public void setOnNavigated(Consumer<String> onNavigated) {
        this.onNavigated = onNavigated;
    }

    public void show(String name) {
        Node page = pages.get(name);
        if (page == null) {
            page = pages.get(PageId.HOME.label());
            name = PageId.HOME.label();
        }
        content.getChildren().setAll(page);
        current = name;
        if (onNavigated != null) {
            onNavigated.accept(name);
        }
    }

    public void show(PageId id) {
        show(id.label());
    }

    public String current() {
        return current;
    }

    public boolean has(String name) {
        return pages.containsKey(name);
    }
}
