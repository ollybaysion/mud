package com.mud.crawler;

import com.mud.domain.entity.TrendItem;
import com.mud.domain.repository.TrendItemRepository;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class CrawlerBaseRetryTest {

    @Mock private TrendItemRepository trendItemRepository;

    static class TestCrawler extends CrawlerBase {
        public TestCrawler(TrendItemRepository repo) { super(repo); }
        @Override public List<TrendItem> crawl() { return List.of(); }
    }

    @FunctionalInterface
    interface FetchAction {
        Document execute() throws IOException;
    }

    private Document invokeRetry(CrawlerBase crawler, FetchAction action, String url) throws Exception {
        // Access the private FetchAction interface and fetchWithRetry method
        Class<?>[] innerClasses = CrawlerBase.class.getDeclaredClasses();
        Class<?> fetchActionClass = null;
        for (Class<?> c : innerClasses) {
            if (c.getSimpleName().equals("FetchAction")) {
                fetchActionClass = c;
                break;
            }
        }

        Method fetchWithRetry = CrawlerBase.class.getDeclaredMethod("fetchWithRetry", fetchActionClass, String.class);
        fetchWithRetry.setAccessible(true);

        // Create proxy for the inner FetchAction interface
        Object proxy = java.lang.reflect.Proxy.newProxyInstance(
            fetchActionClass.getClassLoader(),
            new Class<?>[]{fetchActionClass},
            (p, method, args) -> action.execute()
        );

        return (Document) fetchWithRetry.invoke(crawler, proxy, url);
    }

    @Test
    @DisplayName("첫 시도 성공 → 즉시 반환")
    void successOnFirstAttempt() throws Exception {
        TestCrawler crawler = new TestCrawler(trendItemRepository);
        Document doc = new Document("https://example.com");

        Document result = invokeRetry(crawler, () -> doc, "https://example.com");
        assertThat(result).isSameAs(doc);
    }

    @Test
    @DisplayName("2회 실패 후 3회째 성공")
    void successAfterRetries() throws Exception {
        TestCrawler crawler = new TestCrawler(trendItemRepository);
        Document doc = new Document("https://example.com");
        AtomicInteger attempts = new AtomicInteger(0);

        Document result = invokeRetry(crawler, () -> {
            if (attempts.incrementAndGet() < 3) {
                throw new IOException("timeout");
            }
            return doc;
        }, "https://example.com");

        assertThat(result).isSameAs(doc);
        assertThat(attempts.get()).isEqualTo(3);
    }

    @Test
    @DisplayName("3회 모두 실패 → IOException throw")
    void failsAfterMaxRetries() {
        TestCrawler crawler = new TestCrawler(trendItemRepository);
        AtomicInteger attempts = new AtomicInteger(0);

        assertThatThrownBy(() -> invokeRetry(crawler, () -> {
            attempts.incrementAndGet();
            throw new IOException("connection refused");
        }, "https://example.com"))
            .hasCauseInstanceOf(IOException.class);

        assertThat(attempts.get()).isEqualTo(3);
    }
}
