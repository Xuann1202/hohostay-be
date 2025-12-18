package tw.com.ispan.eeit.ho_back.moderation_actions;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Reason enum 的自定義轉換器
 * 用於處理資料庫中可能存在的舊資料值（如 "惡意評論"）
 */
@Converter
public class ReasonConverter implements AttributeConverter<Reason, String> {

    @Override
    public String convertToDatabaseColumn(Reason reason) {
        if (reason == null) {
            return null;
        }
        // 存儲時使用 enum 名稱
        return reason.name();
    }

    @Override
    public Reason convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return Reason.OTHER;
        }
        // 讀取時使用自定義的 fromString 方法來處理舊資料值
        return Reason.fromString(dbData);
    }
}

