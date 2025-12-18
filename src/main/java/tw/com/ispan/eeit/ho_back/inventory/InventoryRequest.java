package tw.com.ispan.eeit.ho_back.inventory;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

@Data
public class InventoryRequest {
    private Integer stock;
    private Integer price;
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Taipei")
    private LocalDate date;
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Taipei")
    private LocalDate startDate;
    private Integer roomId;
}