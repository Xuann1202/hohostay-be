package tw.com.ispan.eeit.ho_back.moderation_actions;

public enum Status {
    PENDING("待處理"),
    RESOLVED("已完成");

    private final String description;

    Status(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 從字串轉換為 Status enum，支援舊的資料值
     * 如果無法識別，返回 PENDING 作為預設值
     */
    public static Status fromString(String value) {
        if (value == null || value.isBlank() || "NULL".equalsIgnoreCase(value)) {
            return PENDING;
        }

        String trimmed = value.trim();

        // 先嘗試直接匹配 enum 名稱（不區分大小寫）
        try {
            return Status.valueOf(trimmed.toUpperCase());
        } catch (IllegalArgumentException e) {
            // 如果直接匹配失敗，嘗試匹配描述或舊的資料值
        }

        // 匹配描述（中文）
        for (Status status : Status.values()) {
            if (status.getDescription().equals(trimmed)) {
                return status;
            }
        }

        // 處理舊的資料值映射
        switch (trimmed) {
            case "待審查":
            case "待處理":
            case "待審核":
                return PENDING;
            case "已審查":
            case "已處理":
            case "已完成":
            case "已解決":
                return RESOLVED;
            default:
                // 如果都無法匹配，返回 PENDING
                return PENDING;
        }
    }
}