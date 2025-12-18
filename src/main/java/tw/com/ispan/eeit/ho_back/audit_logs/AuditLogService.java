package tw.com.ispan.eeit.ho_back.audit_logs;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuditLogService {
    void logAfterCommit(AuditLog log);

    AuditLog buildLog(
            String actionType,
            String table,
            Long targetId,
            String oldJson,
            String newJson);

    Page<AuditLog> search(AuditLogQuery query, Pageable pageable);
    
    AuditLog findById(Long id);
}
