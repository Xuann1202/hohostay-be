package tw.com.ispan.eeit.ho_back.hotel;

import java.math.BigDecimal;

public interface GetNearbyHotelProjection {
    Integer getId();

    String getName();

    BigDecimal getLongitude();

    BigDecimal getLatitude();

    String getAddress();

    String getLocalCall();

    String getDescription();

    Integer getStarRating();

    Integer getMinPriceForNight();

    String getRoomName();

    Integer getBedNumber();

    String getBedType();

    Integer getMaxOccupancy();

    String getCoverPhotoUrl();

    String getDistrictName();

    String getCityName();

    Double getAverageRating();

    Double getDistanceKm();
}
