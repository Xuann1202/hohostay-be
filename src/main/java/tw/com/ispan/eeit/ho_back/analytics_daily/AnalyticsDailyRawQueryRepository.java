package tw.com.ispan.eeit.ho_back.analytics_daily;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 每日數據統計原生 SQL 查詢 Repository
 * 使用原生 SQL 直接查詢資料庫，不依賴其他模組的 Entity
 */
@Repository
public interface AnalyticsDailyRawQueryRepository extends JpaRepository<AnalyticsDaily, Long> {

        /**
         * 統計截至指定日期的累積用戶總數
         * 查詢 dbo.user 表中 created_time <= 指定日期的累積用戶數
         * 
         * @param date 統計日期（包含當日）
         * @return 累積用戶總數
         */
        @Query(value = "SELECT COUNT(*) FROM dbo.[user] " +
                        "WHERE CAST(created_time AS DATE) <= :date", nativeQuery = true)
        Integer countTotalUsersByDate(@Param("date") LocalDate date);

        /**
         * 統計截至指定日期的累積飯店總數
         * 查詢 dbo.hotel 表中 created_time <= 指定日期且 business_status = 1 的累積飯店數
         * 
         * @param date 統計日期（包含當日）
         * @return 累積飯店總數
         */
        @Query(value = "SELECT COUNT(*) FROM dbo.[hotel] " +
                        "WHERE CAST(created_time AS DATE) <= :date " +
                        "AND business_status = 1", nativeQuery = true)
        Integer countTotalHotelsByDate(@Param("date") LocalDate date);

        /**
         * 統計指定日期當天的訂單數
         * 查詢 booking 表中 status=2（已付款）且 updated_time 為指定日期的訂單數量
         * 
         * @param date 統計日期
         * @return 訂單數
         */
        @Query(value = "SELECT COUNT(*) FROM dbo.[booking] " +
                        "WHERE status = 2 " +
                        "AND CAST(updated_time AS DATE) = :date", nativeQuery = true)
        Integer countOrdersByDate(@Param("date") LocalDate date);

        /**
         * 統計指定日期當天的平台成交額（GMV）
         * 查詢 booking 表中 status=2（已付款）且 updated_time 為指定日期的 total_price 總額
         * 
         * @param date 統計日期
         * @return 成交額
         */
        @Query(value = "SELECT ISNULL(SUM(total_price), 0) FROM dbo.[booking] " +
                        "WHERE status = 2 " +
                        "AND CAST(updated_time AS DATE) = :date", nativeQuery = true)
        BigDecimal sumGmvByDate(@Param("date") LocalDate date);

        /**
         * 統計指定日期當天的新增用戶數
         * 查詢 dbo.[user] 表中 created_time 為指定日期的用戶數量
         * 
         * @param date 統計日期
         * @return 新增用戶數
         */
        @Query(value = "SELECT COUNT(*) FROM dbo.[user] " +
                        "WHERE CAST(created_time AS DATE) = :date", nativeQuery = true)
        Integer countNewUsersByDate(@Param("date") LocalDate date);

        /**
         * 統計指定日期當天的新增評論數
         * 查詢 dbo.[review] 表中 created_date 為指定日期的評論數量
         * 
         * @param date 統計日期
         * @return 新增評論數
         */
        @Query(value = "SELECT COUNT(*) FROM dbo.[review] " +
                        "WHERE CAST(created_date AS DATE) = :date", nativeQuery = true)
        Integer countNewReviewsByDate(@Param("date") LocalDate date);

        /**
         * 統計指定日期當天的新增飯店數
         * 查詢 dbo.[hotel] 表中 created_time 為指定日期且 business_status = 1 的飯店數量
         * 
         * @param date 統計日期
         * @return 新增飯店數
         */
        @Query(value = "SELECT COUNT(*) FROM dbo.[hotel] " +
                        "WHERE CAST(created_time AS DATE) = :date " +
                        "AND business_status = 1", nativeQuery = true)
        Integer countNewHotelsByDate(@Param("date") LocalDate date);

        /**
         * 統計指定日期當天的新增客服案件數
         * 查詢 dbo.[support] 表中 created_time 為指定日期的客服案件數量
         * 
         * @param date 統計日期
         * @return 新增客服案件數
         */
        @Query(value = "SELECT COUNT(*) FROM dbo.[support] " +
                        "WHERE CAST(created_time AS DATE) = :date", nativeQuery = true)
        Integer countSupportCasesByDate(@Param("date") LocalDate date);

        /**
         * 查詢日期範圍內的訂單與成交額數據
         * 返回每天的訂單數和成交額
         * 
         * @param startDate 開始日期
         * @param endDate 結束日期
         * @return 包含日期、訂單數、成交額的結果列表
         */
        @Query(value = "SELECT CAST(updated_time AS DATE) AS date, " +
                        "COUNT(*) AS orders, " +
                        "ISNULL(SUM(total_price), 0) AS gmv " +
                        "FROM dbo.[booking] " +
                        "WHERE status = 2 " +
                        "AND CAST(updated_time AS DATE) BETWEEN :startDate AND :endDate " +
                        "GROUP BY CAST(updated_time AS DATE) " +
                        "ORDER BY CAST(updated_time AS DATE)", nativeQuery = true)
        List<Object[]> getOrderRevenueByDateRange(@Param("startDate") LocalDate startDate, 
                                                   @Param("endDate") LocalDate endDate);
}
