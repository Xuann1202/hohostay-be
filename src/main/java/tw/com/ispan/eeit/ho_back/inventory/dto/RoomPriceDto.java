package tw.com.ispan.eeit.ho_back.inventory.dto;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class RoomPriceDto {
    private Integer hotelId;
    private Integer roomId;
    private Long partPrice;
    private String roomType;
    private String bedType;
    private Integer bedNumber;
    private String roomName;
    private Integer maxOccupancy;
    private BigDecimal roomSize;
    private Integer stock;
    private Integer night;

    public RoomPriceDto(Integer hotelId, Integer roomId, Long partPrice, String roomType, String bedType,
            Integer bedNumber, String roomName, Integer maxOccupancy, BigDecimal roomSize) {
        this.hotelId = hotelId;
        this.roomId = roomId;
        this.partPrice = partPrice;
        this.roomType = roomType;
        this.bedType = bedType;
        this.bedNumber = bedNumber;
        this.roomName = roomName;
        this.maxOccupancy = maxOccupancy;
        this.roomSize = roomSize;
    }

}
