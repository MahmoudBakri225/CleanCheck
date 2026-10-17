package com.mycompany.cleancheck.ui;

import java.nio.file.Path;

/** Defines when a saved automatic-fix backup may be restored from the UI. */
final class FixRollbackPolicy {

    private FixRollbackPolicy() {
    }

    static boolean canRollback(Path selectedProject, Path fixProject, Path sourceFile, Path backup,
            boolean operationRunning) {
        if (operationRunning || selectedProject == null || fixProject == null
                || sourceFile == null || backup == null) {
            return false;
        }
        return selectedProject.toAbsolutePath().normalize()
                .equals(fixProject.toAbsolutePath().normalize());
    }
}
