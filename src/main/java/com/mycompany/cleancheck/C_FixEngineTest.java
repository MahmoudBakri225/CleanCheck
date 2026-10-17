package com.mycompany.cleancheck;

import java.nio.file.Path;
import java.util.List;

public class C_FixEngineTest {

    public static void main(String[] args) {

        C_FixEngine fixEngine =
                new C_FixEngine();

        Path testFile =
                Path.of(
                        "fix-test",
                        "FixTest.java"
                );

        try {

            System.out.println();

            System.out.println(
                    "============================================================"
            );

            System.out.println(
                    "                    FIX ENGINE TEST"
            );

            System.out.println(
                    "============================================================"
            );

            System.out.println(
                    "File: "
                            + testFile.toAbsolutePath()
            );

            System.out.println(
                    "------------------------------------------------------------"
            );

            List<C_FixResult> results =
                    fixEngine.applyFixes(
                            testFile
                    );

            System.out.println(
                    "Fixes Applied : "
                            + results.size()
            );

            System.out.println(
                    "------------------------------------------------------------"
            );

            for (C_FixResult result :
                    results) {

                System.out.println(
                        "File  : "
                                + result.getFileName()
                );

                System.out.println(
                        "Rule  : "
                                + result.getRuleName()
                );

                System.out.println(
                        "Fixed : "
                                + (result.isFixed()
                                ? "YES"
                                : "NO")
                );

                System.out.println(
                        "------------------------------------------------------------"
                );
            }

            if (results.isEmpty()) {

                System.out.println(
                        "No automatic fixes were applied."
                );
            }

            System.out.println(
                    "============================================================"
            );

        } catch (Exception e) {

            System.out.println(
                    "Fix Engine test failed!"
            );

            e.printStackTrace();
        }
    }
}