package tw.com.ispan.eeit.ho_back.wishlist;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import jakarta.transaction.Transactional;
import tw.com.ispan.eeit.ho_back.district.CityDistrictDto;
import tw.com.ispan.eeit.ho_back.district.DistrictRepository;
import tw.com.ispan.eeit.ho_back.hotel.Hotel;
import tw.com.ispan.eeit.ho_back.hotel.HotelRepository;
import tw.com.ispan.eeit.ho_back.photo.Photo;
import tw.com.ispan.eeit.ho_back.photo.PhotoRepository;
import tw.com.ispan.eeit.ho_back.user.User;
import tw.com.ispan.eeit.ho_back.user.UserRepository;

@Service
@Transactional
public class WishlistService {
    @Autowired
    UserRepository userRepository;

    @Autowired
    HotelRepository hotelRepository;

    @Autowired
    ModelMapper modelMapper;

    @Autowired
    PhotoRepository photoRepository;

    @Autowired
    DistrictRepository districtRepository;

    // 新增收藏
    public void addToWishList(Integer userId, Integer hotelId) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "請先登入");
        }
        if (hotelId != null) {
            User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("使用者不存在"));
            Hotel hotel = hotelRepository.findById(hotelId).orElseThrow(() -> new RuntimeException("飯店不存在"));

            List<Hotel> hotels = user.getWishList();
            boolean exists = false;
            for (Hotel h : hotels) {
                if (h != null && h.getId() != null && h.getId().equals(hotel.getId())) {
                    exists = true;
                    break;
                }
            }
            if (exists) {
                throw new RuntimeException("此飯店已收藏");
            } else {
                hotels.add(hotel);
            }
        } else {
            throw new RuntimeException("飯店Id不能為空");
        }
    }

    // 刪除收藏
    public void removeFromWishList(Integer userId, Integer hotelId) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "請先登入");
        }
        if (hotelId == null) {
            throw new RuntimeException("飯店Id不能為空");
        }
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("使用者不存在"));
        Hotel hotel = hotelRepository.findById(hotelId).orElseThrow(() -> new RuntimeException("飯店不存在"));
        Boolean removed = false;
        List<Hotel> hotels = user.getWishList();
        Iterator<Hotel> it = hotels.iterator();
        while (it.hasNext()) {
            Hotel h = it.next();
            if (h.getId().equals(hotel.getId())) {
                it.remove();
                removed = true;
                break;
            }
        }
        if (!removed) {
            throw new RuntimeException("此飯店未收藏");
        }
    }

    // 查詢user的所有收藏
    public List<Hotel> findByUserId(Integer userId) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "請先登入");
        }
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("使用者不存在"));
        List<Hotel> hotels = user.getWishList();
        return hotels;
    }

    // 查詢單一飯店是否收藏
    public boolean isHotelLiked(Integer userId, Integer hotelId) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "請先登入");
        }
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("使用者不存在"));
        if (hotelId == null) {
            throw new RuntimeException("飯店Id不能為空");
        }
        Hotel hotel = hotelRepository.findById(hotelId).orElseThrow(() -> new RuntimeException("飯店不存在"));
        List<Hotel> hotels = user.getWishList();
        Boolean isFavorite = false;
        for (Hotel h : hotels) {
            if (h != null && h.getId() != null && h.getId().equals(hotel.getId())) {
                isFavorite = true;
                break;
            }
        }
        return isFavorite;
    }

    // 分頁wishlist
    public Page<WishlistDto> getUserWishlist(Integer userId, int page, int size) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "請先登入");
        }
        Page<Hotel> hotels = hotelRepository.findByUserFavorite(userId, PageRequest.of(page, size));

        List<WishlistDto> wishlist = new ArrayList<>();
        // 取封面圖
        for (Hotel hotel : hotels) {
            WishlistDto dto = modelMapper.map(hotel, WishlistDto.class);
            Photo photo = photoRepository.findFirstByHotelIdAndIsCoverTrue(hotel.getId());
            if (photo != null) {
                dto.setPhotoUrl(photo.getUrl());
            }
            // 取城市與行政區
            CityDistrictDto cityDistrict = districtRepository.findByHotelId(hotel.getId());
            dto.setCity(cityDistrict.getCityName());
            dto.setDistrict(cityDistrict.getDistrictName());
            wishlist.add(dto);
        }
        return new PageImpl<WishlistDto>(wishlist, hotels.getPageable(), hotels.getTotalElements());

    }
}
