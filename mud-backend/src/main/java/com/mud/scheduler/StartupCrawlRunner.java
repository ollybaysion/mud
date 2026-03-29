package com.mud.scheduler;

import com.mud.crawler.*;
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
import java.util.function.Supplier;

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
    private final NvidiaBlogCrawler nvidiaBlogCrawler;
    private final ServeTheHomeCrawler serveTheHomeCrawler;
    private final TomsHardwareCrawler tomsHardwareCrawler;
    private final PhoronixCrawler phoronixCrawler;
    private final TechPowerUpCrawler techPowerUpCrawler;
    private final HackadayCrawler hackadayCrawler;
    private final EETimesCrawler eeTimesCrawler;
    private final SemiEngineeringCrawler semiEngineeringCrawler;
    private final ChipsAndCheeseCrawler chipsAndCheeseCrawler;
    private final CNXSoftwareCrawler cnxSoftwareCrawler;
    private final TheNextPlatformCrawler theNextPlatformCrawler;
    private final HPCwireCrawler hpcwireCrawler;
    private final IEEESpectrumCrawler ieeeSpectrumCrawler;
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
        log.info("전체 크롤러 실행 시작");
        List<TrendItem> all = new ArrayList<>();
        runCrawler("HACKER_NEWS", hackerNewsCrawler::crawl, all);
        runCrawler("DEV_TO", devToCrawler::crawl, all);
        runCrawler("REDDIT", redditRssCrawler::crawl, all);
        runCrawler("ARXIV", arXivCrawler::crawl, all);
        runCrawler("GITHUB", gitHubTrendingCrawler::crawl, all);
        runCrawler("PAPERS_WITH_CODE", papersWithCodeCrawler::crawl, all);
        runCrawler("INFOQ", infoQCrawler::crawl, all);
        runCrawler("HUGGING_FACE", huggingFaceCrawler::crawl, all);
        runCrawler("LOBSTERS", lobstersCrawler::crawl, all);
        runCrawler("INSIDE_JAVA", insideJavaCrawler::crawl, all);
        runCrawler("ISOCPP", isoCppCrawler::crawl, all);
        runCrawler("TLDR_AI", tldrCrawler::crawl, all);
        runCrawler("THE_NEW_STACK", theNewStackCrawler::crawl, all);
        runCrawler("CNCF", cncfCrawler::crawl, all);
        runCrawler("STACKOVERFLOW_BLOG", stackOverflowBlogCrawler::crawl, all);
        runCrawler("MARTIN_FOWLER", martinFowlerCrawler::crawl, all);
        runCrawler("JETBRAINS", jetBrainsCrawler::crawl, all);
        runCrawler("GEEKNEWS", geekNewsCrawler::crawl, all);
        runCrawler("NVIDIA_BLOG", nvidiaBlogCrawler::crawl, all);
        runCrawler("SERVE_THE_HOME", serveTheHomeCrawler::crawl, all);
        runCrawler("TOMS_HARDWARE", tomsHardwareCrawler::crawl, all);
        runCrawler("PHORONIX", phoronixCrawler::crawl, all);
        runCrawler("TECHPOWERUP", techPowerUpCrawler::crawl, all);
        runCrawler("HACKADAY", hackadayCrawler::crawl, all);
        runCrawler("EE_TIMES", eeTimesCrawler::crawl, all);
        runCrawler("SEMI_ENGINEERING", semiEngineeringCrawler::crawl, all);
        runCrawler("CHIPS_AND_CHEESE", chipsAndCheeseCrawler::crawl, all);
        runCrawler("CNX_SOFTWARE", cnxSoftwareCrawler::crawl, all);
        runCrawler("THE_NEXT_PLATFORM", theNextPlatformCrawler::crawl, all);
        runCrawler("HPCWIRE", hpcwireCrawler::crawl, all);
        runCrawler("IEEE_SPECTRUM", ieeeSpectrumCrawler::crawl, all);

        log.info("크롤링 완료: 총 {}개 신규 항목", all.size());
        analysisService.analyzePendingItems();
    }

    private void runCrawler(String source, Supplier<List<TrendItem>> crawler, List<TrendItem> all) {
        LocalDateTime startedAt = LocalDateTime.now();
        try {
            List<TrendItem> items = crawler.get();
            all.addAll(items);
            crawlerMonitorService.recordRun(source, startedAt, items.size(), null);
        } catch (Exception e) {
            log.error("{} 크롤 실패", source, e);
            crawlerMonitorService.recordRun(source, startedAt, 0, e.getMessage());
        }
    }
}
