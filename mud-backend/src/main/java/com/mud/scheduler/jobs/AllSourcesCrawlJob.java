package com.mud.scheduler.jobs;

import com.mud.crawler.*;
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

    @Autowired private ArXivCrawler arXivCrawler;
    @Autowired private DevToCrawler devToCrawler;
    @Autowired private RedditRssCrawler redditRssCrawler;
    @Autowired private PapersWithCodeCrawler papersWithCodeCrawler;
    @Autowired private InfoQCrawler infoQCrawler;
    @Autowired private HuggingFaceCrawler huggingFaceCrawler;
    @Autowired private LobstersCrawler lobstersCrawler;
    @Autowired private InsideJavaCrawler insideJavaCrawler;
    @Autowired private ISOCppCrawler isoCppCrawler;
    @Autowired private TLDRCrawler tldrCrawler;
    @Autowired private TheNewStackCrawler theNewStackCrawler;
    @Autowired private CNCFCrawler cncfCrawler;
    @Autowired private StackOverflowBlogCrawler stackOverflowBlogCrawler;
    @Autowired private MartinFowlerCrawler martinFowlerCrawler;
    @Autowired private JetBrainsCrawler jetBrainsCrawler;
    @Autowired private GeekNewsCrawler geekNewsCrawler;
    @Autowired private AnalysisService analysisService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.info("=== All sources crawl job started ===");
        try {
            List<TrendItem> all = new ArrayList<>();
            try { all.addAll(devToCrawler.crawl()); }             catch (Exception e) { log.error("devto 크롤 실패", e); }
            try { all.addAll(redditRssCrawler.crawl()); }         catch (Exception e) { log.error("Reddit 크롤 실패", e); }
            try { all.addAll(arXivCrawler.crawl()); }             catch (Exception e) { log.error("ArXiv 크롤 실패", e); }
            try { all.addAll(papersWithCodeCrawler.crawl()); }    catch (Exception e) { log.error("PwC 크롤 실패", e); }
            try { all.addAll(infoQCrawler.crawl()); }             catch (Exception e) { log.error("InfoQ 크롤 실패", e); }
            try { all.addAll(huggingFaceCrawler.crawl()); }       catch (Exception e) { log.error("HuggingFace 크롤 실패", e); }
            try { all.addAll(lobstersCrawler.crawl()); }          catch (Exception e) { log.error("Lobsters 크롤 실패", e); }
            try { all.addAll(insideJavaCrawler.crawl()); }        catch (Exception e) { log.error("InsideJava 크롤 실패", e); }
            try { all.addAll(isoCppCrawler.crawl()); }            catch (Exception e) { log.error("ISOCpp 크롤 실패", e); }
            try { all.addAll(tldrCrawler.crawl()); }              catch (Exception e) { log.error("TLDR 크롤 실패", e); }
            try { all.addAll(theNewStackCrawler.crawl()); }       catch (Exception e) { log.error("NewStack 크롤 실패", e); }
            try { all.addAll(cncfCrawler.crawl()); }              catch (Exception e) { log.error("CNCF 크롤 실패", e); }
            try { all.addAll(stackOverflowBlogCrawler.crawl()); } catch (Exception e) { log.error("SO Blog 크롤 실패", e); }
            try { all.addAll(martinFowlerCrawler.crawl()); }      catch (Exception e) { log.error("MartinFowler 크롤 실패", e); }
            try { all.addAll(jetBrainsCrawler.crawl()); }         catch (Exception e) { log.error("JetBrains 크롤 실패", e); }
            try { all.addAll(geekNewsCrawler.crawl()); }          catch (Exception e) { log.error("GeekNews 크롤 실패", e); }

            analysisService.analyzePendingItems();
            log.info("=== All sources crawl job finished: {} new items ===", all.size());
        } catch (Exception e) {
            log.error("All sources crawl job failed", e);
            throw new JobExecutionException(e);
        }
    }
}
