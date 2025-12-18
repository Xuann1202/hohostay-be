package tw.com.ispan.eeit.ho_back.hotel;

import java.util.ArrayList;
import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import tw.com.ispan.eeit.ho_back.hotelfacility.HotelFacilityDto;
import tw.com.ispan.eeit.ho_back.hotelfacility.HotelFacilityRepository;
import tw.com.ispan.eeit.ho_back.inventory.InventoryRepository;
import tw.com.ispan.eeit.ho_back.inventory.dto.HotelQueryDto;
import tw.com.ispan.eeit.ho_back.inventory.dto.RoomPriceDto;
import tw.com.ispan.eeit.ho_back.inventory.dto.RoomStockDto;
import tw.com.ispan.eeit.ho_back.photo.Photo;
import tw.com.ispan.eeit.ho_back.photo.PhotoRepository;
import tw.com.ispan.eeit.ho_back.review.HotelReviewDto;
import tw.com.ispan.eeit.ho_back.review.ReviewRepository;

@Service
public class HotelPageService {
    @Autowired
    InventoryRepository inventoryRepository;
    @Autowired
    ReviewRepository reviewRepository;
    @Autowired
    HotelFacilityRepository hotelFacilityRepository;
    @Autowired
    HotelRepository hotelRepository;
    @Autowired
    PhotoRepository photoRepository;
    @Autowired
    ModelMapper modelMapper;

    public HotelDetailPageDto hotelPageInfo(Integer hotelId, HotelQueryDto query) {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new RuntimeException("飯店不存在"));
        HotelDetailPageDto result = new HotelDetailPageDto();
        // 飯店基本資訊
        HotelDetailDto hotelInfo = hotelRepository.findHotelInfoByHotelId(hotelId);
        Photo coverPhoto = photoRepository.findFirstByHotelIdAndIsCoverTrue(hotelId);
        if (coverPhoto != null) {
            hotelInfo.setPhotoUrl(coverPhoto.getUrl());
        } else {
            List<Photo> photos = photoRepository.findByHotelId(hotelId);
            if (!photos.isEmpty()) {
                hotelInfo.setPhotoUrl(photos.get(0).getUrl());
            }
        }

        hotelInfo.setHotelId(hotel.getId());

        // 飯店評論
        List<HotelReviewDto> reviews = reviewRepository.findReviewsByHotelId(hotelId);
        Double sum = 0.0;
        for (HotelReviewDto review : reviews) {
            sum += review.getRating();
        }
        Double avgRating = sum / reviews.size();
        hotelInfo.setAvgRating(avgRating);
        result.setHotelDetail(hotelInfo);
        result.setReview(reviews);
        // 飯店設施
        List<HotelFacilityDto> facilities = hotelFacilityRepository.findFacilityByHotelId(hotelId);
        result.setFacilities(facilities);
        // 飯店照片
        List<Photo> photos = photoRepository.findByHotelId(hotelId);
        List<String> photoUrl = new ArrayList<>();
        for (Photo photo : photos) {
            photoUrl.add(photo.getUrl());
        }
        result.setPhotos(photoUrl);
        // 符合的飯店房間價格
        List<RoomPriceDto> rooms = inventoryRepository.findRoomPriceByHotel(hotelId, query);

        // 符合的房間所剩的庫存
        List<Integer> roomIds = new ArrayList<>();
        for (RoomPriceDto room : rooms) {
            room.setNight(query.getNight());
            roomIds.add(room.getRoomId());
        }
        List<RoomStockDto> roomStock = inventoryRepository.findMinStockByRoomIds(roomIds, query.getCheckInDate(),
                query.getCheckOutDate());
        for (RoomPriceDto room : rooms) {
            for (RoomStockDto stock : roomStock) {
                if (room.getRoomId() == stock.getRommId()) {
                    room.setStock(stock.getStock());
                    break;
                }
            }
        }
        result.setRoomInfo(rooms);
        return result;
    }
}
