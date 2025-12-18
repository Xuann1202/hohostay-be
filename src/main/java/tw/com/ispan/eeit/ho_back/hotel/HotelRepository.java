package tw.com.ispan.eeit.ho_back.hotel;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.jpa.repository.query.Procedure;

import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HotelRepository extends JpaRepository<Hotel, Integer> {

        /**
         * 根據業者 ID 查詢他上架的所有飯店
         * 使用 JOIN FETCH 預先加載關聯數據，避免延遲加載導致的並發修改異常（包括 district 的 city）
         * 
         * @param userId 業者的 User ID
         * @return 飯店列表
         */
        @Query("SELECT DISTINCT h FROM Hotel h " + "LEFT JOIN FETCH h.district d " + "LEFT JOIN FETCH d.city "
                        + "LEFT JOIN FETCH h.hotelType " + "WHERE h.userId = :userId")
        List<Hotel> findByUserId(@Param("userId") Integer userId);

        /**
         * 根據業者 ID 和地區篩選查詢飯店
         * 
         * @param userId     業者的 User ID
         * @param cityId     城市 ID（可選，為 null 時不篩選）
         * @param districtId 行政區 ID（可選，為 null 時不篩選）
         * @return 飯店列表
         */
        @Query("SELECT DISTINCT h FROM Hotel h " +
                        "LEFT JOIN FETCH h.district d " +
                        "LEFT JOIN FETCH d.city c " +
                        "LEFT JOIN FETCH h.hotelType " +
                        "WHERE h.userId = :userId " +
                        "AND (:cityId IS NULL OR c.id = :cityId) " +
                        "AND (:districtId IS NULL OR d.id = :districtId)")
        List<Hotel> findByUserIdAndLocation(
                        @Param("userId") Integer userId,
                        @Param("cityId") Integer cityId,
                        @Param("districtId") Integer districtId);

        /**
         * 根據業者 ID 和多條件篩選查詢飯店
         * 
         * @param userId         業者的 User ID
         * @param cityId         城市 ID（可選，為 null 時不篩選）
         * @param districtId     行政區 ID（可選，為 null 時不篩選）
         * @param businessStatus 營業狀態（可選，為 null 時不篩選）
         * @param hotelTypeId    飯店類型 ID（可選，為 null 時不篩選）
         * @return 飯店列表
         */
        @Query("SELECT DISTINCT h FROM Hotel h " +
                        "LEFT JOIN FETCH h.district d " +
                        "LEFT JOIN FETCH d.city c " +
                        "LEFT JOIN FETCH h.hotelType ht " +
                        "WHERE h.userId = :userId " +
                        "AND (:cityId IS NULL OR c.id = :cityId) " +
                        "AND (:districtId IS NULL OR d.id = :districtId) " +
                        "AND (:businessStatus IS NULL OR h.businessStatus = :businessStatus) " +
                        "AND (:hotelTypeId IS NULL OR ht.id = :hotelTypeId)")
        List<Hotel> findByUserIdWithFilters(
                        @Param("userId") Integer userId,
                        @Param("cityId") Integer cityId,
                        @Param("districtId") Integer districtId,
                        @Param("businessStatus") Boolean businessStatus,
                        @Param("hotelTypeId") Integer hotelTypeId);

        /**
         * 根據業者 ID 分頁查詢他上架的所有飯店
         * 使用 JOIN FETCH 預先加載關聯數據，避免延遲加載導致的並發修改異常（包括 district 的 city）
         * 
         * @param userId   業者的 User ID
         * @param pageable 分頁參數
         * @return 分頁的飯店列表
         */
        @Query("SELECT DISTINCT h FROM Hotel h " +
                        "LEFT JOIN FETCH h.district d " +
                        "LEFT JOIN FETCH d.city " +
                        "LEFT JOIN FETCH h.hotelType " +
                        "WHERE h.userId = :userId")
        Page<Hotel> findByUserId(@Param("userId") Integer userId, Pageable pageable);

        /**
         * 根據 ID 查詢飯店，預先加載基本關聯數據（包括 district 的 city）
         * 
         * @param hotelId 飯店 ID
         * @return 飯店（如果存在）
         */
        @Query("SELECT DISTINCT h FROM Hotel h " +
                        "LEFT JOIN FETCH h.district d " +
                        "LEFT JOIN FETCH d.city " +
                        "LEFT JOIN FETCH h.hotelType " +
                        "WHERE h.id = :hotelId")
        Optional<Hotel> findByIdWithAssociations(@Param("hotelId") Integer hotelId);

        // 取得多間飯店資訊
        @Query("SELECT new tw.com.ispan.eeit.ho_back.hotel.HotelDetailDto(h.id, h.name,h.starRating, d.name, c.name, h.address,h.longitude, h.latitude, h.description) "
                        +
                        "FROM Hotel h " +
                        "JOIN h.district d " +
                        "JOIN d.city c " +
                        "WHERE h.id IN :hotelIds ")
        public List<HotelDetailDto> findHotelDetail(List<Integer> hotelIds);

        // 取得一間飯店資訊
        @Query("SELECT new tw.com.ispan.eeit.ho_back.hotel.HotelDetailDto(h.id, h.name,h.starRating, d.name, c.name, h.address,h.longitude, h.latitude, h.description, h.checkInTime, h.checkOutTime) "
                        +
                        "FROM Hotel h " +
                        "JOIN h.district d " +
                        "JOIN d.city c " +
                        "WHERE h.id IN :hotelId ")
        public HotelDetailDto findHotelInfoByHotelId(Integer hotelId);

        // 根據縣市查詢飯店平均分數並排序
        @Query("SELECT new tw.com.ispan.eeit.ho_back.hotel.HotelDetailDto(" +
                        "h.id, h.name,h.starRating, d.name, c.name, h.address,h.longitude, h.latitude, h.description, AVG(re.rating)) "
                        +
                        "FROM Hotel h " +
                        "JOIN h.district d " +
                        "JOIN d.city c " +
                        "LEFT JOIN h.rooms r " +
                        "LEFT JOIN r.inventories i " +
                        "LEFT JOIN i.bookingInventories bi " +
                        "LEFT JOIN bi.booking b " +
                        "LEFT JOIN b.review re " +
                        "WHERE c.name LIKE CONCAT('%', :cityName, '%') " +
                        // "AND h.status = true " +
                        "GROUP BY h.id, h.name, d.name, c.name, h.address, h.longitude, h.latitude, h.description, h.starRating "
                        +
                        "ORDER BY AVG(re.rating) DESC")
        List<HotelDetailDto> findHotelByCity(@Param("cityName") String cityName, Pageable pageable);

        // 根據userId取得收藏
        @Query("SELECT h FROM Hotel h JOIN h.whosWishList u WHERE u.id = :userId")
        Page<Hotel> findByUserFavorite(@Param("userId") Integer userId, Pageable pageable);

        @Procedure(procedureName = "getNearbyHotelSummary")
        List<GetNearbyHotelProjection> findByLonLat(@Param("lat") BigDecimal latitude,
                        @Param("lon") BigDecimal longitude,
                        @Param("r") BigDecimal radius,
                        @Param("checkInDate") LocalDate checkInDate,
                        @Param("checkOutDate") LocalDate checkOutDate,
                        @Param("guestNumber") Integer guestNumber);
}
