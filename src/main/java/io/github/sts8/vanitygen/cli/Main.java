package io.github.sts8.vanitygen.cli;

import io.github.sts8.vanitygen.bruteforce.BruteForceEngine;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.nio.file.Path;
import java.security.Security;

/**
 * The main entry point for the Syncthing Vanity ID Generator.
 * <p>
 * This class handles:
 * <ul>
 * <li>Registration of the Bouncy Castle security provider.</li>
 * <li>Command-line argument parsing for prefix and thread count.</li>
 * <li>Initialization and execution of the {@link BruteForceEngine}.</li>
 * </ul>
 */
public class Main {

    /**
     * Application entry point.
     * * @param args Command-line arguments.
     * Supported: {@code --prefix <STRING>} (required) and {@code --threads <INT>} (optional).
     */
    static void main(String[] args) {

        // Required to support Ed25519 and advanced X.509 certificate generation
        Security.addProvider(new BouncyCastleProvider());

        String prefix = null;
        int threads = Runtime.getRuntime().availableProcessors();

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {

                case "--prefix" -> {
                    if (++i >= args.length) die("Missing value for --prefix");
                    prefix = args[i].toUpperCase();
                }

                case "--threads" -> {
                    if (++i >= args.length) die("Missing value for --threads");
                    try {
                        threads = Integer.parseInt(args[i]);
                        if (threads <= 0) throw new NumberFormatException();
                    } catch (NumberFormatException e) {
                        die("Invalid thread count: " + args[i]);
                    }
                }

                default -> die("Unknown argument: " + args[i]);
            }
        }

        if (prefix == null || prefix.isEmpty()) {
            die("--prefix is required");
        }

        System.out.println("Prefix : " + prefix);
        System.out.println("Threads: " + threads);

        Path output = Path.of("output");

        BruteForceEngine engine =
                new BruteForceEngine(prefix, threads, output);

        engine.run();
    }

    /**
     * Prints an error message and usage instructions to stderr, then terminates the JVM.
     * * @param msg The error message to display.
     */
    private static void die(String msg) {
        System.err.println("Error: " + msg);
        System.err.println("Usage: --prefix <STRING> [--threads <INT>]");
        System.exit(1);
    }
}
