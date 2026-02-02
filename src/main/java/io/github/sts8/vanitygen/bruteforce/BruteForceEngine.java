package io.github.sts8.vanitygen.bruteforce;

import io.github.sts8.vanitygen.crypto.SyncthingKeyPairGenerator;
import io.github.sts8.vanitygen.model.SyncthingIdentity;

import java.nio.file.Path;
import java.security.KeyPair;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Orchestrates the multithreaded brute-force search for Syncthing Device IDs
 * that match a specific vanity prefix.
 * <p>
 * The engine manages a pool of worker threads and a scheduled reporter thread
 * for performance monitoring. It also registers a shutdown hook to ensure
 * resources are released gracefully when the application is closed.
 */
public final class BruteForceEngine {

    private final String prefix;
    private final int threads;
    private final Path outputDir;

    /**
     * Shared counter for tracking total key generation attempts across all threads.
     */
    private final AtomicLong totalIterations = new AtomicLong(0);

    /**
     * Constructs the engine with search parameters.
     *
     * @param prefix    The desired starting characters of the Syncthing ID.
     * @param threads   The number of concurrent worker threads to spawn.
     * @param outputDir The filesystem path where matching identities will be saved.
     */
    public BruteForceEngine(String prefix, int threads, Path outputDir) {
        this.prefix = prefix;
        this.threads = threads;
        this.outputDir = outputDir;
    }

    /**
     * Initializes the thread pools, schedules the rate reporter, and starts the workers.
     * <p>
     * This method also adds a {@link Runtime#addShutdownHook(Thread)} to handle
     * {@code Ctrl+C} or termination signals by shutting down the executors.
     */
    public void run() {

        ExecutorService workers = Executors.newFixedThreadPool(threads);
        ScheduledExecutorService reporter =
                Executors.newSingleThreadScheduledExecutor();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nShutting down...");
            workers.shutdownNow();
            reporter.shutdownNow();
        }));

        // Reports generation speed every 3 seconds
        reporter.scheduleAtFixedRate(
                new RateReporter(totalIterations), 3, 3, TimeUnit.SECONDS);

        for (int i = 0; i < threads; i++) {
            workers.submit(this::workerLoop);
        }
    }

    /**
     * The core logic for an individual worker thread.
     * <p>
     * Continuously generates new key pairs, derives the Syncthing ID, and
     * checks if it matches the target prefix. If a match is found, the
     * identity is written to the output directory.
     */
    private void workerLoop() {
        try {
            SyncthingKeyPairGenerator generator = new SyncthingKeyPairGenerator();

            while (!Thread.currentThread().isInterrupted()) {
                KeyPair keyPair = generator.generate();
                SyncthingIdentity identity = new SyncthingIdentity(keyPair);

                totalIterations.incrementAndGet();

                if (identity.getSyncthingID().startsWith(prefix)) {
                    System.out.println(identity.getSyncthingID());
                    identity.writeToDirectory(outputDir);
                }
            }

        } catch (Exception e) {
            // Log the error to stderr to avoid interrupting other threads
            e.printStackTrace();
        }
    }

}
