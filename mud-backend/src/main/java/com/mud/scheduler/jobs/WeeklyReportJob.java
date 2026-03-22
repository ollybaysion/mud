package com.mud.scheduler.jobs;

import com.mud.service.WeeklyReportService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class WeeklyReportJob implements Job {

    @Autowired private WeeklyReportService weeklyReportService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.info("=== Weekly report job started ===");
        try {
            weeklyReportService.generateWeeklyReport();
            log.info("=== Weekly report job finished ===");
        } catch (Exception e) {
            log.error("Weekly report job failed", e);
            throw new JobExecutionException(e);
        }
    }
}
