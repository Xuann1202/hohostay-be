package tw.com.ispan.eeit.ho_back.analytics_daily;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 每日營運數據收集服務
 * 負責從各個資料來源統計數據
 * 
 * 使用原生 SQL 查詢，不依賴其他模組的 Entity
 */
@Service
@Transactional(readOnly = true)
public class AnalyticsDailyCollectorService {

    private final AnalyticsDailyRawQueryRepository rawQueryRepository;

    public AnalyticsDailyCollectorService(AnalyticsDailyRawQueryRepository rawQueryRepository) {
        this.rawQueryRepository = rawQueryRepository;
    }

    /**
     * 收集指定日期的所有營運數據
     * 
     * @param date 統計日期
     * @return 統計結果
     */
    public AnalyticsDaily collectDataForDate(LocalDate date) {

        // 統計各項數據
        Integer totalUsers = countTotalUsers(date);
        Integer totalHotels = countTotalHotels(date);
        Integer totalOrders = countTotalOrders(date);
        BigDecimal gmvAmount = calculateGMV(date);

        return AnalyticsDaily.builder()
                .date(date)
                .totalUsers(totalUsers)
                .totalHotels(totalHotels)
                .totalOrders(totalOrders)
                .gmvAmount(gmvAmount)
                .build();
    }

    /**
     * 統計累積用戶總數
     * 使用原生 SQL 查詢截至指定日期的累積用戶總數
     * 
     * @param date 統計日期（包含當日）
     * @return 累積用戶總數
     */
    private Integer countTotalUsers(LocalDate date) {
        try {
            Integer count = rawQueryRepository.countTotalUsersByDate(date);
            return count != null ? count : 0;
        } catch (Exception e) {
            System.err.println("統計累積用戶總數失敗: " + e.getMessage());
            return 0;
        }
    }

    /**
     * 統計累積飯店總數
     * 使用原生 SQL 查詢截至指定日期且 business_status = 1 的累積飯店總數
     * 
     * @param date 統計日期（包含當日）
     * @return 累積飯店總數
     */
    private Integer countTotalHotels(LocalDate date) {
        try {
            Integer count = rawQueryRepository.countTotalHotelsByDate(date);
            return count != null ? count : 0;
        } catch (Exception e) {
            System.err.println("統計累積飯店總數失敗: " + e.getMessage());
            return 0;
        }
    }

    /**
     * 統計當日訂單數
     * 使用原生 SQL 查詢指定日期當天的訂單數
     * 
     * @param date 統計日期
     * @return 訂單數
     */
    private Integer countTotalOrders(LocalDate date) {
        try {
            Integer count = rawQueryRepository.countOrdersByDate(date);
            return count != null ? count : 0;
        } catch (Exception e) {
            System.err.println("統計訂單數失敗: " + e.getMessage());
            return 0;
        }
    }

    /**
     * 計算當日平台成交額（GMV）
     * 使用原生 SQL 計算指定日期當天已完成訂單的成交額
     * 
     * @param date 統計日期
     * @return 成交額
     */
    private BigDecimal calculateGMV(LocalDate date) {
        try {
            BigDecimal amount = rawQueryRepository.sumGmvByDate(date);
            return amount != null ? amount : BigDecimal.ZERO;
        } catch (Exception e) {
            System.err.println("統計平台成交額失敗: " + e.getMessage());
            return BigDecimal.ZERO;
        }
    }

}
