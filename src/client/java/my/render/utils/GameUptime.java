package my.render.utils;

public final class GameUptime {
    private static long startNanos = -1L;

    private GameUptime() {
    }

    public static void markStart() {
        if (startNanos < 0L) {
            startNanos = System.nanoTime();
        }
    }

    public static long elapsedMillis() {
        if (startNanos < 0L) {
            return 0L;
        }
        return Math.max(0L, (System.nanoTime() - startNanos) / 1_000_000L);
    }

    public static long elapsedSeconds() {
        return elapsedMillis() / 1000L;
    }
}