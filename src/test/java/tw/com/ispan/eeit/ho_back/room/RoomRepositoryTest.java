package tw.com.ispan.eeit.ho_back.room;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import tw.com.ispan.eeit.ho_back.inventory.dto.HotelQueryDto;

@SpringBootTest
public class RoomRepositoryTest {
    @Autowired
    RoomRepository roomRepository;

    @Test
    public void findByRoomIds() {
        List<Integer> roomIds = new ArrayList<>();
        roomIds.add(1);
        HotelQueryDto query = new HotelQueryDto();
        query.setCheckInDate(LocalDate.of(2025, 11, 21));
        query.setCheckOutDate(LocalDate.of(2025, 11, 23));
        System.out.println(roomRepository.findRoomInventoryByRoomIds(roomIds, query));

    }
}
