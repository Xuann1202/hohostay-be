package tw.com.ispan.eeit.ho_back.inventory.dto;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class SearchResultDto {
    private Integer hotelId;
    private String hotelName;
    private String city;
    private String district;
    private String address;
    private String roomType;
    private String bedType;
    private Integer bedNumber;
    private Long partPrice;
    private Integer night;
    private Double avgRating;
    private Integer starRating;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String photoUrl;
    private Integer maxOccupancy;

}
