package tw.com.ispan.eeit.ho_back.booking;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Integer> {

    // ========= 原本的：coupon 相關 =========

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.couponId = :couponId AND b.status IN (2, 4)")
    Long countCompletedBookingsByCouponId(@Param("couponId") Integer couponId);

    @Query("SELECT b FROM Booking b WHERE b.couponId = :couponId AND b.status IN (2, 4)")
    List<Booking> findCompletedBookingsByCouponId(@Param("couponId") Integer couponId);

    // ========= 新增：會員查詢訂單 =========

    // 依 userId 查詢訂單列表（使用 JOIN FETCH 加載關聯實體）
    @Query("SELECT DISTINCT b FROM Booking b " +
           "LEFT JOIN FETCH b.bookingInventories bi " +
           "LEFT JOIN FETCH bi.inventory inv " +
           "LEFT JOIN FETCH inv.room r " +
           "LEFT JOIN FETCH r.hotel h " +
           "WHERE b.user.id = :userId " +
           "ORDER BY b.bookingDate DESC")
    List<Booking> findByUser_IdOrderByBookingDateDesc(@Param("userId") Integer userId);

    // 依訂單 id + userId 查詢（使用 JOIN FETCH 加載關聯實體）
    @Query("SELECT DISTINCT b FROM Booking b " +
           "LEFT JOIN FETCH b.bookingInventories bi " +
           "LEFT JOIN FETCH bi.inventory inv " +
           "LEFT JOIN FETCH inv.room r " +
           "LEFT JOIN FETCH r.hotel h " +
           "WHERE b.id = :id AND b.user.id = :userId")
    Optional<Booking> findByIdAndUser_Id(@Param("id") Integer id, @Param("userId") Integer userId);

    // ========= 飯店業者查詢訂單 =========
    // 這個要看你 Booking 裡有沒有對應欄位

    // 案例 1：如果 Booking 有 private Integer hotelOwnerId;
    // List<Booking> findByHotelOwnerId(Integer hotelOwnerId);

    // 案例 2：如果 Booking 是這種：Booking -> Hotel hotel -> User owner
    // @Query("SELECT b FROM Booking b WHERE b.hotel.owner.id = :ownerId")
    // List<Booking> findByHotelOwnerId(@Param("ownerId") Integer ownerId);
}
