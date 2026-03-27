package querycraft;

import org.junit.Test;
import querycraft.util.ResourceUtils;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

public class ResourceUtilsTest {

    @Test
    public void testCloseQuietlyWithNull() {
        ResourceUtils.closeQuietly((AutoCloseable) null);
    }

    @Test
    public void testCloseQuietlyClosesResource() {
        AtomicBoolean closed = new AtomicBoolean(false);
        AutoCloseable closeable = () -> closed.set(true);

        ResourceUtils.closeQuietly(closeable);

        assertTrue(closed.get());
    }

    @Test
    public void testWithResourceClosesResource() {
        AtomicBoolean closed = new AtomicBoolean(false);
        AutoCloseable closeable = () -> closed.set(true);

        ResourceUtils.withResource(closeable, resource -> { });

        assertTrue(closed.get());
    }

    @Test
    public void testWithResourceWrapsException() {
        AutoCloseable closeable = () -> { };

        try {
            ResourceUtils.withResource(closeable, resource -> {
                throw new IllegalStateException("boom");
            });
            fail("Expected RuntimeException");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("Resource operation failed"));
            assertTrue(e.getCause() instanceof IllegalStateException);
        }
    }
}
