package io.github.sts8.vanitygen.bruteforce;

import java.util.concurrent.atomic.AtomicLong;

public final class RateReporter implements Runnable {

    private final AtomicLong totalIterations;
    private long lastTotal = 0;
    private long lastTime = System.nanoTime();

    public RateReporter(AtomicLong totalIterations) {
        this.totalIterations = totalIterations;
    }

    @Override
    public void run() {
        long now = System.nanoTime();
        long total = totalIterations.get();

        long delta = total - lastTotal;
        double seconds = (now - lastTime) / 1_000_000_000.0;
        double rate = delta / seconds;

        System.out.printf("Total: %,d\t| Rate: %,.0f /s%n", total, rate);

        lastTotal = total;
        lastTime = now;
    }
}
