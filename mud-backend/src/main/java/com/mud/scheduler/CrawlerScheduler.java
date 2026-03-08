package com.mud.scheduler;

import com.mud.scheduler.jobs.AllSourcesCrawlJob;
import com.mud.scheduler.jobs.GitHubCrawlJob;
import com.mud.scheduler.jobs.HackerNewsCrawlJob;
import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CrawlerScheduler {

    // GitHub Trending - every 30 min at :00
    @Bean
    public JobDetail githubJobDetail() {
        return JobBuilder.newJob(GitHubCrawlJob.class)
            .withIdentity("githubCrawlJob")
            .storeDurably()
            .build();
    }

    @Bean
    public Trigger githubTrigger(JobDetail githubJobDetail) {
        return TriggerBuilder.newTrigger()
            .forJob(githubJobDetail)
            .withIdentity("githubTrigger")
            .withSchedule(CronScheduleBuilder.cronSchedule("0 0/30 * * * ?"))
            .build();
    }

    // Hacker News - every 30 min at :05
    @Bean
    public JobDetail hackerNewsJobDetail() {
        return JobBuilder.newJob(HackerNewsCrawlJob.class)
            .withIdentity("hackerNewsCrawlJob")
            .storeDurably()
            .build();
    }

    @Bean
    public Trigger hackerNewsTrigger(JobDetail hackerNewsJobDetail) {
        return TriggerBuilder.newTrigger()
            .forJob(hackerNewsJobDetail)
            .withIdentity("hackerNewsTrigger")
            .withSchedule(CronScheduleBuilder.cronSchedule("0 5/30 * * * ?"))
            .build();
    }

    // dev.to + Reddit + ArXiv - every 30 min at :10 (ArXiv effectively every 6h via its own logic)
    @Bean
    public JobDetail allSourcesJobDetail() {
        return JobBuilder.newJob(AllSourcesCrawlJob.class)
            .withIdentity("allSourcesCrawlJob")
            .storeDurably()
            .build();
    }

    @Bean
    public Trigger allSourcesTrigger(JobDetail allSourcesJobDetail) {
        return TriggerBuilder.newTrigger()
            .forJob(allSourcesJobDetail)
            .withIdentity("allSourcesTrigger")
            .withSchedule(CronScheduleBuilder.cronSchedule("0 10/30 * * * ?"))
            .build();
    }
}
