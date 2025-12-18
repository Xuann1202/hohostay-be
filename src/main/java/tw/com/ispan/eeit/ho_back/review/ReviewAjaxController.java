package tw.com.ispan.eeit.ho_back.review;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import tw.com.ispan.eeit.ho_back.hotel.Hotel;

@RestController
@RequestMapping("/api/hotelreviews")
public class ReviewAjaxController {

    @Autowired
    private ReviewService reviewService;

    // 取得房東的飯店與評論
    @GetMapping("/owner")
    public ResponseEntity<List<HotelReviewsForReplyDTO>> getHotelsByOwner(@RequestHeader Integer userId) {
        try {
            List<Hotel> hotels = reviewService.getHotelsByOwner(userId);

            List<HotelReviewsForReplyDTO> result = hotels.stream().map(hotel -> {
                List<Review> reviews = reviewService.getReviewsByHotelId(hotel.getId());

                List<HotelReviewDto> reviewDtos = reviews.stream().map(r -> {
                    String firstName = r.getUser() != null ? r.getUser().getFirstName() : "";
                    String lastName = r.getUser() != null ? r.getUser().getLastName() : "";
                    String image = (r.getUser() != null && r.getUser().getImage() != null)
                            ? r.getUser().getImage()
                            : "default-avatar.png";

                    return new HotelReviewDto(
                            r.getId(),
                            hotel.getId(),
                            r.getRating(),
                            r.getComment(),
                            r.getReply(),
                            r.getCreatedDate(),
                            firstName,
                            lastName,
                            image);
                }).toList();

                return new HotelReviewsForReplyDTO(hotel, reviewDtos);
            }).toList();

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    // 新增/修改回覆
    @PostMapping("/reply")
    public ResponseEntity<?> updateReply(@RequestBody ReplyRequestDTO dto) {
        if (dto.getReply() == null || dto.getReply().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("回覆內容不可為空");
        }

        Review result = reviewService.updateReply(dto);
        return ResponseEntity.ok(result);
    }

}