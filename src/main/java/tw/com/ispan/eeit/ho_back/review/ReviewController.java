package tw.com.ispan.eeit.ho_back.review;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    /**
     * 依訂單ID查詢評論
     * GET /api/reviews/booking/{bookingId}
     */
    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<?> getReviewByBookingId(@PathVariable Integer bookingId) {
        try {
            if (bookingId == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "訂單ID不能為空");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
            Review review = reviewService.getReviewByBookingId(bookingId);
            if (review == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "此訂單尚未有評論");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            
            // 使用 JOIN FETCH 查詢後，關聯實體已經載入，不需要手動觸發
            // 確保關聯實體不為 null（如果為 null，可能是查詢問題）
            if (review.getUser() == null || review.getBooking() == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "載入評論關聯資料失敗");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
            
            return ResponseEntity.ok(review);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "查詢評論失敗：" + e.getMessage());
            response.put("error", e.getClass().getSimpleName());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 創建評論
     * POST /api/reviews?userId={userId}
     */
    @PostMapping
    public ResponseEntity<?> createReview(
            @RequestParam Integer userId,
            @RequestHeader(value = "userId", required = false) Integer headerUserId,
            @RequestBody CreateReviewRequestDTO request) {
        try {
            // 優先使用 header 中的 userId，其次使用 query parameter
            Integer finalUserId = headerUserId != null ? headerUserId : userId;
            
            if (finalUserId == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "缺少 userId 參數");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            if (request.getBookingId() == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "缺少 bookingId");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            if (request.getRating() == null || request.getRating() < 1 || request.getRating() > 5) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "評分必須在 1-5 之間");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            if (request.getComment() == null || request.getComment().trim().isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "評論內容不可為空");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            Review review = reviewService.createReview(
                    finalUserId,
                    request.getBookingId(),
                    request.getRating(),
                    request.getComment().trim()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", review);
            response.put("message", "評論創建成功");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            e.printStackTrace();
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "創建評論失敗：" + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 更新評論
     * PUT /api/reviews/{reviewId}?userId={userId}
     */
    @PutMapping("/{reviewId}")
    public ResponseEntity<?> updateReview(
            @PathVariable Integer reviewId,
            @RequestParam Integer userId,
            @RequestHeader(value = "userId", required = false) Integer headerUserId,
            @RequestBody UpdateReviewRequestDTO request) {
        try {
            // 優先使用 header 中的 userId，其次使用 query parameter
            Integer finalUserId = headerUserId != null ? headerUserId : userId;
            
            if (finalUserId == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "缺少 userId 參數");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            if (request.getRating() == null || request.getRating() < 1 || request.getRating() > 5) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "評分必須在 1-5 之間");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            if (request.getComment() == null || request.getComment().trim().isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "評論內容不可為空");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            Review review = reviewService.updateReview(
                    reviewId,
                    finalUserId,
                    request.getRating(),
                    request.getComment().trim()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", review);
            response.put("message", "評論更新成功");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            e.printStackTrace();
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "更新評論失敗：" + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 刪除評論
     * DELETE /api/reviews/{reviewId}?userId={userId}
     */
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<?> deleteReview(
            @PathVariable Integer reviewId,
            @RequestParam Integer userId,
            @RequestHeader(value = "userId", required = false) Integer headerUserId) {
        try {
            // 優先使用 header 中的 userId，其次使用 query parameter
            Integer finalUserId = headerUserId != null ? headerUserId : userId;
            
            if (finalUserId == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "缺少 userId 參數");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            // TODO: 實作刪除邏輯（可能需要權限檢查）
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "刪除功能尚未實作");
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(response);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "刪除評論失敗：" + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 依使用者ID查詢評論列表
     * GET /api/reviews/user/{userId}
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getReviewsByUserId(@PathVariable Integer userId) {
        try {
            List<Review> reviews = reviewService.getReviewsByUserId(userId);
            return ResponseEntity.ok(reviews);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "查詢評論列表失敗：" + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 檢查是否可以評論
     * GET /api/reviews/can-review/{bookingId}?userId={userId}
     */
    @GetMapping("/can-review/{bookingId}")
    public ResponseEntity<?> canReviewBooking(
            @PathVariable Integer bookingId,
            @RequestParam Integer userId) {
        try {
            boolean canReview = reviewService.canReviewBooking(bookingId, userId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("canReview", canReview);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "檢查評論權限失敗：" + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}


