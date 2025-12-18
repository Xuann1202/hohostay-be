package tw.com.ispan.eeit.ho_back.moderation_actions;

import lombok.Data;
import java.time.LocalDateTime;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Data
public class ModerationActionDTO {

    private Long id; // 查詢/回傳時會有，新增時可為 null

    @NotNull(message = "reporterId 不可為空")
    private Long reporterId;

    @NotNull(message = "reviewId 不可為空")
    private Long reviewId;

    @NotNull(message = "reviewAuthorId 不可為空")
    private Long reviewAuthorId;

    @NotNull(message = "moderatorId 不可為空")
    private Long moderatorId;

    @Size(max = 255, message = "reason 最多 255 字")
    private String reason;

    /**
     * 原因的中文描述（從 Reason 枚舉獲取）
     * 此字段僅用於前端顯示，不會被持久化到資料庫
     */
    private String reasonDescription;

    private ActionTaken actionTaken;
    @NotNull(message = "status 不可為空")
    private Status status;

    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;

    @Size(max = 4000, message = "metadata 最多 4000 字")
    private String metadata;
}
