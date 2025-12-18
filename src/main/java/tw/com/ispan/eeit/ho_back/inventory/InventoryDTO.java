package tw.com.ispan.eeit.ho_back.inventory;

import java.time.LocalDate;

import lombok.Data;
import tw.com.ispan.eeit.ho_back.room.RoomDTO;

@Data
public class InventoryDTO {
    private Integer id;
    private Integer stock;
    private Integer price;
    private LocalDate date;
    private RoomDTO room;

}