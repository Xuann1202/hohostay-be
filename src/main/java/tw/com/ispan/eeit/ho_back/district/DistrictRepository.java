package tw.com.ispan.eeit.ho_back.district;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import tw.com.ispan.eeit.ho_back.city.City;
import java.util.Optional;

@Repository
public interface DistrictRepository extends JpaRepository<District, Integer> {

    /**
     * 查詢所有行政區並預載入 City 關聯
     * 修改：使用 JOIN FETCH 避免 LAZY 載入問題
     */
    @Query("SELECT DISTINCT d FROM District d LEFT JOIN FETCH d.city")
    List<District> findAllWithCity();

    /**
     * 根據行政區名稱查詢
     * 
     * @param name 行政區名稱
     * @return 行政區（可能為空）
     */
    Optional<District> findByName(String name);

    /**
     * 根據城市查詢該城市的所有行政區
     * 
     * @param city 城市實體
     * @return 行政區列表
     */
    List<District> findByCity(City city);

    /**
     * 根據城市 ID 查詢該城市的所有行政區
     * 
     * @param cityId 城市 ID
     * @return 行政區列表
     */
    List<District> findByCityId(Integer cityId);

    /**
     * 根據城市和行政區名稱查詢
     * 用於檢查城市下是否已存在該行政區（避免重複建立衝突）
     * 
     * @param city 城市實體
     * @param name 行政區名稱
     * @return 行政區（可能為空）
     */
    Optional<District> findByCityAndName(City city, String name);

    // 根據hotel_id找對應的city,district
    @Query("SELECT new tw.com.ispan.eeit.ho_back.district.CityDistrictDto(c.id, d.id, c.name, d.name) " +
            "FROM Hotel h " +
            "JOIN h.district d " +
            "JOIN d.city c " +
            "WHERE h.id=:hotelId")
    public CityDistrictDto findByHotelId(Integer hotelId);

}
