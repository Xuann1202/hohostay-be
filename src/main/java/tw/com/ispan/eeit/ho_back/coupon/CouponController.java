package tw.com.ispan.eeit.ho_back.coupon;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 優惠券 API 控制器
 */
@RestController
@RequestMapping("/api/coupons")
public class CouponController {

    private final CouponService service;

    public CouponController(CouponService service) {
        this.service = service;
    }

    /**
     * 查詢所有優惠券
     * GET /api/coupons
     */
    @GetMapping
    public ResponseEntity<List<CouponDTO>> getAll() {
        List<CouponDTO> coupons = service.getAll();
        return ResponseEntity.ok(coupons);
    }

    /**
     * 根據 ID 查詢優惠券
     * GET /api/coupons/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<CouponDTO> getById(@PathVariable Integer id) {
        CouponDTO coupon = service.getById(id);
        if (coupon == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(coupon);
    }

    /**
     * 根據序號查詢優惠券
     * GET /api/coupons/sn/{sn}
     */
    @GetMapping("/sn/{sn}")
    public ResponseEntity<CouponDTO> getBySn(@PathVariable String sn) {
        CouponDTO coupon = service.getBySn(sn);
        if (coupon == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(coupon);
    }

    /**
     * 新增優惠券
     * POST /api/coupons
     */
    @PostMapping
    public ResponseEntity<?> create(@RequestBody CouponDTO dto) {
        try {
            CouponDTO created = service.create(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 更新優惠券
     * PUT /api/coupons/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @PathVariable Integer id,
            @RequestBody CouponDTO dto) {
        try {
            CouponDTO updated = service.update(id, dto);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 刪除優惠券
     * DELETE /api/coupons/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Integer id) {
        try {
            service.delete(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 檢查序號是否存在
     * GET /api/coupons/check-sn?sn=XXX
     */
    @GetMapping("/check-sn")
    public ResponseEntity<Map<String, Boolean>> checkSn(@RequestParam String sn) {
        boolean exists = service.existsBySn(sn);
        return ResponseEntity.ok(Map.of("exists", exists));
    }

    @GetMapping("/validate")
    public ResponseEntity<?> validateCoupon(String sn) {
        try {
            Coupon coupon = service.couponValidate(sn);
            return ResponseEntity.status(HttpStatus.OK).body(coupon);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/valid")
    public ResponseEntity<?> getValidCoupon() {
        List<ValidCouponDto> coupon = service.findByValid();
        return ResponseEntity.status(HttpStatus.OK).body(coupon);
    }
}