package tw.com.ispan.eeit.ho_back.booking;

import java.time.LocalDate;
import java.util.List;

import lombok.Data;
import tw.com.ispan.eeit.ho_back.bookingInventory.BookingInventoryDto;

@Data
public class BookingDto {

    private Integer userId;

    private LocalDate startDate;
    private LocalDate endDate;

    private String couponSn;

    private String leadGuest;
    private String request;

    // 房型明細
    private List<BookingInventoryDto> bookingInventoriesDto;

    // 給 ECPay 拼 item name 用
    private List<String> roomName;
}
