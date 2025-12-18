package tw.com.ispan.eeit.ho_back.audit_logs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.util.*;

/**
 * 審計日誌差異比較輔助工具
 * 用於在儲存到資料庫前，只保留有變更的欄位
 */
public class AuditDiffHelper {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    /**
     * 比較兩個 JSON 字串，只保留有差異的欄位
     * 
     * @param oldJson 舊值 JSON
     * @param newJson 新值 JSON
     * @return 陣列 [簡化的 oldJson, 簡化的 newJson]，只包含有變更的欄位
     */
    public static String[] extractChangedFields(String oldJson, String newJson) {
        try {
            System.out.println("\n=== AuditDiffHelper.extractChangedFields ===");
            System.out.println("oldJson: " + oldJson);
            System.out.println("newJson: " + newJson);

            if (oldJson == null || oldJson.isBlank() || "null".equals(oldJson)) {
                // INSERT 操作：保留所有新值
                System.out.println("判定為 INSERT 操作");
                return new String[] { null, newJson };
            }

            if (newJson == null || newJson.isBlank() || "null".equals(newJson)) {
                // DELETE 操作：保留所有舊值
                System.out.println("判定為 DELETE 操作");
                return new String[] { oldJson, null };
            }

            System.out.println("判定為 UPDATE 操作，開始比較差異");
            JsonNode oldNode = MAPPER.readTree(oldJson);
            JsonNode newNode = MAPPER.readTree(newJson);

            ObjectNode simplifiedOld = MAPPER.createObjectNode();
            ObjectNode simplifiedNew = MAPPER.createObjectNode();

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

            boolean hasChanges = false;

            // 定義需要排除的關聯關係欄位（這些欄位的變更不應該觸發主表的 update 記錄）
            Set<String> excludedFields = new HashSet<>(Arrays.asList(
                    "roles", "users", "hotels", "bookings", "wishList", 
                    "bookingInventories", "hotels", "reviews", "photos",
                    "inventories", "facilities", "roomTypes", "bedTypes"
            ));

            for (String fieldName : allFields) {
                // 跳過關聯關係欄位，這些欄位的變更應該記錄為對中間表的操作，而不是主表的 update
                if (excludedFields.contains(fieldName)) {
                    System.out.println("  跳過關聯關係欄位 [" + fieldName + "]（應記錄為中間表操作）");
                    continue;
                }

                JsonNode oldFieldValue = oldNode.get(fieldName);
                JsonNode newFieldValue = newNode.get(fieldName);

                // 比較欄位值是否有變更
                if (!Objects.equals(oldFieldValue, newFieldValue)) {
                    hasChanges = true;
                    System.out.println("  欄位 [" + fieldName + "] 有變更：" + oldFieldValue + " -> " + newFieldValue);

                    // 保留有變更的欄位（包括 null 值）
                    if (oldFieldValue != null && !oldFieldValue.isNull()) {
                        simplifiedOld.set(fieldName, oldFieldValue);
                    } else {
                        simplifiedOld.putNull(fieldName);
                    }

                    if (newFieldValue != null && !newFieldValue.isNull()) {
                        simplifiedNew.set(fieldName, newFieldValue);
                    } else {
                        simplifiedNew.putNull(fieldName);
                    }
                }
            }

            if (!hasChanges) {
                // 如果只有關聯關係變更（已被排除），不應該記錄為 update 操作
                // 這種情況應該在 AuditAspect 中被處理，記錄為對中間表的操作
                System.out.println("⚠️ 警告：UPDATE 操作但沒有檢測到實體欄位變更（可能只有關聯關係變更）");
                System.out.println("⚠️ 建議：這種操作應該記錄為對中間表（如 user_role）的 insert/delete，而不是對主表的 update");
                // 返回空 JSON，表示沒有實體欄位變更
                return new String[] { "{}", "{}" };
            }

            String simplifiedOldJson = simplifiedOld.size() > 0 ? MAPPER.writeValueAsString(simplifiedOld) : "{}";
            String simplifiedNewJson = simplifiedNew.size() > 0 ? MAPPER.writeValueAsString(simplifiedNew) : "{}";

            System.out.println("✅ UPDATE 簡化完成：oldValue=" + simplifiedOldJson + ", newValue=" + simplifiedNewJson);
            return new String[] { simplifiedOldJson, simplifiedNewJson };

        } catch (Exception e) {
            System.err.println("Failed to extract changed fields: " + e.getMessage());
            e.printStackTrace();
            // 發生錯誤時，返回原始 JSON
            return new String[] { oldJson, newJson };
        }
    }

