package tw.com.ispan.eeit.ho_back.audit_logs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.lang.reflect.Field;
import java.util.Optional;

public class AuditSupport {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())  // 支援 Java 8 日期時間類型
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);  // 使用 ISO-8601 格式

    public static Optional<Object> extractId(Object entity) {
        for (Field f : entity.getClass().getDeclaredFields()) {
            if (f.isAnnotationPresent(Id.class) || f.isAnnotationPresent(EmbeddedId.class)) {
                f.setAccessible(true);
                try {
                    return Optional.ofNullable(f.get(entity));
                } catch (IllegalAccessException ignored) {
                }
            }
        }
        return Optional.empty();
    }

    public static String resolveTableName(Class<?> entityClass) {
        Table t = entityClass.getAnnotation(Table.class);
        return (t != null && !t.name().isBlank()) ? t.name() : entityClass.getSimpleName();
    }

    public static String toJson(Object obj) {
        if (obj == null) {
            System.out.println("=== toJson: 物件為 null ===");
            return null;
        }
        try {
            System.out.println("=== toJson: 開始序列化 " + obj.getClass().getSimpleName() + " ===");
            String json = MAPPER.writeValueAsString(obj);
            System.out.println("=== toJson: 序列化成功，長度: " + json.length() + " ===");
            return json;
        } catch (Exception e) {
            System.err.println("=== toJson: 序列化失敗！ ===");
            System.err.println("=== 物件類型: " + obj.getClass().getName() + " ===");
            System.err.println("=== 錯誤: " + e.getMessage() + " ===");
            e.printStackTrace();
            return null;
        }
    }
}
