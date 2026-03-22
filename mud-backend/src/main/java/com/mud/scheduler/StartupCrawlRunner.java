package com.mud.scheduler;

import com.mud.crawler.*;
import com.mud.domain.entity.TrendItem;
import com.mud.service.AnalysisService;
import com.mud.service.TrendService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class StartupCrawlRunner {

    private final HackerNewsCrawler hackerNewsCrawler;
    private final GitHubTrendingCrawler gitHubTrendingCrawler;
    private final ArXivCrawler arXivCrawler;
    private final DevToCrawler devToCrawler;
    private final RedditRssCrawler redditRssCrawler;
    private final PapersWithCodeCrawler papersWithCodeCrawler;
    private final InfoQCrawler infoQCrawler;
    private final HuggingFaceCrawler huggingFaceCrawler;
    private final LobstersCrawler lobstersCrawler;
    private final InsideJavaCrawler insideJavaCrawler;
    private final ISOCppCrawler isoCppCrawler;
    private final TLDRCrawler tldrCrawler;
    private final TheNewStackCrawler theNewStackCrawler;
    private final CNCFCrawler cncfCrawler;
    private final StackOverflowBlogCrawler stackOverflowBlogCrawler;
    private final MartinFowlerCrawler martinFowlerCrawler;
    private final JetBrainsCrawler jetBrainsCrawler;
    private final GeekNewsCrawler geekNewsCrawler;
    private final AnalysisService analysisService;
    private final TrendService trendService;

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
        log.info("전체 크롤러 실행 시작");
        List<TrendItem> all = new ArrayList<>();
        try { all.addAll(hackerNewsCrawler.crawl()); }        catch (Exception e) { log.error("HN 크롤 실패", e); }
        try { all.addAll(devToCrawler.crawl()); }             catch (Exception e) { log.error("devto 크롤 실패", e); }
        try { all.addAll(redditRssCrawler.crawl()); }         catch (Exception e) { log.error("Reddit 크롤 실패", e); }
        try { all.addAll(arXivCrawler.crawl()); }             catch (Exception e) { log.error("ArXiv 크롤 실패", e); }
        try { all.addAll(gitHubTrendingCrawler.crawl()); }    catch (Exception e) { log.error("GitHub 크롤 실패", e); }
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

        log.info("크롤링 완료: 총 {}개 신규 항목", all.size());
        analysisService.analyzePendingItems();
        trendService.evictTrendCaches();
    }
}
