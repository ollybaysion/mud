package com.mud.scheduler.jobs;

import com.mud.crawler.GitHubTrendingCrawler;
import com.mud.domain.entity.TrendItem;
import com.mud.service.AnalysisService;
import com.mud.service.TrendService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class GitHubCrawlJob implements Job {

    @Autowired private GitHubTrendingCrawler crawler;
    @Autowired private AnalysisService analysisService;
    @Autowired private TrendService trendService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.info("=== GitHub Trending crawl job started ===");
        try {
            List<TrendItem> newItems = crawler.crawl();
            if (!newItems.isEmpty()) {
                analysisService.analyzePendingItems();
                trendService.evictTrendCaches();
            }
            log.info("=== GitHub crawl job finished: {} new items ===", newItems.size());
        } catch (Exception e) {
            log.error("GitHub crawl job failed", e);
            throw new JobExecutionException(e);
        }
    }
}
