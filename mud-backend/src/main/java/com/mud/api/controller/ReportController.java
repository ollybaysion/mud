package com.mud.api.controller;

import com.mud.dto.response.WeeklyReportResponse;
import com.mud.service.WeeklyReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class ReportController {

    private final WeeklyReportService weeklyReportService;

    @GetMapping("/api/reports/weekly")
    public ResponseEntity<WeeklyReportResponse> getWeeklyReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        var report = date != null
            ? weeklyReportService.getReportByDate(date)
            : weeklyReportService.getLatestReport();

        return report
            .map(r -> ResponseEntity.ok(WeeklyReportResponse.from(r)))
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/api/admin/reports/generate")
    public ResponseEntity<?> generateReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        var report = weeklyReportService.getOrGenerateReport(date);
        if (report == null) {
            return ResponseEntity.internalServerError().body(Map.of("error", "리포트 생성 실패"));
        }
        return ResponseEntity.ok(WeeklyReportResponse.from(report));
    }

    @PostMapping("/api/admin/reports/regenerate")
    public ResponseEntity<?> regenerateReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        weeklyReportService.regenerateReport(date);
        var report = weeklyReportService.getReportByDate(date);
        return report
            .map(r -> ResponseEntity.ok((Object) WeeklyReportResponse.from(r)))
            .orElse(ResponseEntity.internalServerError().body(Map.of("error", "리포트 재생성 실패")));
    }
}
