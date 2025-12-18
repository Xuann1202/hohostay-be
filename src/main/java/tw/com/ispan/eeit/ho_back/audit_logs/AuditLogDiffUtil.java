package tw.com.ispan.eeit.ho_back.audit_logs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.*;

/**
 * 審計日誌差異比較工具
 * 用於比較 JSON 的前後差異，提取變更的欄位
 */
public class AuditLogDiffUtil {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    /**
     * 將完整的 AuditLog 轉換為欄位級別的變更列表
     * 
     * @param log 原始審計日誌
     * @return 欄位變更列表（每個欄位一筆）
     */
    public static List<AuditLogFieldChangeResponse> extractFieldChanges(AuditLog log) {
        List<AuditLogFieldChangeResponse> changes = new ArrayList<>();

        try {
            String actionType = log.getActionType();
            
            if ("insert".equalsIgnoreCase(actionType) || "create".equalsIgnoreCase(actionType)) {
                // 新增：所有 newValue 的欄位都算是變更
                extractInsertChanges(log, changes);
                
            } else if ("update".equalsIgnoreCase(actionType)) {
                // 更新：比較前後差異
                extractUpdateChanges(log, changes);
                
            } else if ("delete".equalsIgnoreCase(actionType)) {
                // 刪除：所有 oldValue 的欄位都算是變更
                extractDeleteChanges(log, changes);
            } else {
                // 未知操作類型，返回 fallback response
                changes.add(createFallbackResponse(log));
            }
            
        } catch (Exception e) {
            // 如果解析失敗，返回一筆原始記錄
            System.err.println("Failed to parse audit log changes: " + e.getMessage());
            changes.add(createFallbackResponse(log));
        }

        // 只有在解析失敗或未知操作類型時才使用 fallback
        // 如果 changes 為空且不是因為解析失敗，說明確實沒有變更，不需要 fallback
        return changes;
    }

    /**
     * 提取新增操作的變更（顯示所有欄位，包括 null 值）
     */
    private static void extractInsertChanges(AuditLog log, List<AuditLogFieldChangeResponse> changes) throws Exception {
        // 如果 newValue 為 null 或空白，仍然顯示基本信息
        if (log.getNewValue() == null || log.getNewValue().isBlank()) {
            // 即使沒有 newValue，也顯示一條記錄表示新增操作
            changes.add(AuditLogFieldChangeResponse.builder()
                    .auditLogId(log.getId())
                    .actorUserId(log.getActorUserId())
                    .actionType(log.getActionType())
                    .targetTable(log.getTargetTable())
                    .targetId(log.getTargetId())
                    .fieldName("(新增記錄)")
                    .oldValue(null)
                    .newValue(null)
                    .ipAddress(log.getIpAddress())
                    .createdAt(log.getCreatedAt())
                    .build());
            return;
        }

        JsonNode newNode = MAPPER.readTree(log.getNewValue());
        Iterator<String> fieldNames = newNode.fieldNames();
        
        boolean hasFields = false;

        while (fieldNames.hasNext()) {
            String fieldName = fieldNames.next();
            JsonNode valueNode = newNode.get(fieldName);
            hasFields = true;

            // 不再跳過 null 值，顯示所有欄位（包括 null）
            changes.add(AuditLogFieldChangeResponse.builder()
                    .auditLogId(log.getId())
                    .actorUserId(log.getActorUserId())
                    .actionType(log.getActionType())
                    .targetTable(log.getTargetTable())
                    .targetId(log.getTargetId())
                    .fieldName(fieldName)
                    .oldValue(null)  // 新增操作，舊值始終為 null
                    .newValue(formatValue(valueNode))  // 即使為 null 也顯示
                    .ipAddress(log.getIpAddress())
                    .createdAt(log.getCreatedAt())
                    .build());
        }
        
        // 如果 JSON 解析後沒有任何欄位，至少顯示一條記錄
        if (!hasFields) {
            changes.add(AuditLogFieldChangeResponse.builder()
                    .auditLogId(log.getId())
                    .actorUserId(log.getActorUserId())
                    .actionType(log.getActionType())
                    .targetTable(log.getTargetTable())
                    .targetId(log.getTargetId())
                    .fieldName("(新增記錄)")
                    .oldValue(null)
                    .newValue(log.getNewValue())  // 顯示原始 newValue
                    .ipAddress(log.getIpAddress())
                    .createdAt(log.getCreatedAt())
                    .build());
        }
    }

