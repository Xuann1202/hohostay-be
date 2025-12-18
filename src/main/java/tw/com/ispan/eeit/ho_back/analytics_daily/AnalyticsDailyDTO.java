package tw.com.ispan.eeit.ho_back.analytics_daily;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 每日營運分析數據傳輸物件
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalyticsDailyDTO {

    private Long id;
    private LocalDate date;
    private Integer totalUsers;
    private Integer totalHotels;
    private Integer totalOrders;
    private BigDecimal gmvAmount;

    /**
     * 從實體轉換為 DTO
     */
    public static AnalyticsDailyDTO from(AnalyticsDaily entity) {
        if (entity == null) {
            return null;
        }
        return AnalyticsDailyDTO.builder()
                .id(entity.getId())
                .date(entity.getDate())
                .totalUsers(entity.getTotalUsers())
                .totalHotels(entity.getTotalHotels())
                .totalOrders(entity.getTotalOrders())
                .gmvAmount(entity.getGmvAmount())
                .build();
    }

    /**
     * 轉換為實體
     */
    public AnalyticsDaily toEntity() {
        return AnalyticsDaily.builder()
                .id(this.id)
                .date(this.date)
                .totalUsers(this.totalUsers)
                .totalHotels(this.totalHotels)
                .totalOrders(this.totalOrders)
                .gmvAmount(this.gmvAmount)
                .build();
    }
}

