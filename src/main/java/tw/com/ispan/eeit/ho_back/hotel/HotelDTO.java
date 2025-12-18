package tw.com.ispan.eeit.ho_back.hotel;

import java.time.LocalTime;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tw.com.ispan.eeit.ho_back.photo.PhotoDTO;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class HotelDTO {
    // --- 飯店識別 ID ---
    // 查詢和更新操作結果必須包含此 ID
    private Integer id;

    // --- 飯店基本資料 (創建/修改時用) ---

    // 名稱和執照(創建時必須，修改時可選)
    @NotBlank(message = "name 不能為空")
    private String name;

    @NotBlank(message = "license 不能為空")
    private String license;

    // 聯絡資料
    private String phone; // 手機 CHAR(10)
    private String localCall; // 市話 CHAR(10)

    // 描述和等級
    private String description;
    private Integer starRating; // 飯店等級 TINYINT (1-5星)

    // 時間
    private LocalTime checkInTime;

    private LocalTime checkOutTime;

    // --- 外鍵 ID (用於關聯) ---

    @NotNull(message = "districtId 不能為空")
    private Integer districtId; // 行政區的 ID

    @NotNull(message = "hotelTypeId 不能為空")
    private Integer hotelTypeId; // 飯店類型 ID

    // --- 關聯對象 (用於查詢結果，包含完整信息) ---
    private tw.com.ispan.eeit.ho_back.district.DistrictDTO district; // 行政區完整信息（包含城市）
    private tw.com.ispan.eeit.ho_back.hoteltype.HotelTypeDTO hotelType; // 飯店類型完整信息

    // --- 位置資訊 (Service 層會自動將地址轉換經緯度，或直接接收經緯度) ---
    // 由於資料庫 DDL 中經緯度是 NOT NULL，在創建時必須提供

    private String address;

    // 修改：經緯度改為可選，如果前端未提供，後端會使用 geocoding 自動轉換
    private Double latitude; // 緯度 (latitude)

    private Double longitude; // 經度 (longitude)

    // --- 狀態控制(用於修改/營業) ---

    /**
     * 業主設定的營業狀態
     * 0: 停業/停用 (邏輯刪除/下架)
     * 1: 營業/啟用
     */
    private Boolean businessStatus;

    // --- 關聯資料 (可創建時帶入) ---

    // 照片列表 (PhotoDTO 定義在 photo package 中)
    private List<PhotoDTO> photos;

    // 設施 ID 列表 (業者勾選已選設施 ID)
    private List<Integer> facilityIds;

}
