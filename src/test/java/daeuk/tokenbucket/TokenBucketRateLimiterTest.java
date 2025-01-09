package daeuk.tokenbucket;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TokenBucketRateLimiterTest {

    @Test
    @DisplayName("Initial capacity test: tokenCount 넘는 tryAcquire, False 반환")
    void initialCapacityTest() {
        var sut = new TokenBucketRateLimiter(3, 1, 1000);

        for (int i = 0; i < 3; i++) {
            assertThat(sut.tryAcquire()).isTrue();
        }
        assertThat(sut.tryAcquire()).isFalse();
    }

    @Test
    @DisplayName("Refill test: interval 이후 refill 확인")
    void refillTokensAfterInterval() {
        Clock clock = mock(Clock.class);
        var sut = new TokenBucketRateLimiter(2, 1, 1000, clock);
        when(clock.millis()).thenReturn(0L, 0L, 0L, 1000L, 1000L);

        assertThat(sut.tryAcquire()).isTrue();
        assertThat(sut.tryAcquire()).isTrue();
        assertThat(sut.tryAcquire()).isFalse();
        assertThat(sut.tryAcquire()).isTrue();
        assertThat(sut.tryAcquire()).isFalse();
    }

    @Test
    @DisplayName("Concurrency test: capacity 50, 1000번 병렬 tryAcquire 시도")
    void concurrencyTest() {
        var sut = new TokenBucketRateLimiter(50, 1, 1000);
        AtomicInteger successCount = new AtomicInteger(0);

        IntStream.range(0, 1000)
            .parallel()
            .forEach(i -> {
                if (sut.tryAcquire()) {
                    successCount.incrementAndGet();
                }
            });

        assertThat(50).isEqualTo(successCount.get());
    }

    @Test
    @DisplayName("Concurrency test: capacity 1000, 1000번 병렬 tryAcquire 시도")
    void test() {
        var sut = new TokenBucketRateLimiter(1000, 1, 1000);
        AtomicInteger successCount = new AtomicInteger(0);

        IntStream.range(0, 1000)
            .parallel()
            .forEach(i -> {
                if (sut.tryAcquire()) {
                    successCount.incrementAndGet();
                }
            });

        assertThat(1000).isEqualTo(successCount.get());
    }

}
