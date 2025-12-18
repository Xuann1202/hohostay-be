package tw.com.ispan.eeit.ho_back.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RoomStockDto {
    private Integer rommId;
    private Integer stock;
}
