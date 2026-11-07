package org.junit.jupiter.api;

/**
 * Offline stub of the JUnit 5 assertions VortexMC's tests use.
 *
 * <p>Only the overloads the test sources actually call are declared, so that a mismatch between
 * the tests and real JUnit shows up as a compile error rather than a silent difference.</p>
 */
public final class Assertions {

    private Assertions() {
    }

    public static void assertTrue(boolean condition) {
        assertTrue(condition, null);
    }

    public static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message == null ? "expected true" : message);
        }
    }

    public static void assertFalse(boolean condition) {
        assertFalse(condition, null);
    }

    public static void assertFalse(boolean condition, String message) {
        if (condition) {
            throw new AssertionError(message == null ? "expected false" : message);
        }
    }

    public static void assertEquals(Object expected, Object actual) {
        assertEquals(expected, actual, null);
    }

    public static void assertEquals(Object expected, Object actual, String message) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError((message == null ? "" : message + " ==> ")
                    + "expected: " + expected + " but was: " + actual);
        }
    }

    public static void assertEquals(long expected, long actual) {
        assertEquals(expected, actual, null);
    }

    public static void assertEquals(long expected, long actual, String message) {
        if (expected != actual) {
            throw new AssertionError((message == null ? "" : message + " ==> ")
                    + "expected: " + expected + " but was: " + actual);
        }
    }

    public static void assertEquals(double expected, double actual, double delta) {
        assertEquals(expected, actual, delta, null);
    }

    public static void assertEquals(double expected, double actual, double delta, String message) {
        if (Math.abs(expected - actual) > delta) {
            throw new AssertionError((message == null ? "" : message + " ==> ")
                    + "expected: " + expected + " but was: " + actual + " (delta " + delta + ")");
        }
    }

    public static void assertNotNull(Object value) {
        assertNotNull(value, null);
    }

    public static void assertNotNull(Object value, String message) {
        if (value == null) {
            throw new AssertionError(message == null ? "expected not null" : message);
        }
    }

    public static void assertNull(Object value) {
        assertNull(value, null);
    }

    public static void assertNull(Object value, String message) {
        if (value != null) {
            throw new AssertionError(message == null ? "expected null" : message);
        }
    }

    public static void assertThrows(Class<? extends Throwable> expected, Runnable runnable) {
        try {
            runnable.run();
        } catch (Throwable thrown) {
            if (expected.isInstance(thrown)) {
                return;
            }
            throw new AssertionError("expected " + expected.getName() + " but threw " + thrown);
        }
        throw new AssertionError("expected " + expected.getName() + " but nothing was thrown");
    }

    public static void fail(String message) {
        throw new AssertionError(message == null ? "failed" : message);
    }
}
