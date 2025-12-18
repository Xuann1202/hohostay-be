package tw.com.ispan.eeit.ho_back.moderation_actions;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
/*
 * @RequiredArgsConstructor 會自動生成建構子
 * public ModerationActionServiceImpl(ModerationActionRepository repo) {
 * this.repo = repo;
 * }
 */
@RequiredArgsConstructor
public class ModerationActionServiceImpl implements ModerationActionService {

    private final ModerationActionRepository repo;
    private final EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public Page<ModerationAction> list(Pageable pageable) {
        return repo.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ModerationAction> listAll(Sort sort) {
        return repo.findAll(sort);
    }

    @Override
    @Transactional(readOnly = true)
    public ModerationAction get(Long id) {
        return repo.findById(id).orElse(null);
    }

    @Override
    @Transactional
    public ModerationAction create(ModerationAction action) {
        return repo.save(action);
    }

    @Override
    @Transactional
    public ModerationAction update(Long id, ModerationAction updatedAction) {
        // 重新從資料庫查詢原始實體（確保 AuditAspect 能正確比較差異）
        ModerationAction existing = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Moderation action not found with id: " + id));

        // 將更新的欄位複製到原始實體
        // 注意：這裡接收的 updatedAction 已經在 Controller 中合併過了
        // 我們需要重新應用這些變更到新查詢的 existing 上
        existing.setReporterId(updatedAction.getReporterId());
        existing.setReviewId(updatedAction.getReviewId());
        existing.setReviewAuthorId(updatedAction.getReviewAuthorId());
        existing.setReason(updatedAction.getReason());
        existing.setActionTaken(updatedAction.getActionTaken());

        // 設置 moderatorId：優先使用 updatedAction 中的值（Controller 已設置）
        // 如果 updatedAction 中有 moderatorId，使用它；否則保持原有的值
        if (updatedAction.getModeratorId() != null) {
            existing.setModeratorId(updatedAction.getModeratorId());
            System.out.println("Service: 設置 moderatorId 為 " + updatedAction.getModeratorId());
        } else {
            System.out.println("Service: updatedAction 中的 moderatorId 為 null，保持原有值: " + existing.getModeratorId());
        }

        existing.setStatus(updatedAction.getStatus());
        existing.setMetadata(updatedAction.getMetadata());

        // 注意：reviewedAt 欄位由資料庫觸發器自動設置（insertable = false, updatable = false）
        // 當狀態變更為 RESOLVED 或 actionTaken 被設置時，資料庫會自動更新 reviewedAt
        // 因此不需要在 Java 代碼中手動設置

        // 獲取舊的 actionTaken 和 reviewId
        ActionTaken oldActionTaken = existing.getActionTaken();
        Long reviewId = existing.getReviewId();

        // 如果 actionTaken 變更為 HIDE，需要更新 review 表的 is_visible、reply_is_visible 和 comment
        if (updatedAction.getActionTaken() == ActionTaken.HIDE && reviewId != null) {
            updateReviewVisibility(reviewId, false);
            updateReviewContent(reviewId, "此評論因違規已被隱藏");
        }
        // 如果從 HIDE 變更為其他狀態（如 KEEP），恢復可見性（但評論內容無法恢復，因為原始內容已被替換）
        else if (oldActionTaken == ActionTaken.HIDE && updatedAction.getActionTaken() != ActionTaken.HIDE
                && reviewId != null) {
            updateReviewVisibility(reviewId, true);
            // 注意：評論內容已經被替換，無法恢復原始內容
        }

        // 儲存更新（AuditAspect 會自動記錄變更）
        return repo.save(existing);
    }

    /**
     * 更新 review 表的 is_visible 和 reply_is_visible 欄位
     * 
     * @param reviewId 評論ID
     * @param visible  true 表示顯示（設為1），false 表示隱藏（設為0）
     * 
     *                 實現說明：
     *                 - 此方法會嘗試多個可能的表名（review, reviews）以確保兼容性
     *                 - 欄位名稱假設為 is_visible 和 reply_is_visible（INT 類型，0/1 表示隱藏/顯示）
     *                 - 如果表結構不同，需要調整 SQL 語句
     *                 - 如果更新失敗，會記錄錯誤但不拋出異常（避免影響主流程）
     * 
     *                 注意：此方法使用原生 SQL 查詢，因為 review 表可能不在同一個模組中
     */
    private void updateReviewVisibility(Long reviewId, boolean visible) {
        int visibilityValue = visible ? 1 : 0;

        // 嘗試多個可能的表名
        String[] possibleTableNames = { "[review]", "review", "[reviews]", "reviews" };

        for (String tableName : possibleTableNames) {
            try {
                // 更新 is_visible 欄位
                String updateSql = "UPDATE " + tableName + " SET is_visible = :visibility WHERE id = :reviewId";
                Query updateQuery = entityManager.createNativeQuery(updateSql);
                updateQuery.setParameter("visibility", visibilityValue);
                updateQuery.setParameter("reviewId", reviewId);
                int updatedRows = updateQuery.executeUpdate();

                // 如果更新成功，也更新 reply_is_visible
                if (updatedRows > 0) {
                    String updateReplySql = "UPDATE " + tableName
                            + " SET reply_is_visible = :visibility WHERE id = :reviewId";
                    Query updateReplyQuery = entityManager.createNativeQuery(updateReplySql);
                    updateReplyQuery.setParameter("visibility", visibilityValue);
                    updateReplyQuery.setParameter("reviewId", reviewId);
                    updateReplyQuery.executeUpdate();
                    return; // 成功後退出
                }
            } catch (Exception e) {
                // 繼續嘗試下一個表名
                continue;
            }
        }

        // 如果所有嘗試都失敗，記錄錯誤但不拋出異常（避免影響主流程）
        System.err.println("警告：無法更新 review 表的可見性，reviewId: " + reviewId);
    }

    /**
     * 更新 review 表的 comment 欄位為替代文字
     * 
     * @param reviewId 評論ID
     * @param replacementText 替代文字
     * 
     *                 實現說明：
     *                 - 此方法會嘗試多個可能的表名（review, reviews）以確保兼容性
     *                 - 欄位名稱假設為 comment（VARCHAR/NVARCHAR 類型）
     *                 - 如果表結構不同，需要調整 SQL 語句
     *                 - 如果更新失敗，會記錄錯誤但不拋出異常（避免影響主流程）
     * 
     *                 注意：此方法使用原生 SQL 查詢，因為 review 表可能不在同一個模組中
     */
    private void updateReviewContent(Long reviewId, String replacementText) {
        // 嘗試多個可能的表名
        String[] possibleTableNames = { "[review]", "review", "[reviews]", "reviews" };

        for (String tableName : possibleTableNames) {
            try {
                // 更新 comment 欄位
                String updateSql = "UPDATE " + tableName + " SET comment = :replacementText WHERE id = :reviewId";
                Query updateQuery = entityManager.createNativeQuery(updateSql);
                updateQuery.setParameter("replacementText", replacementText);
                updateQuery.setParameter("reviewId", reviewId);
                int updatedRows = updateQuery.executeUpdate();

                if (updatedRows > 0) {
                    System.out.println("✓ 已將評論內容替換為替代文字，reviewId: " + reviewId);
                    return; // 成功後退出
                }
            } catch (Exception e) {
                System.err.println("嘗試更新評論內容失敗（表名: " + tableName + "）: " + e.getMessage());
                // 繼續嘗試下一個表名
                continue;
            }
        }

        // 如果所有嘗試都失敗，記錄錯誤但不拋出異常（避免影響主流程）
        System.err.println("警告：無法更新 review 表的評論內容，reviewId: " + reviewId);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        repo.deleteById(id);
    }

}
