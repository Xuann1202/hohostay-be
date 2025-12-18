package tw.com.ispan.eeit.ho_back.hotel;

import java.math.BigDecimal;
import java.time.LocalDate;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import jakarta.transaction.Transactional;

@SpringBootTest
public class HotelRepositoryTest {
    @Autowired
    HotelRepository hotelRepository;

    @Test
    @Transactional
    public void findhotelDetailHotelIds() {
        List<Integer> hotelIds = new ArrayList<>();
        hotelIds.add(1);
        System.out.println(hotelRepository.findHotelDetail(hotelIds));
    }

    @Test
    @Transactional
    public void findHotelInfo() {
        Integer hotelId = 1;
        System.out.println(hotelRepository.findHotelInfoByHotelId(hotelId));
    }

    @Test
    @Transactional
    public void findHotelByCity() {
        String cityName = "臺北";
        Pageable pageable = PageRequest.of(0, 5);
        System.out.println(hotelRepository.findHotelByCity(cityName, pageable));
    }

    @Test
    public void findByUserFavorite() {
        Integer userId = 11;
        Pageable pageable = PageRequest.of(0, 1);
        System.out.println(hotelRepository.findByUserFavorite(userId, pageable));
    }

    @Transactional
    @Test
    public void findByLonLat() {
        BigDecimal longitude = BigDecimal.valueOf(121.5319);
        BigDecimal latitude = BigDecimal.valueOf(25.0478);
        BigDecimal r = BigDecimal.valueOf(10);
        LocalDate checkInTime = LocalDate.of(2025, 11, 26);
        LocalDate checkOutTime = LocalDate.of(2025, 11, 27);
        Integer guestNumber = 2;
        System.out
                .println(hotelRepository.findByLonLat(latitude, longitude, r, checkInTime, checkOutTime, guestNumber));
    }

}
