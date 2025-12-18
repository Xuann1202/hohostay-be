package tw.com.ispan.eeit.ho_back.analytics_daily;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * 排程控制器
 * 提供手動觸發統計的 API（用於測試或補資料）
 */
@RestController
@RequestMapping("/api/analytics/daily/scheduler")
public class AnalyticsDailySchedulerController {

    private final AnalyticsDailyScheduler scheduler;

    public AnalyticsDailySchedulerController(AnalyticsDailyScheduler scheduler) {
        this.scheduler = scheduler;
    }

    /**
     * 手動觸發統計（指定日期）
     * POST /api/analytics/daily/scheduler/collect?date=2025-01-12
     */
    @PostMapping("/collect")
    public ResponseEntity<Map<String, Object>> manualCollect(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        try {
            scheduler.manualCollect(date);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "數據統計成功");
            response.put("date", date);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            // 印出完整的錯誤堆疊，方便除錯
            e.printStackTrace();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "數據統計失敗: " + e.getMessage());
            response.put("date", date);
            
            // 如果有原因，也加入回應
            if (e.getCause() != null && e.getCause().getMessage() != null) {
                response.put("cause", e.getCause().getMessage());
            }
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 手動觸發統計昨日數據
     * POST /api/analytics/daily/scheduler/collect-yesterday
     */
    @PostMapping("/collect-yesterday")
    public ResponseEntity<Map<String, Object>> collectYesterday() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        return manualCollect(yesterday);
    }

    /**
     * 手動觸發統計今日數據
     * POST /api/analytics/daily/scheduler/collect-today
     */
    @PostMapping("/collect-today")
    public ResponseEntity<Map<String, Object>> collectToday() {
        LocalDate today = LocalDate.now();
        return manualCollect(today);
    }

    /**
     * 批量補資料（指定日期範圍）
     * POST /api/analytics/daily/scheduler/batch-collect?startDate=2025-01-01&endDate=2025-01-07
     */
    @PostMapping("/batch-collect")
    public ResponseEntity<Map<String, Object>> batchCollect(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        Map<String, Object> response = new HashMap<>();
        int successCount = 0;
        int failCount = 0;

        try {
            LocalDate currentDate = startDate;
            while (!currentDate.isAfter(endDate)) {
                try {
                    scheduler.manualCollect(currentDate);
                    successCount++;
                } catch (Exception e) {
                    failCount++;
                    System.err.println("統計日期 " + currentDate + " 失敗: " + e.getMessage());
                }
                currentDate = currentDate.plusDays(1);
            }

            response.put("success", true);
            response.put("message", "批量統計完成");
            response.put("startDate", startDate);
            response.put("endDate", endDate);
            response.put("successCount", successCount);
            response.put("failCount", failCount);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "批量統計失敗: " + e.getMessage());
            response.put("successCount", successCount);
            response.put("failCount", failCount);
            
            return ResponseEntity.status(500).body(response);
        }
    }
}

