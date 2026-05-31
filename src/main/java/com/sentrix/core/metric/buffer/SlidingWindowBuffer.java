package com.sentrix.core.metric.buffer;

import com.sentrix.core.metric.dto.CurrentMetricsResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

@Component
public class SlidingWindowBuffer {

    private final Deque<CurrentMetricsResponse> buffer = new ArrayDeque<>();

    private final int windowSizeSeconds;

    public SlidingWindowBuffer(
            @Value("${sentrix.diagnosis.window-size-seconds}") int windowSizeSeconds
    ) {
        this.windowSizeSeconds = windowSizeSeconds;
    }

    public synchronized void add(CurrentMetricsResponse metrics) {
        buffer.addLast(metrics);
        removeExpired(metrics.getTimestamp());
    }

    private void removeExpired(LocalDateTime now) {
        while (!buffer.isEmpty()) {
            CurrentMetricsResponse oldest = buffer.peekFirst();

            long ageSeconds = Duration.between(oldest.getTimestamp(), now).getSeconds();

            if (ageSeconds > windowSizeSeconds) {
                buffer.removeFirst();
            } else {
                break;
            }
        }
    }

    public synchronized List<CurrentMetricsResponse> getSnapshot() {
        return List.copyOf(buffer);
    }

    public synchronized int size() {
        return buffer.size();
    }

    public synchronized boolean isReady() {
        if (buffer.isEmpty()) {
            return false;
        }

        LocalDateTime oldest = buffer.peekFirst().getTimestamp();
        LocalDateTime latest = buffer.peekLast().getTimestamp();

        long durationSeconds = Duration.between(oldest, latest).getSeconds();

        return durationSeconds >= windowSizeSeconds;
    }

    public synchronized LocalDateTime getOldestTimestamp() {
        return buffer.isEmpty() ? null : buffer.peekFirst().getTimestamp();
    }

    public synchronized LocalDateTime getLatestTimestamp() {
        return buffer.isEmpty() ? null : buffer.peekLast().getTimestamp();
    }
}