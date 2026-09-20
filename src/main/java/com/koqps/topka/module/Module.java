package com.koqps.topka.module;

public final class Module {
    public enum Category { COMBAT, VISUAL, HUD, MOVEMENT }

    private final String id;
    private final String name;
    private final String description;
    private final String icon;
    private final Category category;
    private boolean enabled;

    public Module(String id, String name, String description, String icon, Category category, boolean enabled) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.icon = icon;
        this.category = category;
        this.enabled = enabled;
    }

    public String id() { return id; }
    public String name() { return name; }
    public String description() { return description; }
    public String icon() { return icon; }
    public Category category() { return category; }
    public boolean enabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public void toggle() { enabled = !enabled; }
}
