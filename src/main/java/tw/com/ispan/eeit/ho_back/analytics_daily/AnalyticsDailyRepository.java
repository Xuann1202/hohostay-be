package tw.com.ispan.eeit.ho_back.analytics_daily;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AnalyticsDailyRepository extends JpaRepository<AnalyticsDaily, Long> {

    /**
     * 根據日期查詢統計數據
     * 如果同一天有多筆資料，返回最新的一筆（按 id DESC 排序）
     */
    @Query(value = "SELECT TOP 1 * FROM analytics_daily WHERE dt = :date ORDER BY id DESC", nativeQuery = true)
    Optional<AnalyticsDaily> findByDate(@Param("date") LocalDate date);

    /**
     * 查詢指定日期範圍的統計數據（用於圖表）
     */
    @Query("SELECT a FROM AnalyticsDaily a WHERE a.date BETWEEN :startDate AND :endDate ORDER BY a.date ASC")
    List<AnalyticsDaily> findByDateBetween(@Param("startDate") LocalDate startDate, 
                                           @Param("endDate") LocalDate endDate);

    /**
     * 查詢最新的 N 天數據
     */
    @Query("SELECT a FROM AnalyticsDaily a ORDER BY a.date DESC")
    List<AnalyticsDaily> findTopNByOrderByDateDesc();

    /**
     * 檢查指定日期是否已有數據
     */
    boolean existsByDate(LocalDate date);
}

