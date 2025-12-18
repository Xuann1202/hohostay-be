package tw.com.ispan.eeit.ho_back.review;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class ReviewRepositoryTest {
    @Autowired
    ReviewRepository reviewRepository;

    @Test
    public void findAllByIds() {
        List<Integer> hotelIds = new ArrayList<>();
        hotelIds.add(1);
        List<HotelReviewDto> reviews = reviewRepository.findReviewsByHotelIds(hotelIds);
        System.out.println(reviews);
    }

    @Test
    public void findReviewsByHotelId() {
        Integer hotelId = 1;
        System.out.println(reviewRepository.findReviewsByHotelId(hotelId));
    }
}
