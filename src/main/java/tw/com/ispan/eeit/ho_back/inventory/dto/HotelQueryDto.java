package tw.com.ispan.eeit.ho_back.inventory.dto;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class HotelQueryDto {
    @NotBlank(message = "請輸入想去的城市")
    private String keyword;
    @NotNull(message = "請選擇入住日期")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate checkInDate;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @NotNull(message = "請選擇退房日期")
    private LocalDate checkOutDate;
    private Integer night;
    @NotNull
    @Min(value = 1, message = "至少需要1位客人")
    private Integer guestNumber;
    // 可選的搜尋欄位
    private Integer minPrice;
    private Integer maxPrice;
    private List<Integer> hotelTypes;
    private List<Integer> facilities = new ArrayList<>();
    private Integer facilityCount = 0;
    private Integer starRating;

    // 分頁排序
    private Integer page = 0;
    private Integer size = 10;
    private String sortBy = "maxOccupancy";
    private String sortOrder = "asc";

    private void setNight() {
        this.night = (int) ChronoUnit.DAYS.between(this.checkInDate, this.checkOutDate);
    }

    public void setCheckInDate(LocalDate checkInDate) {
        this.checkInDate = checkInDate;
        if (checkOutDate != null) {
            setNight();
        }
    }

    public void setCheckOutDate(LocalDate checkOutDate) {
        this.checkOutDate = checkOutDate;
        if (checkInDate != null) {
            setNight();
        }
    }

    public void setFacilities(List<Integer> facilities) {
        this.facilities = facilities;
        setFacilityCount(facilities);
    }

    private void setFacilityCount(List<Integer> facilities) {
        this.facilityCount = facilities.size();
    }

}
