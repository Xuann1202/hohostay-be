package tw.com.ispan.eeit.ho_back.district;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class DistrictRepositoryTest {
    @Autowired
    DistrictRepository districtRepository;

    @Test
    public void findByHotelId() {
        Integer hotelId = 1;
        System.out.println(districtRepository.findByHotelId(hotelId));
    }
}