    /**
     * 簡化 INSERT 操作的 JSON
     * 移除 null 值和不必要的欄位
     */
    public static String simplifyInsertJson(String json) {
        try {
            if (json == null || json.isBlank()) {
                return null;
            }

            JsonNode node = MAPPER.readTree(json);
            ObjectNode simplified = MAPPER.createObjectNode();

            // 使用 fieldNames 遍歷所有欄位
            Iterator<String> fieldNames = node.fieldNames();
            while (fieldNames.hasNext()) {
                String fieldName = fieldNames.next();
                JsonNode value = node.get(fieldName);

                // 跳過 null 值和時間戳欄位（通常由資料庫自動生成）
                if (value != null && !value.isNull() &&
                        !fieldName.equals("createdAt") &&
                        !fieldName.equals("updatedAt") &&
                        !fieldName.equals("reviewedAt")) {
                    simplified.set(fieldName, value);
                }
            }

            return simplified.size() > 0 ? MAPPER.writeValueAsString(simplified) : null;

        } catch (Exception e) {
            System.err.println("Failed to simplify insert JSON: " + e.getMessage());
            return json;
        }
    }

    /**
     * 檢測關聯關係的變更（用於記錄中間表的操作）
     * 
     * @param oldJson 舊值 JSON
     * @param newJson 新值 JSON
     * @param associationField 關聯關係欄位名稱（如 "roles"）
     * @return 如果有關聯關係變更，返回變更信息；否則返回 null
     */
    public static AssociationChange detectAssociationChange(String oldJson, String newJson, String associationField) {
        try {
            if (oldJson == null || oldJson.isBlank() || "null".equals(oldJson)) {
                return null; // 新增操作，不處理關聯關係
            }
            if (newJson == null || newJson.isBlank() || "null".equals(newJson)) {
                return null; // 刪除操作，不處理關聯關係
            }

            JsonNode oldNode = MAPPER.readTree(oldJson);
            JsonNode newNode = MAPPER.readTree(newJson);

            JsonNode oldAssoc = oldNode.get(associationField);
            JsonNode newAssoc = newNode.get(associationField);

            // 如果關聯關係欄位不存在，返回 null
            if (oldAssoc == null && newAssoc == null) {
                return null;
            }

            // 比較關聯關係
            if (Objects.equals(oldAssoc, newAssoc)) {
                return null; // 沒有變更
            }

            // 提取變更信息
            Set<String> oldIds = extractIdsFromArray(oldAssoc);
            Set<String> newIds = extractIdsFromArray(newAssoc);

            Set<String> added = new HashSet<>(newIds);
            added.removeAll(oldIds);

            Set<String> removed = new HashSet<>(oldIds);
            removed.removeAll(newIds);

            if (added.isEmpty() && removed.isEmpty()) {
                return null; // 實際上沒有變更（可能是順序不同）
            }

            return new AssociationChange(added, removed);
        } catch (Exception e) {
            System.err.println("Failed to detect association change: " + e.getMessage());
            return null;
        }
    }

    /**
     * 從 JSON 數組中提取 ID
     */
    private static Set<String> extractIdsFromArray(JsonNode arrayNode) {
        Set<String> ids = new HashSet<>();
        if (arrayNode == null || !arrayNode.isArray()) {
            return ids;
        }
        for (JsonNode item : arrayNode) {
            if (item.isObject() && item.has("id")) {
                ids.add(item.get("id").asText());
            } else if (item.isNumber()) {
                ids.add(item.asText());
            }
        }
        return ids;
    }

    /**
     * 關聯關係變更信息
     */
    public static class AssociationChange {
        public final Set<String> added;
        public final Set<String> removed;

        public AssociationChange(Set<String> added, Set<String> removed) {
            this.added = added;
            this.removed = removed;
        }

        public boolean hasChanges() {
            return !added.isEmpty() || !removed.isEmpty();
        }
    }

    /**
     * 簡化 DELETE 操作的 JSON
     * 只保留關鍵欄位（ID 和重要的業務欄位）
     */
    public static String simplifyDeleteJson(String json) {
        try {
            if (json == null || json.isBlank()) {
                return null;
            }

            JsonNode node = MAPPER.readTree(json);
            ObjectNode simplified = MAPPER.createObjectNode();

            // 保留所有欄位（DELETE 時通常需要完整記錄）
            Iterator<String> fieldNames = node.fieldNames();
            while (fieldNames.hasNext()) {
                String fieldName = fieldNames.next();
                simplified.set(fieldName, node.get(fieldName));
            }

            return MAPPER.writeValueAsString(simplified);

        } catch (Exception e) {
            System.err.println("Failed to simplify delete JSON: " + e.getMessage());
            return json;
        }
    }
}
