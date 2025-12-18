package tw.com.ispan.eeit.ho_back.hotel;

import java.util.List;

import lombok.Data;
import tw.com.ispan.eeit.ho_back.hotelfacility.HotelFacilityDto;
import tw.com.ispan.eeit.ho_back.inventory.dto.RoomPriceDto;
import tw.com.ispan.eeit.ho_back.review.HotelReviewDto;

//回傳飯店頁面資料
@Data
public class HotelDetailPageDto {
    private HotelDetailDto hotelDetail;
    private List<RoomPriceDto> roomInfo;
    private List<HotelReviewDto> review;
    private List<String> photos;
    private List<HotelFacilityDto> facilities;
}
