package tw.com.ispan.eeit.ho_back.hotelFacility;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import tw.com.ispan.eeit.ho_back.hotelfacility.HotelFacilityRepository;

@SpringBootTest
public class HotelFacilityRepositoryTest {
    @Autowired
    HotelFacilityRepository hotelFacilityRepository;

    @Test
    public void findFacilityByHotelId() {
        Integer hotelId = 1;
        System.out.println(hotelFacilityRepository.findFacilityByHotelId(hotelId));

    }
}
