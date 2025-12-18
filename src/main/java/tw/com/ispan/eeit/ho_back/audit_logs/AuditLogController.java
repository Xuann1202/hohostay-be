package tw.com.ispan.eeit.ho_back.audit_logs;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tw.com.ispan.eeit.ho_back.common.PageResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/audit-logs")
public class AuditLogController {

    private final AuditLogService service;

    public AuditLogController(AuditLogService service) {
        this.service = service;
    }

    /**
     * 查詢審計日誌（欄位級別的變更）
     * 每個欄位的變更會被拆分成獨立的記錄
     */
    @GetMapping(value = { "", "/" })
    public PageResponse<AuditLogFieldChangeResponse> search(AuditLogRequest req) {
        var pageable = PageRequest.of(
                req.getPage() == null ? 0 : req.getPage(),
                req.getSize() == null ? 20 : req.getSize());

        var query = new AuditLogQuery();
        query.setActionType(req.getActionType());
        query.setTargetTable(req.getTargetTable());
        query.setActorUserId(req.getActorUserId());
        query.setFrom(req.getFrom());
        query.setTo(req.getTo());
        query.setTargetId(req.getTargetId());
        query.setKeyword(req.getKeyword());

        System.out.println("=== 查詢參數 ===");
        System.out.println("Query ActionType: " + query.getActionType());
        System.out.println("Query TargetTable: " + query.getTargetTable());
        System.out.println("Query ActorUserId: " + query.getActorUserId());

        var page = service.search(query, pageable);
        
        System.out.println("=== 查詢結果 ===");
        System.out.println("Total Elements: " + page.getTotalElements());
        System.out.println("Content Size: " + page.getContent().size());

        // 將每個 AuditLog 拆分成多個欄位變更
        List<AuditLogFieldChangeResponse> allChanges = new ArrayList<>();
        for (AuditLog log : page.getContent()) {
            List<AuditLogFieldChangeResponse> changes = AuditLogDiffUtil.extractFieldChanges(log);
            allChanges.addAll(changes);
        }

        Page<AuditLogFieldChangeResponse> resultPage = new PageImpl<>(
                allChanges,
                page.getPageable(),
                allChanges.size());
        
        return PageResponse.of(resultPage);
    }

    /**
     * 查詢審計日誌列表（別名路由，避免與 /{id} 衝突）
     * GET /api/audit-logs/list
     * 返回完整的 AuditLog 列表，不進行欄位拆分
     */
    @GetMapping("/list")
    public PageResponse<AuditLogResponse> searchList(AuditLogRequest req) {
        System.out.println("=== 收到查詢請求 ===");
        System.out.println("ActionType: " + req.getActionType());
        System.out.println("TargetTable: " + req.getTargetTable());
        System.out.println("ActorUserId: " + req.getActorUserId());
        System.out.println("From: " + req.getFrom());
        System.out.println("To: " + req.getTo());
        
        var pageable = PageRequest.of(
                req.getPage() == null ? 0 : req.getPage(),
                req.getSize() == null ? 20 : req.getSize(),
                Sort.by(Sort.Direction.DESC, "id")); // 按日誌ID降序排序

        var query = new AuditLogQuery();
        query.setActionType(req.getActionType());
        query.setTargetTable(req.getTargetTable());
        query.setActorUserId(req.getActorUserId());
        query.setFrom(req.getFrom());
        query.setTo(req.getTo());
        query.setTargetId(req.getTargetId());
        query.setKeyword(req.getKeyword());

        System.out.println("=== 查詢參數 ===");
        System.out.println("Query ActionType: " + query.getActionType());
        System.out.println("Query TargetTable: " + query.getTargetTable());
        System.out.println("Query ActorUserId: " + query.getActorUserId());

        var page = service.search(query, pageable);
        
        System.out.println("=== 查詢結果 ===");
        System.out.println("Total Elements: " + page.getTotalElements());
        System.out.println("Content Size: " + page.getContent().size());

        // 轉換為 AuditLogResponse 列表（不進行欄位拆分）
        List<AuditLogResponse> responses = page.getContent().stream()
                .map(AuditLogResponse::from)
                .collect(Collectors.toList());

        Page<AuditLogResponse> resultPage = new PageImpl<>(
                responses,
                page.getPageable(),
                page.getTotalElements());
        
        return PageResponse.of(resultPage);
    }

    /**
     * 查詢單筆審計日誌（完整 JSON 格式）
     */
    @GetMapping("/{id}/full")
    public ResponseEntity<AuditLogResponse> getByIdFull(@PathVariable String id) {
        try {
            // 先驗證 id 是否為數字
            if (id == null || !id.matches("\\d+")) {
                return ResponseEntity.notFound().build();
            }
            
            Long idLong = Long.parseLong(id);
            AuditLog log = service.findById(idLong);
            if (log == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(AuditLogResponse.from(log));
        } catch (NumberFormatException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 查詢單筆審計日誌（欄位級別的變更）
     */
    @GetMapping("/{id}")
    public ResponseEntity<List<AuditLogFieldChangeResponse>> getById(@PathVariable String id) {
        try {
            // 先驗證 id 是否為數字
            if (id == null || !id.matches("\\d+")) {
                return ResponseEntity.notFound().build();
            }
            
            Long idLong = Long.parseLong(id);
            AuditLog log = service.findById(idLong);
            if (log == null) {
                return ResponseEntity.notFound().build();
            }
            List<AuditLogFieldChangeResponse> changes = AuditLogDiffUtil.extractFieldChanges(log);
            return ResponseEntity.ok(changes);
        } catch (NumberFormatException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
