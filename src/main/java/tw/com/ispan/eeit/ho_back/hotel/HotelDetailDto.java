package tw.com.ispan.eeit.ho_back.hotel;

import java.math.BigDecimal;
import java.time.LocalTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HotelDetailDto {
    private Integer hotelId;
    private String hotelName;
    private Integer starRating;
    private String district;
    private String city;
    private String address;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String photoUrl;
    private String description;
    private Double avgRating;
    private LocalTime checkInTime;
    private LocalTime checkOutTime;

    // 沒有photoUrl的constructor
    public HotelDetailDto(Integer hotelId, String hotelName, Integer starRating, String district, String city,
            String address, BigDecimal latitude, BigDecimal longitude, String description) {
        this.hotelId = hotelId;
        this.hotelName = hotelName;
        this.starRating = starRating;
        this.district = district;
        this.city = city;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.description = description;
    }

    public HotelDetailDto(Integer hotelId, String hotelName, Integer starRating, String district, String city,
            String address, BigDecimal latitude, BigDecimal longitude, String description, Double avgRating) {
        this.hotelId = hotelId;
        this.hotelName = hotelName;
        this.starRating = starRating;
        this.district = district;
        this.city = city;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.description = description;
        this.avgRating = avgRating;
    }

    public HotelDetailDto(Integer hotelId, String hotelName, Integer starRating, String district, String city,
            String address, BigDecimal latitude, BigDecimal longitude, String description,
            LocalTime checkInTime, LocalTime checkOutTime) {
        this.hotelId = hotelId;
        this.hotelName = hotelName;
        this.starRating = starRating;
        this.district = district;
        this.city = city;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.description = description;
        this.checkInTime = checkInTime;
        this.checkOutTime = checkOutTime;
    }

}
