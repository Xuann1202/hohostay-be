package tw.com.ispan.eeit.ho_back.audit_logs;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import tw.com.ispan.eeit.ho_back.moderation_actions.ModerationAction;
import tw.com.ispan.eeit.ho_back.moderation_actions.ModerationActionRepository;
import tw.com.ispan.eeit.ho_back.moderation_actions.Reason;
import tw.com.ispan.eeit.ho_back.moderation_actions.Status;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 測試修改 ModerationAction 是否會自動寫入 audit_logs
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig.class)
class AuditLogIntegrationTest {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private ModerationActionRepository moderationActionRepository;

    @BeforeEach
    void setUp() {
        // 清理測試資料（在測試前先清除之前的資料）
        auditLogRepository.deleteAll();
        moderationActionRepository.deleteAll();
    }

    @Test
    void testUpdateModerationAction_shouldCreateAuditLog() {
        // 1. 先建立一個 ModerationAction
        ModerationAction action = new ModerationAction();
        action.setReporterId(1L);
        action.setReviewId(2L);
        action.setReviewAuthorId(3L);
        action.setModeratorId(4L);
        action.setReason(Reason.OTHER);
        action.setStatus(Status.PENDING);

        ModerationAction saved = moderationActionRepository.save(action);
        moderationActionRepository.flush();

        // 等待一下，確保審計日誌已寫入
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Long savedId = saved.getId();
        assertNotNull(savedId, "儲存的 ModerationAction 應該有 ID");

        // 記錄初始審計日誌數量
        long initialCount = auditLogRepository.count();
        System.out.println("初始審計日誌數量: " + initialCount);

        // 2. 修改 reason
        saved.setReason(Reason.ABUSE);
        moderationActionRepository.save(saved);
        moderationActionRepository.flush();

        // 等待一下，確保審計日誌已寫入
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // 3. 驗證審計日誌數量增加
        long finalCount = auditLogRepository.count();
        System.out.println("最終審計日誌數量: " + finalCount);

        assertTrue(finalCount > initialCount,
                "修改 ModerationAction 後應該增加審計日誌記錄");

        // 4. 驗證審計日誌內容
        List<AuditLog> logs = auditLogRepository.findAll();
        System.out.println("\n所有審計日誌:");
        for (AuditLog log : logs) {
            System.out.println("  - ID: " + log.getId() +
                    ", Action: " + log.getActionType() +
                    ", Table: " + log.getTargetTable() +
                    ", TargetId: " + log.getTargetId());
            System.out.println("    Old: " + (log.getOldValue() != null
                    ? log.getOldValue().substring(0, Math.min(100, log.getOldValue().length())) + "..."
                    : "null"));
            System.out.println("    New: " + (log.getNewValue() != null
                    ? log.getNewValue().substring(0, Math.min(100, log.getNewValue().length())) + "..."
                    : "null"));
        }

        // 尋找 update 類型的審計日誌
        AuditLog updateLog = logs.stream()
                .filter(log -> "update".equals(log.getActionType()) &&
                        "moderation_actions".equals(log.getTargetTable()) &&
                        savedId.equals(log.getTargetId()))
                .findFirst()
                .orElse(null);

        assertNotNull(updateLog, "應該存在 update 類型的審計日誌");
        assertEquals("moderation_actions", updateLog.getTargetTable());
        assertEquals(savedId, updateLog.getTargetId());

        // 驗證舊值包含原始 reason
        assertNotNull(updateLog.getOldValue(), "oldValue 不應該為 null");
        assertTrue(updateLog.getOldValue().contains("OTHER") || updateLog.getOldValue().contains("其他"),
                "舊值應該包含原始的 reason");

        // 驗證新值包含更新後的 reason
        assertNotNull(updateLog.getNewValue(), "newValue 不應該為 null");
        assertTrue(updateLog.getNewValue().contains("ABUSE") || updateLog.getNewValue().contains("辱罵字眼"),
                "新值應該包含更新後的 reason");
    }

    @Test
    void testInsertModerationAction_shouldCreateAuditLog() {
        // 記錄初始審計日誌數量
        long initialCount = auditLogRepository.count();

        // 建立一個新的 ModerationAction
        ModerationAction action = new ModerationAction();
        action.setReporterId(1L);
        action.setReviewId(2L);
        action.setReviewAuthorId(3L);
        action.setModeratorId(4L);
        action.setReason(Reason.SPAM);
        action.setStatus(Status.PENDING);

        ModerationAction saved = moderationActionRepository.save(action);
        moderationActionRepository.flush();

        // 等待一下，確保審計日誌已寫入
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // 驗證審計日誌數量增加
        long finalCount = auditLogRepository.count();
        System.out.println("初始數量: " + initialCount + ", 最終數量: " + finalCount);

        assertTrue(finalCount > initialCount,
                "建立 ModerationAction 後應該增加審計日誌記錄");

        // 驗證審計日誌內容
        List<AuditLog> logs = auditLogRepository.findAll();
        AuditLog insertLog = logs.stream()
                .filter(log -> "insert".equals(log.getActionType()) &&
                        "moderation_actions".equals(log.getTargetTable()))
                .findFirst()
                .orElse(null);

        assertNotNull(insertLog, "應該存在 insert 類型的審計日誌");
        assertEquals(saved.getId(), insertLog.getTargetId());
        assertNotNull(insertLog.getNewValue(), "新增時應該有 newValue");
    }

    @Test
    void testDeleteModerationAction_shouldCreateAuditLog() {
        // 1. 先建立一個 ModerationAction
        ModerationAction action = new ModerationAction();
        action.setReporterId(1L);
        action.setReviewId(2L);
        action.setReviewAuthorId(3L);
        action.setModeratorId(4L);
        action.setReason(Reason.ADVERTISING);
        action.setStatus(Status.PENDING);

        ModerationAction saved = moderationActionRepository.save(action);
        moderationActionRepository.flush();
        Long savedId = saved.getId();

        // 清空審計日誌（只保留刪除操作的記錄）
        auditLogRepository.deleteAll();

        // 等待一下
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        long initialCount = auditLogRepository.count();

        // 2. 刪除 ModerationAction
        moderationActionRepository.deleteById(savedId);
        moderationActionRepository.flush();

        // 等待一下，確保審計日誌已寫入
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // 3. 驗證審計日誌數量增加
        long finalCount = auditLogRepository.count();
        System.out.println("刪除前: " + initialCount + ", 刪除後: " + finalCount);

        assertTrue(finalCount > initialCount,
                "刪除 ModerationAction 後應該增加審計日誌記錄");

        // 4. 驗證審計日誌內容
        List<AuditLog> logs = auditLogRepository.findAll();
        AuditLog deleteLog = logs.stream()
                .filter(log -> "delete".equals(log.getActionType()) &&
                        "moderation_actions".equals(log.getTargetTable()))
                .findFirst()
                .orElse(null);

        assertNotNull(deleteLog, "應該存在 delete 類型的審計日誌");
        assertEquals(savedId, deleteLog.getTargetId());
        assertNotNull(deleteLog.getOldValue(), "刪除時應該有 oldValue");
    }
}
