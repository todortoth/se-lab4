package hu.bme.mit.spaceship;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.function.BiFunction;

/**
 * Minimal command line interface (CLI) to initialize and use spaceships.
 */
public class CommandLineInterface {

    private static Map<String, Handler> handlers = Map.of(
        "HELP", CommandLineInterface::handleHelp,
        "GT4500", CommandLineInterface::handleGT4500,
        "TORPEDO", CommandLineInterface::handleTorpedo,
        "EXIT", CommandLineInterface::handleExit
    );

    public static void main(String[] args) {
        run(System.in, System.out, new OptionalOutput(System.err));
    }

    /**
     * Read and handle commands from an input stream, writing output to another stream.
     * 
     * The optional err stream receives output only useful in an interactive session.
     * 
     * @param in The input stream to read commands from
     * @param out The output stream to write results to
     * @param err Optional stream to write interactive session feedback to
     */
    public static void run(InputStream in, OutputStream out, OptionalOutput err) {
        Context ctx = new Context();
        ctx.out = new PrintStream(out);

        err.println("Welcome to the console interface.  Available commands: "
                + handlers.keySet().toString());
        try (Scanner scanner = new Scanner(in)) {
            CommandResult result = CommandResult.CONTINUE;
            do {
                err.print("> ");
                try {
                    result = handle(ctx, scanner.nextLine());
                } catch (NoSuchElementException e) {
                    result = CommandResult.EXIT;
                }
            } while (result == CommandResult.CONTINUE);
        }
    }

    public static void run(InputStream in, OutputStream out) {
        run(in, out, new OptionalOutput(null));
    }

    /**
     * Handle a command.
     * 
     * @param ctx The current CLI context
     * @param command The command as a list of comma-separated tokens
     * @return whether execution should continue
     */
    private static CommandResult handle(Context ctx, String command) {
        if (command.stripLeading().startsWith("#")) {
            return CommandResult.CONTINUE;
        }

        command = command.replaceAll("#.*$", "").strip();

        String[] parts = command.split(",");
        String mainCommand = parts[0].toUpperCase();

        CommandResult result = CommandResult.CONTINUE;
        try {
            Handler handler = handlers.get(mainCommand);
            if (handler == null) {
                ctx.out.printf("Unknown command: '%s'%n", mainCommand);
            } else {
                result = handler.apply(ctx, parts);
            }
        } catch (IllegalArgumentException e) {
            ctx.out.println(e.getLocalizedMessage());
        }

        return result;
    }

    /**
     * Handle the HELP command.
     */
    private static CommandResult handleHelp(Context ctx, String[] params) {
        ctx.out.println("Available commands: " + handlers.keySet());
        ctx.out.println("Generally, commands receive parameters; refer to the documentation");
        ctx.out.println(
                "Before firing torpedoes using the TORPEDO command, you must initialize a ship (eg. a GT4500) using its name as a command");
        return CommandResult.CONTINUE;
    }

    /**
     * Handle the GT4500 command.
     */
    private static CommandResult handleGT4500(Context ctx, String[] params) {
        if (params.length != 5) {
            throw new IllegalArgumentException(
                    "usage: GT4500,<PRI_CNT>,<PRI_FAIL_RATE>,<SEC_CNT>,<SEC_FAIL_RATE>");
        }

        int primaryCount;
        int secondaryCount;
        double primaryFailRate;
        double secondaryFailRate;
        try {
            primaryCount = Integer.parseInt(params[1]);
            primaryFailRate = Double.parseDouble(params[2]);
            secondaryCount = Integer.parseInt(params[3]);
            secondaryFailRate = Double.parseDouble(params[4]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Invalid numerical arguments passed: " + e.getLocalizedMessage(), e);
        }

        ctx.ship = new GT4500(primaryCount, primaryFailRate, secondaryCount, secondaryFailRate);
        ctx.out.println("SUCCESS");
        return CommandResult.CONTINUE;
    }

    /**
     * Handle the TORPEDO command.
     */
    private static CommandResult handleTorpedo(Context ctx, String[] params) {
        if (ctx.ship == null) {
            throw new IllegalArgumentException("No ship has been initialized");
        }
        if (params.length != 2) {
            throw new IllegalArgumentException("usage: TORPEDO,<SINGLE|ALL>");
        }

        FiringMode firingMode;
        try {
            firingMode = FiringMode.valueOf(params[1].toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    String.format("Unknown firing mode: '%s'", params[1].toUpperCase()), e);
        }
        boolean success = ctx.ship.fireTorpedo(firingMode);
        ctx.out.println(success ? "SUCCESS" : "FAIL");
        return CommandResult.CONTINUE;
    }

    /**
     * Handle the EXIT command.
     */
    private static CommandResult handleExit(Context ctx, String[] params) {
        return CommandResult.EXIT;
    }

    private static class Context {
        SpaceShip ship;
        PrintStream out;
    }

    private static interface Handler extends BiFunction<Context, String[], CommandResult> {}

    /**
     * Rudimentary PrintStream-like interface that silently ignores if the underlying PrintStream is
     * null.
     */
    private static class OptionalOutput {

        private PrintStream out;

        OptionalOutput(OutputStream out) {
            if (out != null) {
                this.out = new PrintStream(out);
            }
        }

        public void print(String message) {
            if (out != null) {
                out.print(message);
            }
        }

        public void println(String message) {
            print(message + "\n");
        }
    }

    /**
     * More readable enumeration for command results.
     */
    private enum CommandResult {
        CONTINUE, EXIT
    }
}
