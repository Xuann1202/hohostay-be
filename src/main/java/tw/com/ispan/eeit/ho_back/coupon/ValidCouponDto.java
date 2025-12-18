package tw.com.ispan.eeit.ho_back.coupon;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

@Data
public class ValidCouponDto {
    private Integer id;
    private String name;
    private String sn;
    private Integer minimum;
    private Integer discount;
    private Integer useCount;
    private Integer takeCount;
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Taipei")
    private LocalDateTime startTime;
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Taipei")
    private LocalDateTime endTime;
    private LocalDateTime createTime;
    private Byte valid;
}
