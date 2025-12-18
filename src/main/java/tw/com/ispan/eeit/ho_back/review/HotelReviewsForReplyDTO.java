package tw.com.ispan.eeit.ho_back.review;

import java.util.List;

import lombok.Data;
import tw.com.ispan.eeit.ho_back.hotel.Hotel;

@Data
public class HotelReviewsForReplyDTO {

    private Hotel hotel;
    private List<HotelReviewDto> reviews;

    public HotelReviewsForReplyDTO(Hotel hotel, List<HotelReviewDto> reviews) {
        this.hotel = hotel;
        this.reviews = reviews;
    }
}
