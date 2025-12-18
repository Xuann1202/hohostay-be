package tw.com.ispan.eeit.ho_back.review;

import lombok.Data;

@Data
public class UpdateReviewRequestDTO {
    private Integer rating;
    private String comment;
}


