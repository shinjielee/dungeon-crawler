package game;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Simple thread-safe logger: prints to stdout AND writes to dungeon.log
 */
public class GameLogger {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private static PrintWriter fileWriter;

    static {
        try {
            fileWriter = new PrintWriter(new OutputStreamWriter(
                    new FileOutputStream("dungeon.log", false), StandardCharsets.UTF_8), true);
        } catch (IOException e) {
            System.err.println("[Logger] Cannot open log file: " + e.getMessage());
        }
    }

    public static void info(String msg) {
        log("INFO ", msg);
    }

    public static void warn(String msg) {
        log("WARN ", msg);
    }

    public static void error(String msg, Throwable t) {
        log("ERROR", msg);
        if (t != null) {
            String trace = stackTraceToString(t);
            log("ERROR", trace);
        }
    }

    public static void combat(String msg) {
        log("COMBAT", msg);
    }

    public static void shop(String msg) {
        log("SHOP ", msg);
    }

    public static void phase(String msg) {
        log("PHASE", msg);
    }

    private static synchronized void log(String level, String msg) {
        String line = "[" + LocalTime.now().format(FMT) + "][" + level + "] " + msg;
        System.out.println(line);
        if (fileWriter != null)
            fileWriter.println(line);
    }

    private static String stackTraceToString(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}
