package tw.com.ispan.eeit.ho_back.review;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

@Data
public class HotelReviewDto {
    private Integer reviewId;
    private Integer hotelId;
    private Integer rating;
    private String comment;
    private String reply; // 舊欄位保留
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate createdDate;
    private String firstName;
    private String lastName;
    private String image;
    private Integer userId; // 評論作者ID

    // ===== 原本建構子 =====
    public HotelReviewDto(Integer reviewId, Integer hotelId, Integer rating, String comment, String reply,
            LocalDate createdDate) {
        this.reviewId = reviewId;
        this.hotelId = hotelId;
        this.rating = rating;
        this.comment = comment;
        this.reply = reply;
        this.createdDate = createdDate;
    }

    public HotelReviewDto(Integer reviewId, Integer hotelId, Integer rating, String comment, String reply,
            LocalDate createdDate, String firstName, String lastName, String image) {
        this.reviewId = reviewId;
        this.hotelId = hotelId;
        this.rating = rating;
        this.comment = comment;
        this.reply = reply;
        this.createdDate = createdDate;
        this.firstName = firstName;
        this.lastName = lastName;

        if (image == null || image.isBlank()) {
            this.image = "hotel-reply.png";
        } else {
            this.image = image;
        }
    }

    public HotelReviewDto(Integer reviewId, Integer hotelId, Integer rating, String comment, String reply,
            LocalDate createdDate, String firstName, String lastName, String image, Integer userId) {
        this.reviewId = reviewId;
        this.hotelId = hotelId;
        this.rating = rating;
        this.comment = comment;
        this.reply = reply;
        this.createdDate = createdDate;
        this.firstName = firstName;
        this.lastName = lastName;
        this.userId = userId;

        // if (image == null || image.isBlank()) {
        // this.image = "/hotel-reply.png";
        // } else {
        this.image = image;
        // }
    }

    // ===== 新增方法：回傳 ReplyDto =====
    public ReplyDto getReplyDto() {
        if (this.reply != null && !this.reply.isBlank()) {
            return new ReplyDto(this.reply);
        } else {
            return null; // 沒有回覆就回 null
        }
    }

}