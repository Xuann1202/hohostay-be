package tw.com.ispan.eeit.ho_back.inventory;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import tw.com.ispan.eeit.ho_back.inventory.dto.HotelQueryDto;

@SpringBootTest
public class InventoryRepositoryTest {
    @Autowired
    InventoryRepository inventoryRepository;

    @Test
    public void findAvailableHotelIds() {
        HotelQueryDto hotelQueryDto = new HotelQueryDto();
        hotelQueryDto.setKeyword("臺北");
        hotelQueryDto.setCheckInDate(LocalDate.of(2025, 11, 20));
        hotelQueryDto.setCheckOutDate(LocalDate.of(2025, 11, 22));
        hotelQueryDto.setGuestNumber(2);
        // hotelQueryDto.setMinPrice(2000);
        // hotelQueryDto.setMaxPrice(4500);
        // List<Integer> hotelType = new ArrayList<>();
        // hotelType.add(2);
        // hotelQueryDto.setHotelType(hotelType);
        List<Integer> hotelIds = inventoryRepository.findAvailableHotelIds(hotelQueryDto);
        System.out.println(hotelIds);
        System.out.println(hotelQueryDto.getNight());
        System.out.println(inventoryRepository.findRoomPriceByHotels(hotelIds, hotelQueryDto));
    }

    @Test
    public void findRoomPriceByHotel() {
        Integer hotelId = 1;
        HotelQueryDto hotelQueryDto = new HotelQueryDto();
        hotelQueryDto.setKeyword("臺北");
        hotelQueryDto.setCheckInDate(LocalDate.of(2025, 11, 20));
        hotelQueryDto.setCheckOutDate(LocalDate.of(2025, 11, 22));
        hotelQueryDto.setGuestNumber(2);
        System.out.println(inventoryRepository.findRoomPriceByHotel(hotelId, hotelQueryDto));
    }

    @Test
    public void findMinStockByRoomIds() {
        List<Integer> roomIds = new ArrayList<>();
        roomIds.add(1);
        roomIds.add(2);
        LocalDate checkInDate = LocalDate.of(2025, 11, 19);
        LocalDate checkOutDate = LocalDate.of(2025, 11, 22);
        System.out.println(inventoryRepository.findMinStockByRoomIds(roomIds, checkInDate, checkOutDate));
    }

    @Test
    public void findByroomIdAndDate() {
        Integer roomId = 1;
        HotelQueryDto hotelQueryDto = new HotelQueryDto();
        hotelQueryDto.setKeyword("臺北");
        hotelQueryDto.setCheckInDate(LocalDate.of(2025, 11, 28));
        hotelQueryDto.setCheckOutDate(LocalDate.of(2025, 11, 29));
        System.out.println(inventoryRepository.findByroomIdAndDate(roomId, hotelQueryDto));
    }
}
