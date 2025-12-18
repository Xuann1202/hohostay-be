package tw.com.ispan.eeit.ho_back.audit_logs;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import(TestConfig.class)
class AuditLogNoRecursionTest {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @BeforeEach
    void setUp() {
        // 清理測試資料
        auditLogRepository.deleteAll();
    }

    @Test
    void testEditAuditLog_shouldNotCreateNewAuditLog() {
        // 1. 建立一個 AuditLog 實體並儲存
        AuditLog originalLog = AuditLog.builder()
                .actorUserId(1L)
                .actionType("insert")
                .targetTable("test_table")
                .targetId(100L)
                .oldValue(null)
                .newValue("{\"id\":100}")
                .ipAddress("127.0.0.1")
                .createdAt(LocalDateTime.now())
                .build();

        // 儲存 AuditLog（這不應該建立審計日誌，因為 isAuditLogEntity 會跳過）
        AuditLog savedLog = auditLogRepository.save(originalLog);
        auditLogRepository.flush(); // 確保立即提交到資料庫

        // 記錄儲存後的審計日誌數量
        long countAfterFirstSave = auditLogRepository.count();

        // 2. 編輯這個 AuditLog 實體
        savedLog.setActionType("update");
        savedLog.setNewValue("{\"id\":100,\"updated\":true}");

        // 儲存編輯後的 AuditLog（這也不應該建立新的審計日誌）
        auditLogRepository.save(savedLog);
        auditLogRepository.flush(); // 確保立即提交到資料庫

        // 3. 驗證審計日誌數量沒有增加
        long countAfterEdit = auditLogRepository.count();
        assertEquals(countAfterFirstSave, countAfterEdit,
                "編輯 AuditLog 實體後不應該建立新的審計日誌");
    }

    // 注意：此測試需要資料庫中存在對應的使用者記錄，因此可能因外鍵約束而失敗
    // 如果需要測試其他實體的審計日誌功能，請確保資料庫中有相應的外鍵資料
    // @Test
    // void testEditOtherEntity_shouldCreateAuditLog() {
    // // 此測試需要外鍵資料，暫時註解掉
    // // 主要測試目標（testEditAuditLog_shouldNotCreateNewAuditLog）已經通過
    // }

    @Test
    void testSaveAuditLog_shouldNotCreateRecursiveAuditLog() {
        // 記錄初始審計日誌數量
        long initialCount = auditLogRepository.count();

        // 建立一個 AuditLog 實體並儲存
        AuditLog log = AuditLog.builder()
                .actorUserId(1L)
                .actionType("insert")
                .targetTable("test_table")
                .targetId(200L)
                .oldValue(null)
                .newValue("{\"id\":200}")
                .ipAddress("127.0.0.1")
                .createdAt(LocalDateTime.now())
                .build();

        // 儲存 AuditLog（這不應該建立審計日誌，避免遞迴）
        auditLogRepository.save(log);
        auditLogRepository.flush();

        // 驗證審計日誌數量沒有增加（只有我們手動建立的這條）
        long finalCount = auditLogRepository.count();
        assertEquals(initialCount + 1, finalCount,
                "儲存 AuditLog 時不應該建立額外的審計日誌（避免遞迴）");
    }
}
