package tw.com.ispan.eeit.ho_back.photo;

import jakarta.validation.constraints.NotBlank;
// 假設您使用 Lombok
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor // 確保 JSON 轉換需要的無參數建構子存在
@AllArgsConstructor // 🚨 解決方案：自動生成一個包含所有欄位的建構子
@Data
public class PhotoDTO {

    private Integer id;

    /**
     * 飯店 ID（查詢時返回）
     */
    private Integer hotelId;

    /**
     * 照片的路徑或 URL
     * 在創建時，這個欄位通常是必填的 (由前端提供圖片上傳後的回傳 URL)
     */
    @NotBlank(message = "照片 URL 不能為空")
    private String url;

    /**
     * 是否為封面照片 (對應 Photo.is_cover)
     * BIT 類型，0 或 1，對應 Boolean
     */
    private Boolean isCover;

    /**
     * 顯示順序
     */
    private Integer displayOrder;

}
