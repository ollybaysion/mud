package com.mud.scheduler.jobs;

import com.mud.crawler.CrawlerBase;
import com.mud.domain.entity.TrendItem;
import com.mud.service.AnalysisService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class AllSourcesCrawlJob implements Job {

    @Autowired private List<CrawlerBase> crawlers;
    @Autowired private AnalysisService analysisService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.info("=== All sources crawl job started: {} crawlers ===", crawlers.size());
        try {
            List<TrendItem> all = new ArrayList<>();
            for (CrawlerBase crawler : crawlers) {
                try {
                    all.addAll(crawler.crawl());
                } catch (Exception e) {
                    log.error("{} 크롤 실패", crawler.getSourceName(), e);
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
