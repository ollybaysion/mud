package com.mud.scheduler.jobs;

import com.mud.crawler.CrawlerBase;
import com.mud.domain.entity.TrendItem;
import com.mud.service.AnalysisService;
import com.mud.service.CrawlerMonitorService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class AllSourcesCrawlJob implements Job {

    @Autowired private List<CrawlerBase> crawlers;
    @Autowired private AnalysisService analysisService;
    @Autowired private CrawlerMonitorService crawlerMonitorService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        List<CrawlerBase> scheduled = crawlers.stream()
            .filter(CrawlerBase::isScheduledCrawlEnabled)
            .toList();
        log.info("=== All sources crawl job started: {} crawlers ===", scheduled.size());
        try {
            List<TrendItem> all = new ArrayList<>();
            for (CrawlerBase crawler : scheduled) {
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

            analysisService.analyzePendingItems();
            log.info("=== All sources crawl job finished: {} new items ===", all.size());
        } catch (Exception e) {
            log.error("All sources crawl job failed", e);
            throw new JobExecutionException(e);
        }
    }
}
