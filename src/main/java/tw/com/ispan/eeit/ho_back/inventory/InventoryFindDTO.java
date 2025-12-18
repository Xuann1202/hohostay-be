package tw.com.ispan.eeit.ho_back.inventory;

import lombok.Data;

@Data
public class InventoryFindDTO {
    private Integer start; // 分頁起始
    private Integer rows; // 每頁數量
    private String sort; // 排序欄位
    private Boolean dir; // true = 升序, false = 降序
    private String hotelName; // 可選：飯店名稱篩選
    private String roomName; // 可選：房間名稱篩選

    private Integer hotelId; // 可選：飯店ID篩選
    private Integer roomId; // 可選：房間ID篩選
}