    /**
     * 提取更新操作的變更（只顯示有差異的欄位）
     */
    private static void extractUpdateChanges(AuditLog log, List<AuditLogFieldChangeResponse> changes) throws Exception {
        if (log.getOldValue() == null || log.getNewValue() == null) {
            changes.add(createFallbackResponse(log));
            return;
        }

        JsonNode oldNode = MAPPER.readTree(log.getOldValue());
        JsonNode newNode = MAPPER.readTree(log.getNewValue());

        // 收集所有欄位名稱
        Set<String> allFields = new HashSet<>();
        Iterator<String> oldFieldNames = oldNode.fieldNames();
        while (oldFieldNames.hasNext()) {
            allFields.add(oldFieldNames.next());
        }
        Iterator<String> newFieldNames = newNode.fieldNames();
        while (newFieldNames.hasNext()) {
            allFields.add(newFieldNames.next());
        }

        for (String fieldName : allFields) {
            JsonNode oldFieldValue = oldNode.get(fieldName);
            JsonNode newFieldValue = newNode.get(fieldName);

            // 使用更可靠的 JSON 比較方法
            if (!isJsonNodeEqual(oldFieldValue, newFieldValue)) {
                changes.add(AuditLogFieldChangeResponse.builder()
                        .auditLogId(log.getId())
                        .actorUserId(log.getActorUserId())
                        .actionType(log.getActionType())
                        .targetTable(log.getTargetTable())
                        .targetId(log.getTargetId())
                        .fieldName(fieldName)
                        .oldValue(formatValue(oldFieldValue))
                        .newValue(formatValue(newFieldValue))
                        .ipAddress(log.getIpAddress())
                        .createdAt(log.getCreatedAt())
                        .build());
            }
        }
        
        // 如果沒有找到任何變更，顯示提示信息而不是完整的 JSON
        if (changes.isEmpty()) {
            changes.add(AuditLogFieldChangeResponse.builder()
                    .auditLogId(log.getId())
                    .actorUserId(log.getActorUserId())
                    .actionType(log.getActionType())
                    .targetTable(log.getTargetTable())
                    .targetId(log.getTargetId())
                    .fieldName("(無變更)")
                    .oldValue("所有欄位值保持不變")
                    .newValue("所有欄位值保持不變")
                    .ipAddress(log.getIpAddress())
                    .createdAt(log.getCreatedAt())
                    .build());
        }
    }
    
    /**
     * 比較兩個 JsonNode 是否相等
     * 使用 JsonNode.equals() 方法，它會正確比較 JSON 內容（忽略順序）
     */
    private static boolean isJsonNodeEqual(JsonNode node1, JsonNode node2) {
        if (node1 == null && node2 == null) {
            return true;
        }
        if (node1 == null || node2 == null) {
            return false;
        }
        // JsonNode.equals() 會正確比較 JSON 內容，包括嵌套結構
        return node1.equals(node2);
    }

    /**
     * 提取刪除操作的變更（顯示所有被刪除的欄位）
     */
    private static void extractDeleteChanges(AuditLog log, List<AuditLogFieldChangeResponse> changes) throws Exception {
        if (log.getOldValue() == null || log.getOldValue().isBlank()) {
            changes.add(createFallbackResponse(log));
            return;
        }

        JsonNode oldNode = MAPPER.readTree(log.getOldValue());
        Iterator<String> fieldNames = oldNode.fieldNames();

        while (fieldNames.hasNext()) {
            String fieldName = fieldNames.next();
            JsonNode valueNode = oldNode.get(fieldName);

            changes.add(AuditLogFieldChangeResponse.builder()
                    .auditLogId(log.getId())
                    .actorUserId(log.getActorUserId())
                    .actionType(log.getActionType())
                    .targetTable(log.getTargetTable())
                    .targetId(log.getTargetId())
                    .fieldName(fieldName)
                    .oldValue(formatValue(valueNode))
                    .newValue(null)
                    .ipAddress(log.getIpAddress())
                    .createdAt(log.getCreatedAt())
                    .build());
        }
    }

    /**
     * 格式化 JSON 值為字串
     */
    private static String formatValue(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isTextual()) {
            return node.asText();
        }
        if (node.isNumber()) {
            return node.asText(); // 保持數字格式一致
        }
        if (node.isBoolean()) {
            return String.valueOf(node.asBoolean());
        }
        // 對於對象和數組，使用 toString() 保持原始格式
        return node.toString();
    }

    /**
     * 當解析失敗時，返回一筆完整的記錄作為備用
     */
    private static AuditLogFieldChangeResponse createFallbackResponse(AuditLog log) {
        return AuditLogFieldChangeResponse.builder()
                .auditLogId(log.getId())
                .actorUserId(log.getActorUserId())
                .actionType(log.getActionType())
                .targetTable(log.getTargetTable())
                .targetId(log.getTargetId())
                .fieldName("(全部)")
                .oldValue(log.getOldValue())
                .newValue(log.getNewValue())
                .ipAddress(log.getIpAddress())
                .createdAt(log.getCreatedAt())
                .build();
    }
}

