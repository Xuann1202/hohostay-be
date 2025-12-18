package tw.com.ispan.eeit.ho_back.facility;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FacilityRepository extends JpaRepository<Facility, Integer> {
    /**
     * 根據設施名稱查詢
     * 
     * @param name 設施名稱
     * @return 設施（如果存在）
     */
    Optional<Facility> findByName(String name);
}
