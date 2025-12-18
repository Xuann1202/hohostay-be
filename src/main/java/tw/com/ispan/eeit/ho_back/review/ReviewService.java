package tw.com.ispan.eeit.ho_back.review;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tw.com.ispan.eeit.ho_back.booking.Booking;
import tw.com.ispan.eeit.ho_back.booking.BookingRepository;
import tw.com.ispan.eeit.ho_back.hotel.Hotel;
import tw.com.ispan.eeit.ho_back.hotel.HotelRepository;
import tw.com.ispan.eeit.ho_back.user.User;
import tw.com.ispan.eeit.ho_back.user.UserRepository;

@Service
public class ReviewService {
    @Autowired
    private ReviewRepository reviewRepository;
    @Autowired
    private HotelRepository hotelRepository;
    @Autowired
    private BookingRepository bookingRepository;
    @Autowired
    private UserRepository userRepository;

    public List<Hotel> getHotelsByOwner(Integer userId) {
        return hotelRepository.findByUserId(userId); // ✅ 已存在的查詢
    }

    // 依飯店ID抓評論
    public List<Review> getReviewsByHotelId(Integer hotelId) {
        return reviewRepository.findByBooking_BookingInventories_Inventory_Room_Hotel_Id(hotelId);
    }

    // public Review updateReply(ReplyRequestDTO dto) {
    // Review review = reviewRepository.findById(dto.getId()).orElse(null);
    // if (review == null)
    // return null;

    // LocalDate today = LocalDate.now(); // 只取日期
    // review.setReply(dto.getReply());

    // if (review.getReplyCreatedDate() == null) {
    // review.setReplyCreatedDate(today);
    // }
    // review.setReplyUpdatedDate(today);

    // return reviewRepository.save(review);
    // }
    public Review updateReply(ReplyRequestDTO dto) {
        Review review = reviewRepository.findById(dto.getId())
                .orElseThrow(() -> new RuntimeException("找不到該評論"));
        if (review.getReply() == null) {
            // 新增回覆
            review.setReply(dto.getReply());
            review.setReplyCreatedDate(LocalDate.now());
        } else {
            // 修改回覆
            review.setReply(dto.getReply());
            review.setReplyUpdatedDate(LocalDate.now());
        }

        return reviewRepository.save(review);
    }

    /**
     * 依訂單ID查詢評論
     */
    public Review getReviewByBookingId(Integer bookingId) {
        return reviewRepository.findByBookingId(bookingId);
    }

    /**
     * 創建評論
     */
    @Transactional
    public Review createReview(Integer userId, Integer bookingId, Integer rating, String comment) {
        // 檢查訂單是否存在
        Optional<Booking> bookingOpt = bookingRepository.findById(bookingId);
        if (bookingOpt.isEmpty()) {
            throw new RuntimeException("訂單不存在");
        }
        Booking booking = bookingOpt.get();

        // 檢查使用者是否存在
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("使用者不存在");
        }
        User user = userOpt.get();

        // 檢查是否已經有評論
        Review existingReview = reviewRepository.findByBookingId(bookingId);
        if (existingReview != null) {
            // 如果評論存在，檢查評論內容是否為空（NULL 或空字串）
            String existingComment = existingReview.getComment();
            if (existingComment != null && !existingComment.trim().isEmpty()) {
                // 如果評論已有內容，不允許創建新評論
                throw new RuntimeException("此訂單已有評論，請使用更新功能");
            } else {
                // 如果評論存在但內容為空，直接更新這個空評論
                // 檢查使用者是否有權限修改此評論
                // 注意：需要確保 existingReview.getUser() 不為 null
                if (existingReview.getUser() == null || !existingReview.getUser().getId().equals(userId)) {
                    // 如果 user 關聯為 null，嘗試重新載入
                    // 或者檢查訂單的 user_id 是否匹配
                    // 為了安全起見，我們檢查訂單的 user_id
                    if (booking.getUser() == null || !booking.getUser().getId().equals(userId)) {
                        throw new RuntimeException("無權限修改此評論");
                    }
                    // 如果訂單的 user_id 匹配，更新評論的 user 關聯
                    if (existingReview.getUser() == null) {
                        existingReview.setUser(user);
                    }
                }

                // 驗證評分和評論內容
                if (rating == null || rating < 1 || rating > 5) {
                    throw new RuntimeException("評分必須在 1-5 之間");
                }

                if (comment == null || comment.trim().isEmpty()) {
                    throw new RuntimeException("評論內容不可為空");
                }

                // 更新空評論
                existingReview.setRating(rating);
                existingReview.setComment(comment.trim());
                existingReview.setIsEdited(true);
                existingReview.setUpdatedDate(LocalDate.now());

                return reviewRepository.save(existingReview);
            }
        }

        // 創建新評論
        Review review = new Review();
        review.setUser(user);
        review.setBooking(booking);
        review.setRating(rating);
        review.setComment(comment);
        review.setIsEdited(false);
        review.setIsVisible(true);
        review.setCreatedDate(LocalDate.now());
        review.setUpdatedDate(LocalDate.now());

        return reviewRepository.save(review);
    }

    /**
     * 更新評論
     * 注意：如果評論已有內容（非空白），不允許再修改
     */
    @Transactional
    public Review updateReview(Integer reviewId, Integer userId, Integer rating, String comment) {
        Optional<Review> reviewOpt = reviewRepository.findById(reviewId);
        if (reviewOpt.isEmpty()) {
            throw new RuntimeException("評論不存在");
        }
        Review review = reviewOpt.get();

        // 檢查使用者是否有權限修改此評論
        if (!review.getUser().getId().equals(userId)) {
            throw new RuntimeException("無權限修改此評論");
        }

        // 檢查評論是否已有內容（非空白），如果有則不允許修改
        if (review.getComment() != null && !review.getComment().trim().isEmpty()) {
            throw new RuntimeException("評論已提交，無法再修改");
        }

        // 驗證評分和評論內容
        if (rating == null || rating < 1 || rating > 5) {
            throw new RuntimeException("評分必須在 1-5 之間");
        }

        if (comment == null || comment.trim().isEmpty()) {
            throw new RuntimeException("評論內容不可為空");
        }

        review.setRating(rating);
        review.setComment(comment.trim());
        review.setIsEdited(true);
        review.setUpdatedDate(LocalDate.now());

        return reviewRepository.save(review);
    }

    /**
     * 依使用者ID查詢評論列表
     */
    public List<Review> getReviewsByUserId(Integer userId) {
        return reviewRepository.findByUserId(userId);
    }

    /**
     * 檢查使用者是否可以對訂單評論
     */
    public boolean canReviewBooking(Integer bookingId, Integer userId) {
        // 檢查是否已有評論
        Review existingReview = reviewRepository.findByBookingId(bookingId);
        if (existingReview != null) {
            return false; // 已有評論
        }

        // 檢查訂單是否存在且屬於該使用者
        Optional<Booking> bookingOpt = bookingRepository.findById(bookingId);
        if (bookingOpt.isEmpty()) {
            return false;
        }
        Booking booking = bookingOpt.get();

        // 檢查訂單是否屬於該使用者
        if (!booking.getUser().getId().equals(userId)) {
            return false;
        }

        // 檢查訂單狀態是否為已完成（status = 4）
        if (booking.getStatus() == null || booking.getStatus() != 4) {
            return false;
        }

        // 檢查是否超過退房時間
        if (booking.getEndDate() == null || !booking.getEndDate().isBefore(LocalDate.now())) {
            return false;
        }

        return true;
    }
}