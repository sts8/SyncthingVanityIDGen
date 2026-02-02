package io.github.sts8.vanitygen.cli;

import io.github.sts8.vanitygen.bruteforce.BruteForceEngine;
import io.github.sts8.vanitygen.bruteforce.PrefixTrie;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Security;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

/**
 * The main entry point for the Syncthing Vanity ID Generator.
 * <p>
 * This class handles the registration of the security provider, CLI argument parsing,
 * and loading prefixes from both direct input and multiple external files into a {@link PrefixTrie}.
 */
public class Main {

    /**
     * Application entry point.
     *
     * @param args Command-line arguments. Supported:
     *             {@code --prefix <STR>} (multiple), {@code --prefix-list <PATH>} (multiple), {@code --threads <INT>}.
     */
    static void main(String[] args) {

        // Required to support Ed25519 and advanced X.509 certificate generation
        Security.addProvider(new BouncyCastleProvider());

        Set<String> prefixSet = new HashSet<>();
        Set<Path> listPaths = new HashSet<>();
        int threads = Runtime.getRuntime().availableProcessors();

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {

                case "--prefix" -> {
                    if (++i >= args.length) die("Missing value for --prefix");
                    prefixSet.add(args[i].toUpperCase());
                }
                case "--prefix-list" -> {
                    if (++i >= args.length) die("Missing value for --prefix-list");
                    listPaths.add(Path.of(args[i]));
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

        // Load prefixes from all provided files
        for (Path path : listPaths) {
            loadPrefixesFromFile(path, prefixSet);
        }

        if (prefixSet.isEmpty()) {
            die("No prefixes provided. Use --prefix or --prefix-list.");
        }

        // Initialize the high-performance Trie
        PrefixTrie trie = new PrefixTrie();
        prefixSet.forEach(trie::insert);

        System.out.println("Loaded " + prefixSet.size() + " unique prefixes.");
        System.out.println("Threads: " + threads);

        Path output = Path.of("output");
        BruteForceEngine engine = new BruteForceEngine(trie, threads, output);
        engine.run();
    }

    /**
     * Reads prefixes from a file, normalizes them, and adds them to the provided set.
     *
     * @param path      The path to the text file.
     * @param prefixSet The set to populate with unique prefixes.
     */
    private static void loadPrefixesFromFile(Path path, Set<String> prefixSet) {
        try (Stream<String> lines = Files.lines(path)) {
            lines.map(String::trim)
                    .filter(s -> !s.isBlank())
                    .map(String::toUpperCase)
                    .forEach(prefixSet::add);
        } catch (IOException e) {
            die("Failed to read prefix list '" + path + "': " + e.getMessage());
        }
    }

    /**
     * Prints an error message and usage instructions to stderr, then terminates the JVM.
     *
     * @param msg The error message to display.
     */
    private static void die(String msg) {
        System.err.println("Error: " + msg);
        System.err.println("Usage: [--prefix <STRING>]... [--prefix-list <FILE>]... [--threads <INT>]");
        System.err.println("Note: At least one prefix source is required.");
        System.exit(1);
    }
}
