package tw.com.ispan.eeit.ho_back.analytics_daily;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 每日營運數據自動統計排程器
 */
@Component
public class AnalyticsDailyScheduler {

    private static final Logger logger = LoggerFactory.getLogger(AnalyticsDailyScheduler.class);

    private final AnalyticsDailyService service;
    private final AnalyticsDailyRepository repository;

    public AnalyticsDailyScheduler(AnalyticsDailyService service,
            AnalyticsDailyRepository repository) {
        this.service = service;
        this.repository = repository;
    }

    /**
     * 應用程式啟動時，自動補充缺失的數據
     * 檢查昨天是否有統計數據，如果沒有則自動統計
     */
    @PostConstruct
    public void checkAndFillMissingData() {
        logger.info("========================================");
        logger.info("應用程式啟動，檢查是否有缺失的統計數據...");
        logger.info("========================================");

        try {
            LocalDate yesterday = LocalDate.now().minusDays(1);

            // 檢查昨天的數據是否存在
            if (!repository.existsByDate(yesterday)) {
                logger.warn("⚠️ 偵測到昨日（{}）數據缺失，開始補充統計...", yesterday);

                AnalyticsDailyDTO data = service.collectAndSaveDailyData(yesterday);

                logger.info("✅ 成功補充昨日數據");
                logger.info("   - 日期: {}", yesterday);
                logger.info("   - 累積用戶: {}", data.getTotalUsers());
                logger.info("   - 累積飯店: {}", data.getTotalHotels());
                logger.info("   - 訂單數: {}", data.getTotalOrders());
                logger.info("   - 成交額: {}", data.getGmvAmount());
            } else {
                logger.info("✅ 昨日（{}）數據已存在，無需補充", yesterday);
            }

        } catch (Exception e) {
            logger.error("❌ 補充缺失數據時發生錯誤: {}", e.getMessage(), e);
        }

        logger.info("========================================");
        logger.info("數據檢查完成");
        logger.info("========================================\n");
    }

    /**
     * 每日凌晨 1:00 自動統計昨日數據
     * Cron 表達式：秒 分 時 日 月 星期
     * 0 0 1 * * ? = 每天凌晨 1:00:00
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void collectYesterdayData() {
        LocalDate yesterday = LocalDate.now().minusDays(1);

        logger.info("========================================");
        logger.info("開始執行每日數據統計排程");
        logger.info("統計日期: {}", yesterday);
        logger.info("========================================");

        try {
            // 檢查是否已有該日期的數據
            if (repository.existsByDate(yesterday)) {
                logger.info("日期 {} 的數據已存在，跳過收集（不覆蓋）", yesterday);
                return; // 如果已有資料，直接返回，不進行收集
            }

            // 收集並保存數據（只有在沒有資料時才執行）
            AnalyticsDailyDTO data = service.collectAndSaveDailyData(yesterday);

            logger.info("✅ 成功統計並保存日期 {} 的數據", yesterday);
            logger.info("   - 累積用戶: {}", data.getTotalUsers());
            logger.info("   - 累積飯店: {}", data.getTotalHotels());
            logger.info("   - 訂單數: {}", data.getTotalOrders());
            logger.info("   - 成交額: {}", data.getGmvAmount());

        } catch (Exception e) {
            logger.error("❌ 統計日期 {} 的數據時發生錯誤: {}", yesterday, e.getMessage(), e);
        }

        logger.info("========================================");
        logger.info("每日數據統計排程執行完成");
        logger.info("========================================\n");
    }

    /**
     * 每小時統計今日數據（實時更新）
     * Cron 表達式：0 0 * * * ? = 每小時整點執行
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void collectTodayData() {
        LocalDate today = LocalDate.now();

        logger.info("開始更新今日數據 ({})", today);

        try {
            AnalyticsDailyDTO data = service.collectAndSaveDailyData(today);

            logger.info("✅ 今日數據更新成功 - 訂單數: {}, 成交額: {}",
                    data.getTotalOrders(), data.getGmvAmount());

        } catch (Exception e) {
            logger.error("❌ 更新今日數據時發生錯誤: {}", e.getMessage(), e);
        }
    }

    /**
     * 手動觸發統計（用於測試或補資料）
     * 如果該日期已有資料，則不覆蓋
     */
    public void manualCollect(LocalDate date) {
        logger.info("手動觸發統計，日期: {}", date);

        try {
            // 檢查是否已有該日期的數據
            if (repository.existsByDate(date)) {
                logger.info("日期 {} 的數據已存在，跳過收集（不覆蓋）", date);
                AnalyticsDailyDTO existingData = service.getByDate(date);
                logger.info("現有數據: totalUsers={}, totalHotels={}, totalOrders={}, gmvAmount={}",
                        existingData.getTotalUsers(), existingData.getTotalHotels(), 
                        existingData.getTotalOrders(), existingData.getGmvAmount());
                return; // 如果已有資料，直接返回，不進行收集
            }

            logger.info("開始收集數據...");
            AnalyticsDailyDTO data = service.collectAndSaveDailyData(date);

            logger.info("數據收集完成: totalUsers={}, totalHotels={}, totalOrders={}, gmvAmount={}",
                    data.getTotalUsers(), data.getTotalHotels(), data.getTotalOrders(), data.getGmvAmount());
            logger.info("✅ 手動統計完成");

        } catch (Exception e) {
            logger.error("❌ 手動統計失敗，詳細錯誤: ", e);
            String detailedMessage = e.getMessage();
            if (e.getCause() != null) {
                detailedMessage += " (原因: " + e.getCause().getMessage() + ")";
            }
            throw new RuntimeException(detailedMessage, e);
        }
    }
}
