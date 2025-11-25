package org.bdj;

import java.io.PrintStream;

public class Logger {
    public static void log(PrintStream console, String message) {
        if (console != null) {
            console.println(message);
        }
    }
}