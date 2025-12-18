package tw.com.ispan.eeit.ho_back.hotel;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class HotelAvgReviewDto {
    private Hotel hotel;
    private Double avgRating;
}
