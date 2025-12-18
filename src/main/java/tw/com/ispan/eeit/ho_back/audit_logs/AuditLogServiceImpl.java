package tw.com.ispan.eeit.ho_back.audit_logs;

import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;

@Service
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository repository;
    private final RequestContext requestContext;
    private final ApplicationContext applicationContext;

    public AuditLogServiceImpl(AuditLogRepository repository,
            RequestContext requestContext,
            ApplicationContext applicationContext) {
        this.repository = repository;
        this.requestContext = requestContext;
        this.applicationContext = applicationContext;
    }

    @Override
    public void logAfterCommit(AuditLog log) {
        System.out.println("\n---------- logAfterCommit 被調用 ----------");
        System.out.println("審計日誌: " + log);

        boolean isSyncActive = TransactionSynchronizationManager.isSynchronizationActive();
        System.out.println("事務同步是否啟用: " + isSyncActive);

        if (isSyncActive) {
            System.out.println("註冊事務同步回調，將在事務提交後保存");
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    System.out.println("\n>>> 事務已提交，開始保存審計日誌 <<<");
                    // 通過 ApplicationContext 獲取代理，確保 @Transactional 生效
                    AuditLogServiceImpl proxy = applicationContext.getBean(AuditLogServiceImpl.class);
                    proxy.saveInNewTransaction(log);
                }
            });
        } else {
            System.out.println("⚠️ 事務同步未啟用，直接在新事務中保存");
            AuditLogServiceImpl proxy = applicationContext.getBean(AuditLogServiceImpl.class);
            proxy.saveInNewTransaction(log);
        }
        System.out.println("---------- logAfterCommit 完成 ----------\n");
    }

    /**
     * 在新事務中保存審計日誌
     * 使用 REQUIRES_NEW 確保即使在事務同步回調中也能成功保存
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveInNewTransaction(AuditLog log) {
        System.out.println(">>> 在新事務中保存審計日誌 <<<");
        try {
            AuditLog saved = repository.save(log);
            System.out.println(">>> 審計日誌保存成功！ID: " + saved.getId() + " <<<");
            System.out.println(">>> 審計內容: action=" + saved.getActionType() +
                    ", table=" + saved.getTargetTable() +
                    ", targetId=" + saved.getTargetId() + " <<<\n");
        } catch (Exception e) {
            System.err.println(">>> ❌ 審計日誌保存失敗: " + e.getMessage() + " <<<");
            e.printStackTrace();
        }
    }

    @Override
    public AuditLog buildLog(String actionType, String table, Long targetId, String oldJson, String newJson) {
        String ip;
        Long userId;

        try {
            ip = requestContext.getClientIp();
        } catch (Exception e) {
            // 如果獲取 IP 失敗（例如在非 HTTP 請求環境中），使用預設值
            System.err.println("Failed to get client IP: " + e.getMessage());
            ip = "0.0.0.0";
        }

        try {
            userId = requestContext.getCurrentUserId();
        } catch (Exception e) {
            // 如果獲取 userId 失敗，使用默認用戶 ID
            System.err.println("Failed to get current user ID: " + e.getMessage());
            userId = null;
        }

        // 如果 userId 為 null，使用默認用戶 ID (1)
        // 確保每筆審計日誌都有操作者
        // 注意：RequestContext.getCurrentUserId() 已實現從 JWT token 獲取用戶 ID
        // 如果仍然為 null（例如非 HTTP 請求環境或 token 無效），使用默認用戶 ID
        if (userId == null) {
            userId = 1L; // 使用 id=1 的用戶作為系統操作的默認用戶（用於非認證環境或系統操作）
            System.out.println("⚠️ 無法獲取當前用戶 ID，使用默認用戶 ID (1) 作為操作者");
        }

        if (ip == null || ip.isBlank()) {
            ip = "0.0.0.0";
        }

        System.out.println("Building audit log: action=" + actionType +
                ", table=" + table +
                ", targetId=" + targetId +
                ", userId=" + userId +
                ", ip=" + ip);

        return AuditLog.builder()
                .actionType(actionType)
                .targetTable(table)
                .targetId(targetId)
                .oldValue(oldJson)
                .newValue(newJson)
                .actorUserId(userId) // 永遠不會是 null
                .ipAddress(ip)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Override
    public Page<AuditLog> search(AuditLogQuery query, Pageable pageable) {
        System.out.println("AuditLogServiceImpl.search: query=" + query);
        System.out.println("AuditLogServiceImpl.search: query.actionType=" + (query != null ? query.getActionType() : "null"));
        Specification<AuditLog> spec = AuditLogSpecs.byQuery(query);
        System.out.println("AuditLogServiceImpl.search: Specification created");
        Page<AuditLog> result = repository.findAll(spec, pageable);
        System.out.println("AuditLogServiceImpl.search: Result size=" + result.getContent().size());
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public AuditLog findById(Long id) {
        return repository.findById(id).orElse(null);
    }
}
