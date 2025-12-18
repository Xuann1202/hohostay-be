package tw.com.ispan.eeit.ho_back.audit_logs;

import com.fasterxml.jackson.annotation.JsonRawValue;
import lombok.Builder;
import lombok.Value;
import java.time.LocalDateTime;

@Value
@Builder
public class AuditLogResponse {
    Long id;
    Long actorUserId;
    String actionType;
    String targetTable;
    Long targetId;
    
    @JsonRawValue
    String oldValue;
    
    @JsonRawValue
    String newValue;
    
    String ipAddress;
    LocalDateTime createdAt;

    public static AuditLogResponse from(AuditLog log) {
        return AuditLogResponse.builder()
                .id(log.getId())
                .actorUserId(log.getActorUserId())
                .actionType(log.getActionType())
                .targetTable(log.getTargetTable())
                .targetId(log.getTargetId())
                .oldValue(log.getOldValue())
                .newValue(log.getNewValue())
                .ipAddress(log.getIpAddress())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
