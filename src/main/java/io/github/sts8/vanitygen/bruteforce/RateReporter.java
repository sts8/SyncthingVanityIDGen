package io.github.sts8.vanitygen.bruteforce;

import java.util.concurrent.atomic.AtomicLong;

/**
 * A background reporter that calculates and prints the iteration rate of the brute-force process.
 * <p>
 * This class implements {@link Runnable} and is intended to be executed periodically
 * to provide real-time performance telemetry.
 */
public final class RateReporter implements Runnable {

    /**
     * The shared thread-safe counter tracking total iterations across all threads.
     */
    private final AtomicLong totalIterations;

    /**
     * The total count recorded during the previous execution.
     */
    private long lastTotal = 0;

    /**
     * The timestamp (in nanoseconds) of the previous execution.
     */
    private long lastTime = System.nanoTime();

    /**
     * Constructs a new RateReporter.
     *
     * @param totalIterations the {@link AtomicLong} being incremented by the worker threads
     */
    public RateReporter(AtomicLong totalIterations) {
        this.totalIterations = totalIterations;
    }

    /**
     * Calculates the iterations performed since the last run and prints the statistics to stdout.
     * <p>
     * The rate is calculated as: {@code (currentTotal - lastTotal) / timeDelta}.
     */
    @Override
    public void run() {
        long now = System.nanoTime();
        long total = totalIterations.get();
        long delta = total - lastTotal;

        // Convert nanoseconds to seconds for the rate calculation
        double seconds = (now - lastTime) / 1_000_000_000.0;
        double rate = delta / seconds;

        System.out.printf("Total: %,d\t| Rate: %,.0f /s%n", total, rate);

        lastTotal = total;
        lastTime = now;
    }
}
