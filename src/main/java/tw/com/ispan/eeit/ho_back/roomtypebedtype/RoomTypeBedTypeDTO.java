package tw.com.ispan.eeit.ho_back.roomtypebedtype;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

// 讓業者定義或修改一種特定的「房型分類 + 床型 + 數量」的組合
// 假設您使用 Lombok
import lombok.Data;

@Data
public class RoomTypeBedTypeDTO {

    // 配置 ID（查詢和更新時返回）
    private Integer id;

    // 所屬的房型分類 (例如：豪華房)
    @NotNull(message = "房型分類 ID (room_type_id) 不能為空")
    private Integer roomTypeId;

    // 床型 ID (例如：雙人床)
    @NotNull(message = "床型 ID (bed_type_id) 不能為空")
    private Integer bedTypeId;

    // 該床型在該房型中的數量 (例如：2張單人床)
    @NotNull(message = "床的數量不能為空")
    @Positive(message = "床的數量必須大於 0")
    private Integer bedNumber;

}
