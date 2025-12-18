package tw.com.ispan.eeit.ho_back.room;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tw.com.ispan.eeit.ho_back.hotel.HotelDTO;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class RoomDTO {

    // --- 房型基本資訊 ---
    // 這裡改成關聯 DTO
    private HotelDTO hotel;
    // 房間 ID（查詢和更新時返回）
    private Integer id;

    // 所屬飯店 ID (創建時必須)
    private Integer hotelId;

    // 房型名稱 (例如：海景101套房、行政套房、美景房、CityRoom，業者自取)
    private String name;

    // 基本數量、人數、大小
    private Integer quantity; // 基本房間數量
    private Integer maxOccupancy; // 最多入住人數
    private Double size; // 房間大小 DECIMAL(5,2) (使用 Double 傳輸)

    // 價格 (基本定價，如果床型配置沒有單獨定價則使用此價格)
    private BigDecimal basePrice;

    private String description;

    // 狀態 (用於啟用/停用房型)
    private Short status; // 0:停用, 1:啟用

    // 修改：直接關聯到 room_type_bed_type 配置
    // 創建/更新時使用：房東選擇的房型分類、床型和數量
    private Integer roomTypeId; // 房型分類 ID
    private Integer bedTypeId; // 床型 ID
    private Integer bedNumber; // 床的數量

    // 查詢時返回：已關聯的配置 ID
    private Integer roomTypeBedTypeId;
}