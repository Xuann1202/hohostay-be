package tw.com.ispan.eeit.ho_back.city;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CityRepository extends JpaRepository<City, Integer> {

    /**
     * 根據城市名稱查詢
     * 
     * @param name 城市名稱
     * @return 城市（如果存在）
     */
    Optional<City> findByName(String name);
}
