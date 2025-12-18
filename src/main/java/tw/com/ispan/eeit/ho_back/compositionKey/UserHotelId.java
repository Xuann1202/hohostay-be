package tw.com.ispan.eeit.ho_back.compositionKey;

import java.io.Serializable;

import lombok.Data;

@Data
public class UserHotelId implements Serializable {
    private Integer userId;
    private Integer hotelId;
}
