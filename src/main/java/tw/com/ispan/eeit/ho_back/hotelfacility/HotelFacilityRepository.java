package tw.com.ispan.eeit.ho_back.hotelfacility;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface HotelFacilityRepository extends JpaRepository<HotelFacility, Integer> {

        @Query("SELECT new tw.com.ispan.eeit.ho_back.hotelfacility.HotelFacilityDto(h.id, f.name) " +
                        "FROM HotelFacility hf " +
                        "JOIN hf.facility f " +
                        "JOIN hf.hotel h " +
                        "WHERE h.id = :hotelId")
        public List<HotelFacilityDto> findFacilityByHotelId(Integer hotelId);

        /**
         * 查詢某飯店的所有設施關聯
         * 
         * @param hotelId 飯店 ID
         * @return 飯店設施關聯列表
         */
        List<HotelFacility> findByHotelId(Integer hotelId);

        /**
         * 批量查詢多個飯店的所有設施關聯
         * 
         * @param hotelIds 飯店 ID 列表
         * @return 飯店設施關聯列表
         */
        @Query("SELECT hf FROM HotelFacility hf " +
                        "LEFT JOIN FETCH hf.facility " +
                        "WHERE hf.hotel.id IN :hotelIds")
        List<HotelFacility> findByHotelIdIn(@Param("hotelIds") List<Integer> hotelIds);

        /**
         * 查詢某飯店的所有設施 ID
         * 
         * @param hotelId 飯店 ID
         * @return 設施 ID 列表
         */
        @Query("SELECT hf.facility.id FROM HotelFacility hf WHERE hf.hotel.id = :hotelId")
        List<Integer> findFacilityIdsByHotelId(@Param("hotelId") Integer hotelId);

        @Modifying
        @Query("DELETE FROM HotelFacility hf WHERE hf.hotel.id = :hotelId")
        void deleteByHotelId(@Param("hotelId") Integer hotelId);

        // 注意：這裡將方法名命名為 deleteByHotelId 依然可以，
        // 但因為有 @Query 存在，方法名不再重要，重要的是 JPQL 語句。
}
