package tw.com.ispan.eeit.ho_back.coupon;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tw.com.ispan.eeit.ho_back.booking.BookingRepository;

/**
 * 優惠券業務邏輯層
 */
@Service
@Transactional
public class CouponService {

    private final CouponRepository repository;
    private final BookingRepository bookingRepository;
    @Autowired
    ModelMapper modelMapper;

    public CouponService(CouponRepository repository, BookingRepository bookingRepository) {
        this.repository = repository;
        this.bookingRepository = bookingRepository;
    }

    /**
     * 查詢所有優惠券
     * 如果優惠券已過期（結束時間已過），自動將 valid 設為 0
     * 默認按建立時間降序、ID降序排序
     */
    public List<CouponDTO> getAll() {
        LocalDateTime now = LocalDateTime.now();
        List<Coupon> coupons = repository.findAllOrderByCreateTimeDescAndIdDesc();

        // 檢查並更新已過期的優惠券
        for (Coupon coupon : coupons) {
            // 如果設定了結束時間且當前時間已超過結束時間，且 valid 為 1，則設為 0
            if (coupon.getEndTime() != null
                    && now.isAfter(coupon.getEndTime())
                    && coupon.getValid() != null
                    && coupon.getValid() == 1) {
                coupon.setValid((byte) 0);
                repository.save(coupon);
            }
        }

        return coupons.stream()
                .map(CouponDTO::from)
                .collect(Collectors.toList());
    }

    /**
     * 根據 ID 查詢優惠券
     * 如果優惠券已過期（結束時間已過），自動將 valid 設為 0
     */
    public CouponDTO getById(Integer id) {
        Optional<Coupon> couponOpt = repository.findById(id);
        if (couponOpt.isEmpty()) {
            return null;
        }

        Coupon coupon = couponOpt.get();
        LocalDateTime now = LocalDateTime.now();

        // 如果設定了結束時間且當前時間已超過結束時間，且 valid 為 1，則設為 0
        if (coupon.getEndTime() != null
                && now.isAfter(coupon.getEndTime())
                && coupon.getValid() != null
                && coupon.getValid() == 1) {
            coupon.setValid((byte) 0);
            coupon = repository.save(coupon);
        }

        return CouponDTO.from(coupon);
    }

    /**
     * 根據序號查詢優惠券
     * 如果優惠券已過期（結束時間已過），自動將 valid 設為 0
     */
    public CouponDTO getBySn(String sn) {
        Optional<Coupon> couponOpt = repository.findBySn(sn);
        if (couponOpt.isEmpty()) {
            return null;
        }

        Coupon coupon = couponOpt.get();
        LocalDateTime now = LocalDateTime.now();

        // 如果設定了結束時間且當前時間已超過結束時間，且 valid 為 1，則設為 0
        if (coupon.getEndTime() != null
                && now.isAfter(coupon.getEndTime())
                && coupon.getValid() != null
                && coupon.getValid() == 1) {
            coupon.setValid((byte) 0);
            coupon = repository.save(coupon);
        }

        return CouponDTO.from(coupon);
    }

    /**
     * 新增優惠券
     */
    public CouponDTO create(CouponDTO dto) {
        // 檢查序號是否已存在
        if (dto.getSn() != null && repository.existsBySn(dto.getSn())) {
            throw new RuntimeException("優惠券序號已存在: " + dto.getSn());
        }

        Coupon coupon = dto.toEntity();
        Coupon saved = repository.save(coupon);
        return CouponDTO.from(saved);
    }

