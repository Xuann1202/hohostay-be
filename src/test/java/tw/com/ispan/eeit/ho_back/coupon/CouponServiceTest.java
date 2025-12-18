package tw.com.ispan.eeit.ho_back.coupon;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class CouponServiceTest {
    @Autowired
    CouponService couponService;

    @Test
    public void isCouponValid() {
        String sn = "NEWUSER100";
        couponService.couponValidate(sn);
        System.out.println("coupon sn :" + sn + "驗證完成");
    }
}
