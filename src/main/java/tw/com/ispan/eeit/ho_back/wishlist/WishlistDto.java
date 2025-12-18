package tw.com.ispan.eeit.ho_back.wishlist;

import lombok.Data;

@Data
public class WishlistDto {
    private Integer id;
    private String name;
    private String city;
    private String district;
    private String address;
    private Integer starRating;
    private String photoUrl;
    private String description;
}
