package daeuk.tokenbucket;

import java.time.Clock;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

public class TokenBucketRateLimiter {

    private final AtomicLong tokenCount;
    private volatile long lastRefillTimeMillis;
    private final long capacity;
    private final int refillRate;
    private final long refillIntervalMillis;
    private final Clock clock;
    private final ReentrantLock lock = new ReentrantLock();

    public TokenBucketRateLimiter(long capacity, int refillRate, long refillIntervalMillis) {
        this(capacity, refillRate, refillIntervalMillis, Clock.systemDefaultZone());
    }

    TokenBucketRateLimiter(long capacity, int refillRate, long refillIntervalMillis, Clock clock) {
        this.tokenCount = new AtomicLong(capacity);
        this.lastRefillTimeMillis = clock.millis();
        this.capacity = capacity;
        this.refillRate = refillRate;
        this.refillIntervalMillis = refillIntervalMillis;
        this.clock = clock;
    }

    public boolean tryAcquire() {
        refill();

        return tokenCount.getAndUpdate(count -> Math.max(0, count - 1)) > 0;
    }

    private void refill() {
        long currentTime = clock.millis();

        if (currentTime <= lastRefillTimeMillis) {
            return;
        }

        lock.lock();
        try {
            long lastTime = lastRefillTimeMillis;
            long elapsedTime = currentTime - lastTime;
            if (elapsedTime < refillIntervalMillis) {
                return;
            }
            long refillCount = elapsedTime / refillIntervalMillis;
            long tokensToAdd = refillRate * refillCount;
            lastRefillTimeMillis = lastTime + refillCount * refillIntervalMillis;
            tokenCount.updateAndGet(count -> Math.min(capacity, count + tokensToAdd));
        } finally {
            lock.unlock();
        }
    }

}
