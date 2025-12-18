package tw.com.ispan.eeit.ho_back.inventory;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import tw.com.ispan.eeit.ho_back.inventory.dto.HotelQueryDto;
import tw.com.ispan.eeit.ho_back.inventory.dto.RoomPriceDto;
import tw.com.ispan.eeit.ho_back.inventory.dto.RoomStockDto;

public interface InventoryRepository extends JpaRepository<Inventory, Integer>, InventoryDAO {

        // 根據庫存、關鍵字、日期、人數、價格、飯店型態篩選
        @Query("SELECT DISTINCT h.id " +
                        "FROM Hotel h " +
                        "JOIN h.rooms r " +
                        "JOIN r.inventories i " +
                        "LEFT JOIN h.hotelType ht " +
                        "LEFT JOIN h.district d " +
                        "LEFT JOIN d.city c " +
                        "WHERE i.stock > 0 " +
                        // "AND h.status = true " +
                        "AND (h.name LIKE %:#{#query.keyword}% OR " +
                        "     c.name LIKE %:#{#query.keyword}% OR " +
                        "     d.name LIKE %:#{#query.keyword}%) " +
                        "AND i.date >= :#{#query.checkInDate} " +
                        "AND i.date < :#{#query.checkOutDate} " +
                        "AND r.maxOccupancy >= :#{#query.guestNumber} " +
                        // 新增
                        "AND (:#{#query.starRating} IS NULL OR h.starRating >= :#{#query.starRating})  " +
                        "AND (:#{#query.minPrice} IS NULL OR i.price >= :#{#query.minPrice}) " +
                        "AND (:#{#query.maxPrice} IS NULL OR i.price <= :#{#query.maxPrice}) " +
                        "AND (:#{#query.hotelTypes} IS NULL OR ht.id IN :#{#query.hotelTypes}) " +
                        "GROUP BY h.id " +
                        "HAVING COUNT(DISTINCT i.date) = :#{#query.night}")
        List<Integer> findAvailableHotelIds(HotelQueryDto query);

        // 設施篩選
        @Query("SELECT DISTINCT h.id " +
                        "FROM Hotel h " +
                        "JOIN h.hotelFacilities hf " +
                        "JOIN hf.facility f " +
                        "WHERE h.id IN :hotelIds " +
                        "AND (f.id IN :#{#query.facilities}) " +
                        "GROUP BY h.id " +
                        "HAVING (COUNT(DISTINCT f.id) = :#{#query.facilityCount})")
        List<Integer> filterHotelsByFacilities(List<Integer> hotelIds,
                        HotelQueryDto query);

        // 取得每個飯店中每個房型價格
        @Query("SELECT new tw.com.ispan.eeit.ho_back.inventory.dto.RoomPriceDto(h.id, r.id, SUM(i.price), rt.name, bt.name, rb.bedNumber, r.name, r.maxOccupancy, r.size) "
                        +
                        "FROM Inventory i " +
                        "JOIN i.room r " +
                        "JOIN r.hotel h " +
                        "JOIN r.roomTypeBedType rb " +
                        "JOIN rb.roomType rt " +
                        "JOIN rb.bedType bt " +
                        "WHERE h.id IN :hotelIds " +
                        "AND i.stock > 0 " +
                        "AND i.date >= :#{#hotelQuery.checkInDate} " +
                        "AND i.date < :#{#hotelQuery.checkOutDate} " +
                        "AND r.maxOccupancy >= :#{#hotelQuery.guestNumber} " +
                        "AND (:#{#hotelQuery.minPrice} IS NULL OR i.price >= :#{#hotelQuery.minPrice}) " +
                        "AND (:#{#hotelQuery.maxPrice} IS NULL OR i.price <= :#{#hotelQuery.maxPrice}) " +
                        "GROUP BY h.id, r.id, rt.name, bt.name, rb.bedNumber, r.name, r.maxOccupancy, r.size " +
                        "HAVING COUNT(i.date) = :#{#hotelQuery.night}")
        List<RoomPriceDto> findRoomPriceByHotels(List<Integer> hotelIds, HotelQueryDto hotelQuery);

