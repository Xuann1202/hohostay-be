package tw.com.ispan.eeit.ho_back.audit_logs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 欄位級別的審計日誌回應
 * 每一筆代表一個欄位的變更
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogFieldChangeResponse {

    /** 原始審計日誌 ID */
    private Long auditLogId;

    /** 執行動作的使用者 ID */
    private Long actorUserId;

    /** 動作類型: insert / update / delete */
    private String actionType;

    /** 受影響的表名 */
    private String targetTable;

    /** 被操作記錄的 ID */
    private Long targetId;

    /** 被修改的欄位名稱 */
    private String fieldName;

    /** 欄位的舊值 */
    private String oldValue;

    /** 欄位的新值 */
    private String newValue;

    /** IP 地址 */
    private String ipAddress;

    /** 建立時間 */
    private LocalDateTime createdAt;
}
