package tw.com.ispan.eeit.ho_back.moderation_actions;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * ActionTaken enum 的自定義轉換器
 * 用於處理資料庫中可能存在的舊資料值（如 "NULL"）
 */
@Converter
public class ActionTakenConverter implements AttributeConverter<ActionTaken, String> {

    @Override
    public String convertToDatabaseColumn(ActionTaken actionTaken) {
        if (actionTaken == null) {
            return null;
        }
        // 存儲時使用 enum 名稱
        return actionTaken.name();
    }

    @Override
    public ActionTaken convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return ActionTaken.NONE;
        }
        // 讀取時使用自定義的 fromString 方法來處理舊資料值
        return ActionTaken.fromString(dbData);
    }
}

