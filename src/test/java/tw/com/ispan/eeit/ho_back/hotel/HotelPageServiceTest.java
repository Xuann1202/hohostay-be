package tw.com.ispan.eeit.ho_back.hotel;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import tw.com.ispan.eeit.ho_back.inventory.dto.HotelQueryDto;

@SpringBootTest
public class HotelPageServiceTest {
    @Autowired
    HotelPageService hotelPageService;

    @Test
    public void hotelPageInfo() {
        Integer hotelId = 1;
        HotelQueryDto query = new HotelQueryDto();
        query.setCheckInDate(LocalDate.of(2025, 11, 20));
        query.setCheckOutDate(LocalDate.of(2025, 11, 22));
        query.setGuestNumber(2);
        System.out.println(hotelPageService.hotelPageInfo(hotelId, query));
    }
}
