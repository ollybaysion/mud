package com.mud.scheduler.jobs;

import com.mud.crawler.HackerNewsCrawler;
import com.mud.domain.entity.TrendItem;
import com.mud.service.AnalysisService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class HackerNewsCrawlJob implements Job {

    @Autowired private HackerNewsCrawler crawler;
    @Autowired private AnalysisService analysisService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.info("=== HackerNews crawl job started ===");
        try {
            List<TrendItem> newItems = crawler.crawl();
            if (!newItems.isEmpty()) {
                analysisService.analyzePendingItems();
            }
            log.info("=== HackerNews crawl job finished: {} new items ===", newItems.size());
        } catch (Exception e) {
            log.error("HackerNews crawl job failed", e);
            throw new JobExecutionException(e);
        }
    }
}
