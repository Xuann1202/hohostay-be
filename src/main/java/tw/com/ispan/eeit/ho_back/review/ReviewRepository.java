package tw.com.ispan.eeit.ho_back.review;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Integer> {
        List<Review> findByBooking_BookingInventories_Inventory_Room_Hotel_Id(Integer hotelId);

        // 查詢多間飯店評論 - 修正版:加入 user 資料
        @Query("SELECT DISTINCT new tw.com.ispan.eeit.ho_back.review.HotelReviewDto(" +
                        "re.id, h.id, re.rating, re.comment, re.reply, re.createdDate, " +
                        "u.firstName, u.lastName, u.image, u.id) " +
                        "FROM Review re " +
                        "JOIN re.user u " + // Review 直接關聯 User
                        "JOIN re.booking b " +
                        "JOIN b.bookingInventories bi " +
                        "JOIN bi.inventory i " +
                        "JOIN i.room r " +
                        "JOIN r.hotel h " +
                        "WHERE h.id IN :hotelIds " +
                        "ORDER BY re.createdDate DESC")
        List<HotelReviewDto> findReviewsByHotelIds(List<Integer> hotelIds);

        // 查詢一間飯店評論
        @Query("SELECT DISTINCT new tw.com.ispan.eeit.ho_back.review.HotelReviewDto(" +
                        "re.id, h.id, re.rating, re.comment, re.reply, re.createdDate, " +
                        "u.firstName, u.lastName, u.image, u.id) " +
                        "FROM Review re " +
                        "JOIN re.user u " + // Review 直接關聯 User
                        "JOIN re.booking b " +
                        "JOIN b.bookingInventories bi " +
                        "JOIN bi.inventory i " +
                        "JOIN i.room r " +
                        "JOIN r.hotel h " +
                        "WHERE h.id = :hotelId " +
                        "ORDER BY re.createdDate DESC")
        List<HotelReviewDto> findReviewsByHotelId(Integer hotelId);

        // 依訂單ID查詢評論（使用 JOIN FETCH 立即載入關聯實體，避免序列化問題）
        @Query("SELECT r FROM Review r LEFT JOIN FETCH r.user LEFT JOIN FETCH r.booking WHERE r.booking.id = :bookingId")
        Review findByBookingId(Integer bookingId);

        // 依使用者ID查詢評論列表（使用 JOIN FETCH 立即載入關聯實體）
        @Query("SELECT r FROM Review r LEFT JOIN FETCH r.user LEFT JOIN FETCH r.booking WHERE r.user.id = :userId")
        List<Review> findByUserId(Integer userId);
}