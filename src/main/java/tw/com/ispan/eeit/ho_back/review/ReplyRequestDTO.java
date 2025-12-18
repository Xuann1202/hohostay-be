package tw.com.ispan.eeit.ho_back.review;

import java.util.Date;

import lombok.Data;

@Data
public class ReplyRequestDTO {
    private Integer id;
    private String reply;
    private Date replyCreatedDate;
    private Date replyUpdatedDate;
}
