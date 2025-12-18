package tw.com.ispan.eeit.ho_back.coupon;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 優惠券資料傳輸物件
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponDTO {

    private Integer id;
    private String name;
    private String sn;
    private Integer minimum;
    private Integer discount;
    private Integer useCount;
    private Integer takeCount;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime createTime;
    private Byte valid;

    /**
     * 從實體轉換為 DTO
     */
    public static CouponDTO from(Coupon entity) {
        if (entity == null) {
            return null;
        }
        return CouponDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .sn(entity.getSn())
                .minimum(entity.getMinimum())
                .discount(entity.getDiscount())
                .useCount(entity.getUseCount())
                .takeCount(entity.getTakeCount())
                .startTime(entity.getStartTime())
                .endTime(entity.getEndTime())
                .createTime(entity.getCreateTime())
                .valid(entity.getValid())
                .build();
    }

    /**
     * 轉換為實體
     */
    public Coupon toEntity() {
        Coupon coupon = new Coupon();
        coupon.setId(this.id);
        coupon.setName(this.name);
        coupon.setSn(this.sn);
        coupon.setMinimum(this.minimum);
        coupon.setDiscount(this.discount);
        coupon.setUseCount(this.useCount != null ? this.useCount : 0);
        coupon.setTakeCount(this.takeCount != null ? this.takeCount : 0);
        coupon.setStartTime(this.startTime);
        coupon.setEndTime(this.endTime);
        coupon.setValid(this.valid);
        return coupon;
    }
}

