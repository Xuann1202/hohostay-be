package tw.com.ispan.eeit.ho_back.coupon;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

/**
 * 優惠券資料存取層
 */
@Repository
public interface CouponRepository extends JpaRepository<Coupon, Integer> {
    /**
     * 根據優惠券序號查詢
     */
    Optional<Coupon> findBySn(String sn);

    /**
     * 檢查優惠券序號是否存在
     */
    boolean existsBySn(String sn);

    // 取出valid的優惠卷
    List<Coupon> findByValid(Byte valid);

    /**
     * 查詢所有優惠券，按建立時間降序、ID降序排序
     */
    @Query("SELECT c FROM Coupon c ORDER BY c.createTime DESC, c.id DESC")
    List<Coupon> findAllOrderByCreateTimeDescAndIdDesc();
}
