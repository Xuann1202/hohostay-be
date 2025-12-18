package tw.com.ispan.eeit.ho_back.moderation_actions;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Status enum 的自定義轉換器
 * 用於處理資料庫中可能存在的舊資料值（如 "NULL" 或中文值）
 */
@Converter
public class StatusConverter implements AttributeConverter<Status, String> {

    @Override
    public String convertToDatabaseColumn(Status status) {
        if (status == null) {
            return null;
        }
        // 存儲時使用 enum 名稱
        return status.name();
    }

    @Override
    public Status convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return Status.PENDING;
        }
        // 讀取時使用自定義的 fromString 方法來處理舊資料值
        return Status.fromString(dbData);
    }
}

