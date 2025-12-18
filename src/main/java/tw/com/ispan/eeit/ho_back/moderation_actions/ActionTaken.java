package tw.com.ispan.eeit.ho_back.moderation_actions;

public enum ActionTaken {
    NONE("尚未處理"),
    KEEP("保留評論"),
    DELETE("刪除評論"),
    HIDE("隱藏評論"),
    DELETE_REVIEW("刪除評論"),
    WARNING("警告"),
    BAN_USER("封鎖用戶");

    private final String description;

    ActionTaken(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 從字串轉換為 ActionTaken enum，支援舊的資料值
     * 如果無法識別，返回 NONE 作為預設值
     */
    public static ActionTaken fromString(String value) {
        if (value == null || value.isBlank() || "NULL".equalsIgnoreCase(value)) {
            return NONE;
        }

        String trimmed = value.trim();

        // 先嘗試直接匹配 enum 名稱（不區分大小寫）
        try {
            return ActionTaken.valueOf(trimmed.toUpperCase());
        } catch (IllegalArgumentException e) {
            // 如果直接匹配失敗，嘗試匹配描述或舊的資料值
        }

        // 匹配描述（中文）
        for (ActionTaken action : ActionTaken.values()) {
            if (action.getDescription().equals(trimmed)) {
                return action;
            }
        }

        // 處理舊的資料值映射
        switch (trimmed) {
            case "保留":
            case "保留評論":
                return KEEP;
            case "刪除":
            case "刪除評論":
                return DELETE;
            case "隱藏":
            case "隱藏評論":
                return HIDE;
            case "尚未處理":
            case "尚未審查":
            case "未處理":
                return NONE;
            default:
                // 如果都無法匹配，返回 NONE
                return NONE;
        }
    }
}