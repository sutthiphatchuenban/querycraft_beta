package querycraft;

import javafx.application.Platform;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class JavaFxTestBootstrap {

    private static final AtomicBoolean STARTED = new AtomicBoolean(false);

    private JavaFxTestBootstrap() {
    }

    public static void initToolkit() {
        if (STARTED.compareAndSet(false, true)) {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.startup(latch::countDown);
            try {
                if (!latch.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("JavaFX toolkit startup timed out");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while starting JavaFX toolkit", e);
            }
        }
    }

    public static void runOnFxThreadAndWait(Runnable runnable) {
        initToolkit();
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                runnable.run();
            } finally {
                latch.countDown();
            }
        });
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("JavaFX action timed out");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for JavaFX action", e);
        }
    }
}
