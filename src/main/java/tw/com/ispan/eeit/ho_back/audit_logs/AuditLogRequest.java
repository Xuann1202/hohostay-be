package tw.com.ispan.eeit.ho_back.audit_logs;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

@Data
public class AuditLogRequest {
    private String actionType;
    private String targetTable;
    private Long actorUserId;
    private Long targetId;
    private String keyword;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime from;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime to;

    // 分頁用（可讓 Controller 透過 Pageable 處理）
    private Integer page = 0;
    private Integer size = 20;
}