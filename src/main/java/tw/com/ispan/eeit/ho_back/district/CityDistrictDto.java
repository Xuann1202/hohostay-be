package tw.com.ispan.eeit.ho_back.district;

import lombok.Data;

@Data
public class CityDistrictDto {
    private Integer cityId;
    private Integer districtId;
    private String cityName;
    private String districtName;

    public CityDistrictDto(Integer cityId, Integer districtId, String cityName, String districtName) {
        this.cityId = cityId;
        this.districtId = districtId;
        this.cityName = cityName;
        this.districtName = districtName;
    }

}
