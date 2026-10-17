package com.mycompany.cleancheck.fix;

public class FixResult {

    private final String fileName;
    private final String ruleName;
    private final boolean fixed;

    public FixResult(String fileName, String ruleName, boolean fixed) {
        this.fileName = fileName;
        this.ruleName = ruleName;
        this.fixed = fixed;
    }

    public String getFileName() {
        return fileName;
    }

    public String getRuleName() {
        return ruleName;
    }

    public boolean isFixed() {
        return fixed;
    }
}
