package tw.com.ispan.eeit.ho_back.review;

import lombok.Data;

@Data
public class CreateReviewRequestDTO {
    private Integer bookingId;
    private Integer rating;
    private String comment;
}


