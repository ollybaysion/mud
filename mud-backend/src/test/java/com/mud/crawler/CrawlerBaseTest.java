package com.mud.crawler;

import com.mud.domain.entity.TrendItem;
import com.mud.domain.repository.TrendItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CrawlerBaseTest {

    @Mock
    private TrendItemRepository trendItemRepository;

    private TestCrawler crawler;

    @BeforeEach
    void setUp() {
        crawler = new TestCrawler(trendItemRepository);
    }

    // Concrete implementation for testing abstract class
    static class TestCrawler extends CrawlerBase {
        public TestCrawler(TrendItemRepository repo) { super(repo); }
        @Override public List<TrendItem> crawl() { return List.of(); }
    }

    @Test
    @DisplayName("동일 URL → 동일 해시")
    void sameUrlSameHash() {
        String hash1 = crawler.computeUrlHash("https://example.com/article");
        String hash2 = crawler.computeUrlHash("https://example.com/article");
        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    @DisplayName("다른 URL → 다른 해시")
    void differentUrlDifferentHash() {
        String hash1 = crawler.computeUrlHash("https://example.com/a");
        String hash2 = crawler.computeUrlHash("https://example.com/b");
        assertThat(hash1).isNotEqualTo(hash2);
    }

    @Test
    @DisplayName("신규 아이템 저장 성공")
    void saveIfNewSuccess() {
        TrendItem item = TrendItem.builder()
            .title("Test")
            .originalUrl("https://example.com/new")
            .urlHash("newhash")
            .source(TrendItem.CrawlSource.GITHUB)
            .build();

        when(trendItemRepository.existsByUrlHash("newhash")).thenReturn(false);
        when(trendItemRepository.save(any())).thenReturn(item);

        TrendItem result = crawler.saveIfNew(item);
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Test");
    }

    @Test
    @DisplayName("중복 아이템 → null 반환")
    void saveIfNewDuplicate() {
        TrendItem item = TrendItem.builder()
            .title("Test")
            .originalUrl("https://example.com/dup")
            .urlHash("duphash")
            .source(TrendItem.CrawlSource.GITHUB)
            .build();

        when(trendItemRepository.existsByUrlHash("duphash")).thenReturn(true);

        TrendItem result = crawler.saveIfNew(item);
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("레이스 컨디션 예외 → null 반환")
    void saveIfNewRaceCondition() {
        TrendItem item = TrendItem.builder()
            .title("Test")
            .originalUrl("https://example.com/race")
            .urlHash("racehash")
            .source(TrendItem.CrawlSource.GITHUB)
            .build();

        when(trendItemRepository.existsByUrlHash("racehash")).thenReturn(false);
        when(trendItemRepository.save(any())).thenThrow(new RuntimeException("Duplicate key"));

        TrendItem result = crawler.saveIfNew(item);
        assertThat(result).isNull();
    }
}
