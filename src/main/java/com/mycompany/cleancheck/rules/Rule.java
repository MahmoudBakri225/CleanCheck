package com.mycompany.cleancheck.rules;

import com.mycompany.cleancheck.analysis.Warning;

import java.util.List;

public interface Rule {

    String getName();

    List<Warning> analyze(String fileName, String source);
}
