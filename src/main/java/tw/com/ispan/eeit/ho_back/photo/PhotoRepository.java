package tw.com.ispan.eeit.ho_back.photo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PhotoRepository extends JpaRepository<Photo, Integer> {
    /**
     * 查詢某飯店的所有照片
     * 
     * @param hotelId 飯店 ID
     * @return 照片列表
     */
    List<Photo> findByHotelId(Integer hotelId);

    /**
     * 批量查詢多個飯店的所有照片
     * 
     * @param hotelIds 飯店 ID 列表
     * @return 照片列表
     */
    @Query("SELECT p FROM Photo p WHERE p.hotel.id IN :hotelIds")
    List<Photo> findByHotelIdIn(@Param("hotelIds") List<Integer> hotelIds);

    // // 批次刪除
    // @Modifying
    // @Query("DELETE FROM Photo p WHERE p.hotel.id = :hotelId")
    // void deleteByHotelId(@Param("hotelId") Integer hotelId);

    public Photo findFirstByHotelIdAndIsCoverTrue(Integer hotelId);

}
