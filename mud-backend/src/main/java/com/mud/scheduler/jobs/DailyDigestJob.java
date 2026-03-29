package com.mud.scheduler.jobs;

import com.mud.service.DigestService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DailyDigestJob implements Job {

    @Autowired private DigestService digestService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.info("=== Daily digest job started ===");
        try {
            digestService.sendDailyDigest();
            log.info("=== Daily digest job finished ===");
        } catch (Exception e) {
            log.error("Daily digest job failed", e);
            throw new JobExecutionException(e);
        }
    }
}
