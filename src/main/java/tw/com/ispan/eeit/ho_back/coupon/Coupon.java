package tw.com.ispan.eeit.ho_back.coupon;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 優惠券實體
 */
@Data
@Entity
@Table(name = "coupon")
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "name", length = 100)
    private String name;

    @Column(name = "sn", length = 50, unique = true)
    private String sn;

    @Column(name = "minimum")
    private Integer minimum;

    @Column(name = "discount")
    private Integer discount;

    @Column(name = "use_count", nullable = false)
    private Integer useCount = 0;

    @Column(name = "take_count", nullable = false)
    private Integer takeCount = 0;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @CreationTimestamp
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @Column(name = "valid")
    private Byte valid; // 1=有效, 0=無效
}