    /**
     * 更新優惠券
     */
    public CouponDTO update(Integer id, CouponDTO dto) {
        Optional<Coupon> existingOpt = repository.findById(id);
        if (existingOpt.isEmpty()) {
            throw new RuntimeException("優惠券不存在，ID: " + id);
        }

        Coupon existing = existingOpt.get();

        // 如果序號有變更，檢查新序號是否已被使用
        if (dto.getSn() != null && !dto.getSn().equals(existing.getSn())) {
            if (repository.existsBySn(dto.getSn())) {
                throw new RuntimeException("優惠券序號已存在: " + dto.getSn());
            }
        }

        // 更新欄位
        if (dto.getName() != null) {
            existing.setName(dto.getName());
        }
        if (dto.getSn() != null) {
            existing.setSn(dto.getSn());
        }
        if (dto.getMinimum() != null) {
            existing.setMinimum(dto.getMinimum());
        }
        if (dto.getDiscount() != null) {
            existing.setDiscount(dto.getDiscount());
        }
        // 注意：use_count 應從 booking table 自動計算，不建議手動更新
        // 如果提供了 use_count，會自動重新計算以確保資料一致性
        if (dto.getUseCount() != null) {
            // 重新計算 use_count 以確保與實際訂單數量一致
            Long actualUseCount = bookingRepository.countCompletedBookingsByCouponId(id);
            existing.setUseCount(actualUseCount != null ? actualUseCount.intValue() : 0);
        }
        if (dto.getTakeCount() != null) {
            existing.setTakeCount(dto.getTakeCount());
        }
        if (dto.getStartTime() != null) {
            existing.setStartTime(dto.getStartTime());
        }
        if (dto.getEndTime() != null) {
            existing.setEndTime(dto.getEndTime());
        }
        if (dto.getValid() != null) {
            existing.setValid(dto.getValid());
        }

        Coupon updated = repository.save(existing);
        return CouponDTO.from(updated);
    }

    /**
     * 刪除優惠券
     */
    public void delete(Integer id) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("優惠券不存在，ID: " + id);
        }
        repository.deleteById(id);
    }

    /**
     * 檢查序號是否存在
     */
    public boolean existsBySn(String sn) {
        return repository.existsBySn(sn);
    }

    /**
     * 增加優惠券的使用次數（當訂單狀態變更為已付款或完成時調用）
     * 
     * @param couponId 優惠券ID
     */
    @Transactional
    public void incrementUseCount(Integer couponId) {
        Optional<Coupon> couponOpt = repository.findById(couponId);
        if (couponOpt.isPresent()) {
            Coupon coupon = couponOpt.get();
            // 重新計算 use_count 以確保資料一致性
            Long actualUseCount = bookingRepository.countCompletedBookingsByCouponId(couponId);
            coupon.setUseCount(actualUseCount != null ? actualUseCount.intValue() : 0);
            repository.save(coupon);
        }
    }

    /**
     * 根據 booking table 自動更新所有優惠券的使用次數
     * 
     * 計算邏輯：
     * 1. 查詢所有 coupon
     * 2. 對每個 coupon，統計 booking table 中 status=2(已付款) 或 status=4(完成) 的訂單數量
     * 3. 更新對應 coupon 的 use_count 欄位
     * 
     * 建議使用場景：
     * - 定時任務/排程，定期同步更新所有 coupon 的 use_count
     * - 資料修復或初始化時使用
     */
    @Transactional
    public void updateUseCountFromBookings() {
        List<Coupon> coupons = repository.findAll();
        for (Coupon coupon : coupons) {
            Long useCount = bookingRepository.countCompletedBookingsByCouponId(coupon.getId());
            coupon.setUseCount(useCount != null ? useCount.intValue() : 0);
            repository.save(coupon);
        }
    }

    public Coupon couponValidate(String sn) {
        if (sn == null || sn.length() == 0) {
            return null;
        }
        Coupon coupon = repository.findBySn(sn).orElseThrow(() -> new RuntimeException("優惠券不存在，sn: " + sn));

        // 檢查優惠券是否有效
        if (coupon.getValid() == null || coupon.getValid() != 1) {
            throw new RuntimeException("優惠券無效，sn: " + sn);
        }
        // 檢查優惠券是否在有效期間內
        LocalDateTime now = LocalDateTime.now();
        if (coupon.getStartTime() != null && now.isBefore(coupon.getStartTime())) {
            throw new RuntimeException("優惠券尚未生效，sn: " + sn);
        }
        if (coupon.getEndTime() != null && now.isAfter(coupon.getEndTime())) {
            throw new RuntimeException("優惠券已過期，sn: " + sn);
        }
        return coupon;
    }

    // 取出valid的優惠卷
    public List<ValidCouponDto> findByValid() {
        List<Coupon> coupons = repository.findByValid((byte) 1);
        List<ValidCouponDto> validCoupons = new ArrayList<>();
        for (Coupon coupon : coupons) {
            ValidCouponDto validCoupon = modelMapper.map(coupon, ValidCouponDto.class);
            validCoupons.add(validCoupon);
            // LocalDateTime startTime = coupon.getStartTime();
            // LocalDateTime endTime = coupon.getEndTime();
            // startTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            // endTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            // coupon.setStartTime(startTime);
            // coupon.setEndTime(endTime);
        }
        return validCoupons;
    }
}
