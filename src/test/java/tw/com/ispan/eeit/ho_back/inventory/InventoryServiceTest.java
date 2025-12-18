package tw.com.ispan.eeit.ho_back.inventory;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import jakarta.transaction.Transactional;
import tw.com.ispan.eeit.ho_back.inventory.dto.HotelQueryDto;

@SpringBootTest
public class InventoryServiceTest {
    @Autowired
    InventoryService inventoryService;

    @Test
    @Transactional
    public void findAvailableHotelIds() {
        HotelQueryDto hotelQueryDto = new HotelQueryDto();
        hotelQueryDto.setKeyword("臺北");
        hotelQueryDto.setCheckInDate(LocalDate.of(2025, 11, 20));
        hotelQueryDto.setCheckOutDate(LocalDate.of(2025, 11, 22));
        // hotelQueryDto.setNight();
        hotelQueryDto.setGuestNumber(2);
        // hotelQueryDto.setMinPrice(2000);
        // hotelQueryDto.setMaxPrice(4500);
        // hotelQueryDto.setHotelType(2);
        List<Integer> hotelIds = inventoryService.findAvailableHotelIds(hotelQueryDto);
        System.out.println(hotelIds);
        System.out.println(hotelQueryDto.getNight());
    }

    @Test
    @Transactional
    public void filterHotelsByFacilities() {
        HotelQueryDto hotelQueryDto = new HotelQueryDto();
        hotelQueryDto.setKeyword("台北");
        hotelQueryDto.setCheckInDate(LocalDate.of(2025, 11, 18));
        hotelQueryDto.setCheckOutDate(LocalDate.of(2025, 11, 19));
        // hotelQueryDto.setNight();
        hotelQueryDto.setGuestNumber(2);
        // hotelQueryDto.setMinPrice(2000);
        // hotelQueryDto.setMaxPrice(4500);
        // hotelQueryDto.setHotelType(2);
        List<Integer> facilityIds = new ArrayList<>();
        facilityIds.add(1);
        hotelQueryDto.setFacilities(facilityIds);
        System.out.println(hotelQueryDto.getFacilityCount());
        List<Integer> hotelIds = inventoryService.filterHotelsByFacilities(hotelQueryDto);
        System.out.println(hotelIds);
    }

    @Test
    @Transactional
    public void findRoomPriceByHotel() {
        HotelQueryDto hotelQueryDto = new HotelQueryDto();
        hotelQueryDto.setKeyword("臺北");
        hotelQueryDto.setCheckInDate(LocalDate.of(2025, 11, 20));
        hotelQueryDto.setCheckOutDate(LocalDate.of(2025, 11, 22));
        // hotelQueryDto.setNight();
        hotelQueryDto.setGuestNumber(2);
        System.out.println(inventoryService.findRoomPriceByHotel(hotelQueryDto));
    }

    @Test
    @Transactional
    public void searchReuslt() {
        HotelQueryDto hotelQueryDto = new HotelQueryDto();
        hotelQueryDto.setKeyword("臺北");
        hotelQueryDto.setCheckInDate(LocalDate.of(2025, 11, 25));
        hotelQueryDto.setCheckOutDate(LocalDate.of(2025, 11, 26));
        hotelQueryDto.setGuestNumber(2);
        hotelQueryDto.setSortBy("price");
        hotelQueryDto.setSortOrder("desc");
        hotelQueryDto.setPage(0);
        hotelQueryDto.setSize(1);
        // List<Integer> facilityIds = new ArrayList<>();
        // facilityIds.add(1);
        // hotelQueryDto.setFacilities(facilityIds);
        // hotelQueryDto.setFacilityCount(facilityIds.size());
        System.out.println(inventoryService.searchHotel(hotelQueryDto));

    }
}
