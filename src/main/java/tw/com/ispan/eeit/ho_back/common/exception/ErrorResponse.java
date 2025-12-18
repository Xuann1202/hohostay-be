package tw.com.ispan.eeit.ho_back.common.exception;

import java.time.LocalDateTime;
import java.util.Map;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    private LocalDateTime timestamp; // 例：2025-11-10T15:30:21.529
    private int status; // 例：404
    private String error; // 例："Not Found"
    private String message; // 例："No ModerationAction found with id=5"
    private String path; // 例："/api/moderation-actions/5"

    // optional: field -> message map for validation errors
    private Map<String, String> errors;
}
