package com.mycompany.cleancheck;

public class C_FixResult {

    private final String fileName;
    private final String ruleName;
    private final boolean fixed;

    public C_FixResult(
            String fileName,
            String ruleName,
            boolean fixed
    ) {

        this.fileName =
                fileName;

        this.ruleName =
                ruleName;

        this.fixed =
                fixed;
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