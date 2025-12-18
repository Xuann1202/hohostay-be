package tw.com.ispan.eeit.ho_back.wishlist;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

@RestController
public class WishlistController {
    @Autowired
    WishlistService wishlistService;

    @PostMapping("/api/wishlist/{hotelId}")
    public ResponseEntity<?> addWishList(@PathVariable Integer hotelId, HttpServletRequest request) {
        try {
            Integer userId = (Integer) request.getAttribute("userId");
            wishlistService.addToWishList(userId, hotelId);
            return ResponseEntity.status(HttpStatus.CREATED).body("新增成功");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @DeleteMapping("/api/wishlist/{hotelId}")
    public ResponseEntity<?> getMethodName(@PathVariable Integer hotelId, HttpServletRequest request) {
        try {
            Integer userId = (Integer) request.getAttribute("userId");
            wishlistService.removeFromWishList(userId, hotelId);
            return ResponseEntity.status(HttpStatus.OK).body("刪除成功");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    // 單一飯店是否收藏
    @GetMapping("/api/wishlist/{hotelId}/status")
    public ResponseEntity<?> isFavorite(@PathVariable Integer hotelId, HttpServletRequest request) {
        try {
            Integer userId = (Integer) request.getAttribute("userId");
            Boolean isFavorite = wishlistService.isHotelLiked(userId, hotelId);
            return ResponseEntity.status(HttpStatus.OK).body(isFavorite);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }

    }

    // 查詢所有收藏
    @GetMapping("/api/wishlist")
    public ResponseEntity<?> getAll(HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        return ResponseEntity.ok(wishlistService.findByUserId(userId));
    }

    @GetMapping("/api/wishlist/user")
    public ResponseEntity<?> getUserWishlist(@RequestParam Integer page, @RequestParam Integer size,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        System.out.println(userId);
        Page<WishlistDto> wishlist = wishlistService.getUserWishlist(userId, page, size);
        return ResponseEntity.ok(wishlist);
    }

}
