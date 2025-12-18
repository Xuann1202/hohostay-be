package tw.com.ispan.eeit.ho_back.moderation_actions;

public enum Reason {
    ABUSE("辱罵字眼"),
    SPAM("垃圾內容"),
    ADVERTISING("廣告訊息"),
    OFFENSIVE("冒犯性內容"),
    OTHER("其他");

    private final String description;

    Reason(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 從字串轉換為 Reason enum，支援舊的資料值
     * 如果無法識別，返回 OTHER 作為預設值
     */
    public static Reason fromString(String value) {
        if (value == null || value.isBlank()) {
            return OTHER;
        }

        String trimmed = value.trim();

        // 先嘗試直接匹配 enum 名稱（不區分大小寫）
        try {
            return Reason.valueOf(trimmed.toUpperCase());
        } catch (IllegalArgumentException e) {
            // 如果直接匹配失敗，嘗試匹配描述或舊的資料值
        }

        // 匹配描述（中文）
        for (Reason reason : Reason.values()) {
            if (reason.getDescription().equals(trimmed)) {
                return reason;
            }
        }

        // 處理舊的資料值映射
        switch (trimmed) {
            case "惡意評論":
            case "疑似惡意評論":
                return ABUSE;
            case "評論內容不實":
            case "評論內容不當":
            case "不實資訊":
            case "不實":
                return OFFENSIVE;
            case "辱罵":
                return ABUSE;
            case "垃圾":
                return SPAM;
            case "廣告":
                return ADVERTISING;
            default:
                // 如果都無法匹配，返回 OTHER
                return OTHER;
        }
    }
}
