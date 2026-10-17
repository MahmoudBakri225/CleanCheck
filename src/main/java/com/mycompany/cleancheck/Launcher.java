package com.mycompany.cleancheck;

import com.mycompany.cleancheck.application.CiRunner;
import com.mycompany.cleancheck.application.CleanCheck;
import com.mycompany.cleancheck.ui.CleanCheckGUI;

import java.util.Arrays;

/**
 * Main class of the runnable jar. It is deliberately not a JavaFX Application, which is what lets
 * a plain "java -jar cleancheck.jar" start JavaFX from the classpath.
 *
 * <pre>
 * java -jar cleancheck.jar                      open the GUI
 * java -jar cleancheck.jar gui                  open the GUI
 * java -jar cleancheck.jar ci   &lt;project&gt; ...   CI mode: exit 0 passed, 1 gate failed, 2 error
 * java -jar cleancheck.jar scan &lt;project&gt;       read-only analysis (saved to the local history)
 * </pre>
 */
public final class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) {
        String mode = args.length == 0 ? "gui" : args[0].toLowerCase();
        String[] rest = args.length == 0 ? args : Arrays.copyOfRange(args, 1, args.length);
        switch (mode) {
            case "gui" -> CleanCheckGUI.main(rest);
            case "ci" -> CiRunner.main(rest);
            case "scan" -> {
                String[] withFlag = new String[rest.length + 1];
                System.arraycopy(rest, 0, withFlag, 0, rest.length);
                withFlag[rest.length] = "--analyze-only";
                CleanCheck.main(rest.length == 0 ? rest : withFlag);
            }
            default -> {
                System.err.println("Unknown mode '" + args[0] + "'. Use: gui | ci <project> [options] | scan <project>");
                System.exit(2);
            }
        }
    }
}
