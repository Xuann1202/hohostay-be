package tw.com.ispan.eeit.ho_back.analytics_daily;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 每日營運分析數據實體
 */
@Entity
@Table(name = "analytics_daily")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalyticsDaily {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 統計日期
     */
    @Column(name = "dt", nullable = false)
    private LocalDate date;

    /**
     * 累積用戶總數
     */
    @Column(name = "total_users", nullable = false)
    private Integer totalUsers;

    /**
     * 累積飯店總數
     */
    @Column(name = "total_hotels", nullable = false)
    private Integer totalHotels;

    /**
     * 當日訂單數
     */
    @Column(name = "total_orders", nullable = false)
    @Builder.Default
    private Integer totalOrders = 0;

    /**
     * 當日平台成交額（GMV）
     */
    @Column(name = "gmv_amount", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal gmvAmount = BigDecimal.ZERO;
}
