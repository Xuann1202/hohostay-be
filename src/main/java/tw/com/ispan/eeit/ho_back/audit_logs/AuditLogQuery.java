package tw.com.ispan.eeit.ho_back.audit_logs;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AuditLogQuery {
    private String actionType; // insert / update / delete
    private String targetTable; // users / moderation_actions / coupon ...
    private Long actorUserId;
    private Long targetId;
    private LocalDateTime from; // created_at >= from
    private LocalDateTime to; // created_at < to
    private String keyword; // fuzzy search in old/new JSON
}