        // 取得某間飯店每個房型價格
        @Query("SELECT new tw.com.ispan.eeit.ho_back.inventory.dto.RoomPriceDto(h.id, r.id, SUM(i.price), rt.name, bt.name, rb.bedNumber, r.name, r.maxOccupancy, r.size) "
                        +
                        "FROM Inventory i " +
                        "JOIN i.room r " +
                        "JOIN r.hotel h " +
                        "JOIN r.roomTypeBedType rb " +
                        "JOIN rb.roomType rt " +
                        "JOIN rb.bedType bt " +
                        "WHERE h.id = :hotelId " +
                        "AND i.stock > 0 " +
                        "AND i.date >= :#{#hotelQuery.checkInDate} " +
                        "AND i.date < :#{#hotelQuery.checkOutDate} " +
                        "AND r.maxOccupancy >= :#{#hotelQuery.guestNumber} " +
                        "AND (:#{#hotelQuery.minPrice} IS NULL OR i.price >= :#{#hotelQuery.minPrice}) " +
                        "AND (:#{#hotelQuery.maxPrice} IS NULL OR i.price <= :#{#hotelQuery.maxPrice}) " +
                        "GROUP BY h.id, r.id, rt.name, bt.name, rb.bedNumber, r.name, r.maxOccupancy, r.size " +
                        "HAVING COUNT(i.date) = :#{#hotelQuery.night}")
        List<RoomPriceDto> findRoomPriceByHotel(Integer hotelId, HotelQueryDto hotelQuery);

        // 根據room_id及篩選日期找最小庫存
        @Query("SELECT new tw.com.ispan.eeit.ho_back.inventory.dto.RoomStockDto(i.room.id, Min(i.stock))" +
                        "FROM Inventory i " +
                        "WHERE i.room.id IN :roomIds " +
                        "AND i.date >= :checkInDate " +
                        "AND i.date < :checkOutDate " +
                        "GROUP BY i.room.id")
        List<RoomStockDto> findMinStockByRoomIds(List<Integer> roomIds, LocalDate checkInDate, LocalDate checkOutDate);

        // 用roomId找庫存
        @Query("SELECT i.id FROM Inventory i " +
                        "WHERE i.room.id = :roomId " +
                        "AND i.date >= :#{#query.checkInDate} " +
                        "AND i.date < :#{#query.checkOutDate} ")
        List<Integer> findByroomIdAndDate(Integer roomId, HotelQueryDto query);

        // 減庫存
        @Modifying
        @Query("UPDATE Inventory i SET i.stock = i.stock - :quantityToDecrement WHERE i.id = :inventoryId AND i.stock >= :quantityToDecrement")
        void decrementStockByQuantity(Integer inventoryId, Integer quantityToDecrement);

        // ✅ 改為 LocalDate
        Optional<Inventory> findByRoom_IdAndDate(Integer roomId, LocalDate date);

        // ✅ 已經是 LocalDate，保持不變
        List<Inventory> findByDateBetween(LocalDate start, LocalDate end);

        // ✅ 使用 JPA Query Methods
        Page<Inventory> findByRoom_Hotel_User_Id(
                        Integer userId,
                        Pageable pageable);

        // ✅ 使用 @Query - 只查詢今天之後的資料
        @Query("SELECT i FROM Inventory i " +
                        "JOIN i.room r " +
                        "JOIN r.hotel h " +
                        "WHERE h.user.id = :userId " +
                        "AND i.date >= CURRENT_DATE") // ✅ 只顯示今天及之後的日期
        Page<Inventory> findByUserId(
                        @Param("userId") Integer userId,
                        Pageable pageable);

        // ✅ 計算總數 - 只計算今天之後的
        @Query("SELECT COUNT(i.id) FROM Inventory i " +
                        "JOIN i.room r " +
                        "JOIN r.hotel h " +
                        "WHERE h.user.id = :userId " +
                        "AND i.date >= CURRENT_DATE") // ✅ 只計算今天及之後的
        Long countByUserId(@Param("userId") Integer userId);

}
