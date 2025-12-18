package tw.com.ispan.eeit.ho_back.hoteltype;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HotelTypeRepository extends JpaRepository<HotelType, Integer> {
    /**
     * 根據類型名稱查詢
     * 
     * @param type 類型名稱
     * @return 飯店類型（如果存在）
     */
    Optional<HotelType> findByType(String type);
}
