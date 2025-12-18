package tw.com.ispan.eeit.ho_back.booking;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import tw.com.ispan.eeit.ho_back.bookingInventory.BookingInventoryDto;

@SpringBootTest
public class BookingServiceTest {
    @Autowired
    BookingService bookingService;

    @Test
    public void createBooking() {
        BookingDto bookingDto = new BookingDto();

        BookingInventoryDto inventory1 = new BookingInventoryDto();
        inventory1.setInventoryId(1);
        inventory1.setNumber(1);

        BookingInventoryDto inventory2 = new BookingInventoryDto();
        inventory2.setInventoryId(2);
        inventory2.setNumber(1);

        List<BookingInventoryDto> inventories = new ArrayList<>();
        inventories.add(inventory1);
        inventories.add(inventory2);
        bookingDto.setBookingInventoriesDto(inventories);
        bookingDto.setUserId(3);
        bookingDto.setStartDate(LocalDate.of(2025, 11, 25));
        bookingDto.setEndDate(LocalDate.of(2025, 11, 26));
        bookingDto.setCouponSn(null);
        bookingDto.setLeadGuest(null);
        bookingDto.setRequest(null);
        System.out.println(bookingDto);
        bookingService.createBooking(bookingDto);
    }
}
