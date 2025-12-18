package tw.com.ispan.eeit.ho_back.audit_logs;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "audit_logs")
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 執行動作的使用者ID
     * 從 JWT token 或 request attribute 中獲取
     * 如果無法獲取，使用默認用戶 ID (1) 作為系統操作的默認用戶
     */
    @Column(name = "actor_user_id", nullable = false)
    private Long actorUserId;

    // 動作類型: insert / update / delete
    @Column(name = "action_type", length = 100, nullable = false)
    private String actionType;

    // 受影響的表，如 users/reviews/hotels/...
    @Column(name = "target_table", length = 100)
    private String targetTable;

    // 被操作 table 的 id
    @Column(name = "target_id")
    private Long targetId;

    // 變更前資料（存 JSON 字串）
    @Column(name = "old_value", columnDefinition = "NVARCHAR(MAX)")
    private String oldValue;

    // 變更後資料（存 JSON 字串）
    @Column(name = "new_value", columnDefinition = "NVARCHAR(MAX)")
    private String newValue;

    // 來源 IP
    @Column(name = "ip_address", length = 45, nullable = false)
    private String ipAddress;

    // 觸發器/DB default 自動塞時間 → Java 不主動更新
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

}
