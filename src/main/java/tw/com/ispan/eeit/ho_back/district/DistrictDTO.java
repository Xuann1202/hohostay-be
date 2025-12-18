package tw.com.ispan.eeit.ho_back.district;

import lombok.Data;
import tw.com.ispan.eeit.ho_back.city.CityDTO;

@Data
public class DistrictDTO {
    private Integer id;
    private String name;
    private CityDTO city;
}
