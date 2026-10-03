package com.example.ipd_sp_back_end.assistant;

/** Immutable deadline owned by one request, never by the shared model client. */
public final class AssistantCallDeadline {
    private final long end;
    private AssistantCallDeadline(long end) { this.end = end; }
    public static AssistantCallDeadline afterSeconds(int seconds) { return new AssistantCallDeadline(System.nanoTime() + seconds * 1_000_000_000L); }
    public static AssistantCallDeadline unlimited() { return new AssistantCallDeadline(Long.MAX_VALUE); }
    public int timeout(int maximum) {
        if (end == Long.MAX_VALUE) return maximum;
        long remaining = (end - System.nanoTime()) / 1_000_000_000L;
        if (remaining < 1) throw new IllegalStateException("Assistant turn deadline exceeded.");
        return (int)Math.min(maximum, remaining);
    }
}
