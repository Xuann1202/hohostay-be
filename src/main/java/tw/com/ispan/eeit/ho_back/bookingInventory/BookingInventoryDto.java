package tw.com.ispan.eeit.ho_back.bookingInventory;

import lombok.Data;

@Data
public class BookingInventoryDto {

    // 對應 Inventory 的主鍵
    private Integer inventoryId;

    // 訂幾間房
    private Integer number;
}
