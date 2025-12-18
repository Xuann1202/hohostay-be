package tw.com.ispan.eeit.ho_back.wishlist;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class WishListServiceTest {
    @Autowired
    WishlistService wishlistService;

    @Test
    public void findByUserId() {
        Integer id = 11;
        System.out.println(wishlistService.findByUserId(id));
    }

    @Test
    public void getUserWishlist() {
        Integer id = 11;
        System.out.println(wishlistService.getUserWishlist(id, 0, 3).getPageable());
    }
}
