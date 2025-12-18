package tw.com.ispan.eeit.ho_back.hotel;

import java.util.List;

import lombok.Data;
import tw.com.ispan.eeit.ho_back.room.RoomIdsDTO;

@Data
public class HotelRoomIdsDTO {
    private Integer hotelId;
    private String hotelName;
    private List<RoomIdsDTO> rooms;
}
