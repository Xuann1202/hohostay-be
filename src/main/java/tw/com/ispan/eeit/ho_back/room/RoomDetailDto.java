package tw.com.ispan.eeit.ho_back.room;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import lombok.Data;

@Data
public class RoomDetailDto {
    private List<Integer> inventoryIds;
    private Integer roomId;
    private String name;
    private Integer maxOccupancy;
    private BigDecimal size;
    private String description;
    private Integer stock;
    private Long price;
    private LocalDate date;

    public RoomDetailDto(Integer roomId, String name, Integer maxOccupancy, BigDecimal size, String description,
            Long price) {
        this.roomId = roomId;
        this.name = name;
        this.maxOccupancy = maxOccupancy;
        this.size = size;
        this.description = description;
        this.price = price;

    }

}
