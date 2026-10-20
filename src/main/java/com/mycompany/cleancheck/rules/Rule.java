package com.mycompany.cleancheck.rules;

import com.mycompany.cleancheck.analysis.Warning;

import java.util.List;

public interface Rule extends AnalysisRule {

    String getName();

    List<Warning> analyze(String fileName, String source);
}
