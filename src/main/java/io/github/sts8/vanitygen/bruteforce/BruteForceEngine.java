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

public final class BruteForceEngine {

    private final String prefix;
    private final int threads;
    private final Path outputDir;

    private final AtomicLong totalIterations = new AtomicLong(0);

    public BruteForceEngine(String prefix, int threads, Path outputDir) {
        this.prefix = prefix;
        this.threads = threads;
        this.outputDir = outputDir;
    }

    public void run() {

        ExecutorService workers = Executors.newFixedThreadPool(threads);
        ScheduledExecutorService reporter =
                Executors.newSingleThreadScheduledExecutor();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nShutting down...");
            workers.shutdownNow();
            reporter.shutdownNow();
        }));

        reporter.scheduleAtFixedRate(
                new RateReporter(totalIterations), 3, 3, TimeUnit.SECONDS);

        for (int i = 0; i < threads; i++) {
            workers.submit(this::workerLoop);
        }
    }

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
            e.printStackTrace();
        }
    }

}
