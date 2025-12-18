package tw.com.ispan.eeit.ho_back.moderation_actions;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tw.com.ispan.eeit.ho_back.audit_logs.RequestContext;
import tw.com.ispan.eeit.ho_back.common.PageResponse;
import tw.com.ispan.eeit.ho_back.util.OwnerAuthHelper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/moderation-actions")
@RequiredArgsConstructor
public class ModerationActionController {

    private final ModerationActionService service;
    private final ModelMapper modelMapper;
    private final EntityManager entityManager;
    private final RequestContext requestContext;
    private final OwnerAuthHelper ownerAuthHelper;

    /**
     * 將 ModerationAction 轉換為 DTO 並設置 reasonDescription
     */
    private ModerationActionDTO toDTO(ModerationAction action) {
        ModerationActionDTO dto = modelMapper.map(action, ModerationActionDTO.class);
        // 設置原因的中文描述
        if (action.getReason() != null) {
            dto.setReasonDescription(action.getReason().getDescription());
        }
        return dto;
    }

    /**
     * 同時支援分頁與不分頁
     * 若沒有帶 page / size，則回傳全部 List
     */
    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {

        // 沒有分頁參數 → 回傳整個 List
        if (page == null || size == null) {
            List<ModerationActionDTO> list = service.listAll(Sort.by("createdAt").descending())
                    .stream()
                    .map(this::toDTO)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(list);
        }

        // 有分頁參數 → 回傳 Page
        Page<ModerationAction> actionPage = service.list(PageRequest.of(page, size, Sort.by("createdAt").descending()));

        Page<ModerationActionDTO> dtoPage = actionPage
                .map(this::toDTO);

        return ResponseEntity.ok(PageResponse.of(dtoPage));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ModerationActionDTO> get(@PathVariable Long id) {
        ModerationAction data = service.get(id);
        return (data != null)
                ? ResponseEntity.ok(toDTO(data))
                : ResponseEntity.notFound().build();
    }

    @PostMapping
    public ResponseEntity<ModerationActionDTO> create(@Valid @RequestBody ModerationActionDTO dto) {
        ModerationAction action = modelMapper.map(dto, ModerationAction.class);
        ModerationAction saved = service.create(action);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toDTO(saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ModerationActionDTO> update(
            @PathVariable Long id,
            @RequestBody ModerationActionDTO dto,
            HttpServletRequest request) {

        // 檢查記錄是否存在
        ModerationAction existing = service.get(id);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }

        // 保存原始的 moderatorId（如果存在）
        Long originalModeratorId = existing.getModeratorId();

        // 只更新 DTO 中有值的欄位（null 值會被忽略）
        modelMapper.getConfiguration().setSkipNullEnabled(true);
        modelMapper.map(dto, existing);

        // 如果狀態更新為 RESOLVED（審查完成），自動設置審查人ID為當前登入用戶
        // 檢查 DTO 中的狀態或 existing 對象的狀態
        Status newStatus = dto.getStatus() != null ? dto.getStatus() : existing.getStatus();
        if (newStatus == Status.RESOLVED) {
            // 嘗試多種方式獲取當前用戶ID
            Long currentUserId = null;

            // 方式1: 從 RequestContext 獲取（可能從 JWT token 解析）
            try {
                currentUserId = requestContext.getCurrentUserId();
            } catch (Exception e) {
                System.err.println("從 RequestContext 獲取 userId 失敗: " + e.getMessage());
            }

            // 方式2: 如果 RequestContext 失敗，直接從 request 獲取（備用方案）
            if (currentUserId == null) {
                try {
                    Integer userIdInt = ownerAuthHelper.getUserIdFromRequest(request);
                    currentUserId = userIdInt != null ? userIdInt.longValue() : null;
                } catch (Exception e) {
                    System.err.println("從 OwnerAuthHelper 獲取 userId 失敗: " + e.getMessage());
                }
            }

            // 方式3: 從 request attribute 直接獲取（如果 JwtInterceptor 設置了）
            if (currentUserId == null) {
                Object userIdAttr = request.getAttribute("userId");
                if (userIdAttr != null) {
                    try {
                        currentUserId = userIdAttr instanceof Long ? (Long) userIdAttr
                                : userIdAttr instanceof Integer ? ((Integer) userIdAttr).longValue()
                                        : Long.parseLong(userIdAttr.toString());
                    } catch (Exception e) {
                        System.err.println("從 request attribute 解析 userId 失敗: " + e.getMessage());
                    }
                }
            }

            System.out.println("=== 審查完成處理 ===");
            System.out.println("DTO Status: " + dto.getStatus());
            System.out.println("Existing Status: " + existing.getStatus());
            System.out.println("Current User ID: " + currentUserId);
            System.out.println("Original Moderator ID: " + originalModeratorId);

            if (currentUserId != null) {
                existing.setModeratorId(currentUserId);
                System.out.println("✓ 已設置審查人ID為: " + currentUserId);
            } else {
                System.out.println("✗ 警告：無法獲取當前登入用戶ID，審查人ID未更新");
                // 如果無法獲取當前用戶ID，保持原有的 moderatorId（如果存在）
                if (originalModeratorId == null) {
                    System.out.println("✗ 錯誤：原有 moderatorId 也為 null，無法更新");
                    // 如果原有 moderatorId 也為 null，使用默認值 1（admin）
                    existing.setModeratorId(1L);
                    System.out.println("⚠️ 使用默認 moderatorId: 1 (admin)");
                }
            }
        } else {
            System.out.println("狀態不是 RESOLVED，跳過設置審查人ID。當前狀態: " + newStatus);
        }

        // 在保存前驗證 moderatorId 是否已設置
        System.out.println("=== 保存前驗證 ===");
        System.out.println("Existing Moderator ID: " + existing.getModeratorId());
        System.out.println("Existing Status: " + existing.getStatus());

        // 儲存更新
        ModerationAction updated = service.update(id, existing);

        System.out.println("=== 保存後驗證 ===");
        System.out.println("Updated Moderator ID: " + updated.getModeratorId());
        System.out.println("Updated Status: " + updated.getStatus());

        return ResponseEntity.ok(toDTO(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 根據 reviewId 獲取評論內容（使用原生 SQL 查詢 review 表）
     * 
     * 實現說明：
     * - 此方法用於後台查看詳情，即使 is_visible = 0（隱藏的評論），後台仍能查詢到內容
     * - 會嘗試多個可能的表名和欄位名稱以確保兼容性
     * - 支援多種可能的評論內容欄位名稱（comment, content, text, message 等）
     * - 如果查詢失敗，會返回錯誤信息
     */
    @GetMapping("/review/{reviewId}")
    public ResponseEntity<Map<String, Object>> getReviewContent(@PathVariable Long reviewId) {
        // 嘗試多個可能的表名（SQL Server 可能需要方括號）
        String[] possibleTableNames = { "[review]", "review", "[reviews]", "reviews" };

        Exception lastException = null;

        for (String tableName : possibleTableNames) {
            try {
                // 方法1: 先查詢 INFORMATION_SCHEMA 找出實際存在的欄位
                String cleanTableName = tableName.replace("[", "").replace("]", "");
                String checkColumnsSql = "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS " +
                        "WHERE TABLE_NAME = :tableName AND " +
                        "COLUMN_NAME IN ('comment', 'content', 'text', 'message', 'review_text', 'review_content', 'comment_text', 'description', 'body')";

                Query checkQuery = entityManager.createNativeQuery(checkColumnsSql);
                checkQuery.setParameter("tableName", cleanTableName);

                @SuppressWarnings("unchecked")
                List<String> availableColumns = checkQuery.getResultList();

                if (!availableColumns.isEmpty()) {
                    // 使用第一個找到的欄位
                    String columnName = availableColumns.get(0);
                    String sql = "SELECT id, [" + columnName + "] FROM " + tableName + " WHERE id = :reviewId";

                    Query query = entityManager.createNativeQuery(sql);
                    query.setParameter("reviewId", reviewId);

                    @SuppressWarnings("unchecked")
                    List<Object[]> results = query.getResultList();

                    if (results != null && !results.isEmpty()) {
                        Object[] result = results.get(0);
                        String comment = result[1] != null ? result[1].toString().trim() : "";

                        // 查詢到記錄就返回，即使內容為空（前端會顯示 "(無)"）
                        Map<String, Object> reviewData = new HashMap<>();
                        reviewData.put("id", result[0]);
                        reviewData.put("comment", comment);
                        reviewData.put("content", comment);
                        reviewData.put("text", comment);
                        reviewData.put("message", comment);
                        return ResponseEntity.ok(reviewData);
                    }
                }

                // 方法2: 如果找不到欄位，嘗試直接查詢所有可能的欄位（使用 COALESCE）
                String sql = "SELECT TOP 1 id, " +
                        "COALESCE([comment], [content], [text], [message], [review_text], [review_content], [comment_text], [description], [body], '') AS comment_text "
                        +
                        "FROM " + tableName + " WHERE id = :reviewId";

                Query query = entityManager.createNativeQuery(sql);
                query.setParameter("reviewId", reviewId);

                @SuppressWarnings("unchecked")
                List<Object[]> results = query.getResultList();

                if (results != null && !results.isEmpty()) {
                    Object[] result = results.get(0);
                    String comment = result[1] != null ? result[1].toString().trim() : "";

                    // 查詢到記錄就返回，即使內容為空（前端會顯示 "(無)"）
                    Map<String, Object> reviewData = new HashMap<>();
                    reviewData.put("id", result[0]);
                    reviewData.put("comment", comment);
                    reviewData.put("content", comment);
                    reviewData.put("text", comment);
                    reviewData.put("message", comment);
                    return ResponseEntity.ok(reviewData);
                }

            } catch (Exception e) {
                lastException = e;
                // 繼續嘗試下一個表名
                continue;
            }
        }

        // 所有嘗試都失敗了
        Map<String, Object> error = new HashMap<>();
        error.put("error", "無法獲取評論內容");
        if (lastException != null) {
            String errorMsg = lastException.getMessage();
            // 如果錯誤信息包含表名相關的錯誤，提供更友好的提示
            if (errorMsg != null && (errorMsg.contains("Invalid object name") || errorMsg.contains("找不到"))) {
                error.put("message", "找不到 review 表，請確認資料庫表名是否正確");
            } else {
                error.put("message", "查詢失敗: " + errorMsg);
            }
            lastException.printStackTrace();
        } else {
            error.put("message", "找不到評論 ID " + reviewId);
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    /**
     * 獲取本月總評論數（從 review 表查詢 created_date 為本月的評論總數）
     * 
     * 實現說明：
     * - 此方法會嘗試多個可能的表名（review, reviews）和日期欄位名稱以確保兼容性
     * - 支援多種日期欄位名稱（created_date, created_at, create_date 等）
     * - 會先檢查欄位是否存在，再執行查詢
     * - 如果查詢失敗，會返回錯誤信息
     */
    @GetMapping("/stats/month-total-reviews")
    public ResponseEntity<Map<String, Object>> getMonthTotalReviews() {
        // 獲取本月的開始和結束時間
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(java.util.Calendar.DAY_OF_MONTH, 1);
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
        cal.set(java.util.Calendar.MINUTE, 0);
        cal.set(java.util.Calendar.SECOND, 0);
        cal.set(java.util.Calendar.MILLISECOND, 0);
        java.util.Date monthStart = cal.getTime();

        // 下個月的第一天作為結束時間
        cal.add(java.util.Calendar.MONTH, 1);
        java.util.Date monthEnd = cal.getTime();

        // 嘗試多個可能的表名和日期欄位名
        String[] possibleTableNames = { "[review]", "review", "[reviews]", "reviews" };
        String[] possibleDateColumns = { "created_date", "created_at", "create_date", "create_at", "date_created",
                "createdDate", "createdAt" };

        Exception lastException = null;

        for (String tableName : possibleTableNames) {
            for (String dateColumn : possibleDateColumns) {
                try {
                    String cleanTableName = tableName.replace("[", "").replace("]", "");

                    // 先檢查欄位是否存在
                    String checkColumnSql = "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS " +
                            "WHERE TABLE_NAME = :tableName AND COLUMN_NAME = :dateColumn";
                    Query checkQuery = entityManager.createNativeQuery(checkColumnSql);
                    checkQuery.setParameter("tableName", cleanTableName);
                    checkQuery.setParameter("dateColumn", dateColumn);

                    @SuppressWarnings("unchecked")
                    List<String> columns = checkQuery.getResultList();

                    if (!columns.isEmpty()) {
                        // 欄位存在，執行查詢
                        String sql = "SELECT COUNT(*) FROM " + tableName +
                                " WHERE [" + dateColumn + "] >= :monthStart AND [" + dateColumn + "] < :monthEnd";

                        Query query = entityManager.createNativeQuery(sql);
                        query.setParameter("monthStart", new java.sql.Timestamp(monthStart.getTime()));
                        query.setParameter("monthEnd", new java.sql.Timestamp(monthEnd.getTime()));

                        Object result = query.getSingleResult();
                        Long count = result != null ? ((Number) result).longValue() : 0L;

                        Map<String, Object> response = new HashMap<>();
                        response.put("count", count);
                        response.put("monthStart", monthStart);
                        response.put("monthEnd", monthEnd);
                        return ResponseEntity.ok(response);
                    }
                } catch (Exception e) {
                    lastException = e;
                    continue;
                }
            }
        }

        // 所有嘗試都失敗了
        Map<String, Object> error = new HashMap<>();
        error.put("error", "無法獲取本月總評論數");
        if (lastException != null) {
            error.put("message", "查詢失敗: " + lastException.getMessage());
            lastException.printStackTrace();
        } else {
            error.put("message", "找不到 review 表或日期欄位");
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
