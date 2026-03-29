package com.mud.scheduler;

import com.mud.crawler.CrawlerBase;
import com.mud.domain.entity.TrendItem;
import com.mud.service.AnalysisService;
import com.mud.service.CrawlerMonitorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class StartupCrawlRunner {

    private final List<CrawlerBase> crawlers;
    private final AnalysisService analysisService;
    private final CrawlerMonitorService crawlerMonitorService;

    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void runAllCrawlersAsync() {
        log.info("=== 시작 시 초기 크롤링 실행 (30초 후) ===");
        try {
            Thread.sleep(30_000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }
        runAllCrawlers();
    }

    public void runAllCrawlers() {
        log.info("전체 크롤러 실행 시작: {}개 크롤러", crawlers.size());
        List<TrendItem> all = new ArrayList<>();

        for (CrawlerBase crawler : crawlers) {
            String source = crawler.getSourceName();
            LocalDateTime startedAt = LocalDateTime.now();
            try {
                List<TrendItem> items = crawler.crawl();
                all.addAll(items);
                crawlerMonitorService.recordRun(source, startedAt, items.size(), null);
            } catch (Exception e) {
                log.error("{} 크롤 실패", source, e);
                crawlerMonitorService.recordRun(source, startedAt, 0, e.getMessage());
            }
        }

        log.info("크롤링 완료: 총 {}개 신규 항목", all.size());
        analysisService.analyzePendingItems();
    }
}
