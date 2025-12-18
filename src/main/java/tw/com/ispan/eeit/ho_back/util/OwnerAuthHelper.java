package tw.com.ispan.eeit.ho_back.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tw.com.ispan.eeit.ho_back.role.Role;
import tw.com.ispan.eeit.ho_back.user.User;
import tw.com.ispan.eeit.ho_back.user.UserService;

import java.util.List;

/**
 * 房東認證輔助工具類
 * 用於從請求中獲取用戶信息和檢查房東權限
 * 使用JsonWebTokenUtils驗證token
 */
@Component
public class OwnerAuthHelper {

    @Autowired
    private UserService userService;

    @Autowired
    private JsonWebTokenUtils jsonWebTokenUtils;

    /**
     * 從請求中獲取用戶 ID
     * 
     * 優先順序：
     * 1. request attribute "userId"（由 JwtInterceptor 設置）
     * 2. X-Dev-User-Id header（開發/測試模式）
     * 3. userId header（前端使用）
     * 4. Authorization Bearer token（從 JWT token 解析，如果解析失敗則跳過）
     */
    public Integer getUserIdFromRequest(HttpServletRequest request) {
        // 優先從 request attribute 獲取（由 JwtInterceptor 或其他 Filter 設置）
        Object userIdAttr = request.getAttribute("userId");
        if (userIdAttr != null) {
            try {
                Integer userId = userIdAttr instanceof Integer ? (Integer) userIdAttr
                        : Integer.parseInt(userIdAttr.toString());
                if (userId > 0) {
                    System.out.println("OwnerAuthHelper - 從 request attribute 獲取到 userId: " + userId);
                    return userId;
                }
            } catch (Exception e) {
                // 忽略格式錯誤
            }
        }

        // 優先從 X-Dev-User-Id header 讀取（開發/測試模式）
        String devUserId = request.getHeader("X-Dev-User-Id");
        if (devUserId != null && !devUserId.trim().isEmpty()) {
            try {
                Integer userId = Integer.parseInt(devUserId.trim());
                if (userId > 0) {
                    System.out.println("OwnerAuthHelper - 從 X-Dev-User-Id header 獲取到 userId: " + userId);
                    return userId;
                }
            } catch (NumberFormatException e) {
                // 忽略格式錯誤
            }
        }

        // 直接從 userId header 讀取（前端使用）
        String userIdHeader = request.getHeader("userId");
        if (userIdHeader != null && !userIdHeader.trim().isEmpty()) {
            try {
                Integer userId = Integer.parseInt(userIdHeader.trim());
                if (userId > 0) {
                    System.out.println("OwnerAuthHelper - 從 userId header 獲取到 userId: " + userId);
                    return userId;
                }
            } catch (NumberFormatException e) {
                // 忽略格式錯誤
            }
        }

        // 從 Authorization header 獲取 token（前端使用）
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim(); // 移除 "Bearer " 前綴
            if (token != null && !token.isEmpty()) {
                try {
                    // 使用 JsonWebTokenUtils 驗證 token
                    String userJson = jsonWebTokenUtils.validateEncryptedToken(token);
                    if (userJson != null && !userJson.trim().isEmpty()) {
                        // 嘗試解析為 JSON
                        try {
                            com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
                            com.fasterxml.jackson.databind.JsonNode jsonNode = objectMapper.readTree(userJson);
                            if (jsonNode.has("id")) {
                                Integer userId = jsonNode.get("id").asInt();
                                System.out.println("OwnerAuthHelper - 從 JWT token 解析出 userId: " + userId);
                                return userId;
                            }
                        } catch (Exception jsonException) {
                            // 如果不是 JSON 格式，嘗試從 toString() 格式解析
                            // 格式：user [id=1, email=xxx, ...]
                            if (userJson.contains("id=")) {
                                try {
                                    // 使用正則表達式提取 id
                                    java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("id=(\\d+)");
                                    java.util.regex.Matcher matcher = pattern.matcher(userJson);
                                    if (matcher.find()) {
                                        Integer userId = Integer.parseInt(matcher.group(1));
                                        System.out.println("OwnerAuthHelper - 從 toString() 格式解析出 userId: " + userId);
                                        return userId;
                                    }
                                } catch (Exception parseException) {
                                    System.err.println("OwnerAuthHelper - 解析 userJson 失敗: " + userJson);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    // JWT token 解析失敗（可能是密鑰不匹配、token 格式錯誤等）
                    // 不拋出異常，繼續嘗試其他方式
                    System.err.println("OwnerAuthHelper - 驗證 Authorization token 失敗: " + e.getClass().getSimpleName()
                            + ": " + e.getMessage());
                    // 如果是解密失敗，可能是 token 格式或密鑰問題，記錄但不中斷流程
                    if (e.getCause() != null) {
                        System.err.println("OwnerAuthHelper - 根本原因: " + e.getCause().getClass().getSimpleName() + ": "
                                + e.getCause().getMessage());
                    }
                }
            }
        }

        // 以下為註解掉的 Cookie token 驗證邏輯（保留以便將來需要時恢復）
        /*
         * // 優先從 Authorization header 獲取 token（前端使用）
         * String authHeader = request.getHeader("Authorization");
         * System.out.println("OwnerAuthHelper - Authorization header: " + (authHeader
         * != null ? "存在" : "不存在"));
         * if (authHeader != null && authHeader.startsWith("Bearer ")) {
         * String token = authHeader.substring(7).trim(); // 移除 "Bearer " 前綴並去除空白
         * System.out.println("OwnerAuthHelper - Token 長度: " + token.length());
         * if (token != null && !token.isEmpty()) {
         * try {
         * // 使用 JsonWebTokenUtils 驗證 token
         * String userJson = jsonWebTokenUtils.validateEncryptedToken(token);
         * System.out.println("OwnerAuthHelper - validateEncryptedToken 返回: "
         * + (userJson != null ? userJson.substring(0, Math.min(100, userJson.length()))
         * + "..."
         * : "null"));
         * if (userJson != null && !userJson.trim().isEmpty()) {
         * // 嘗試解析為 JSON
         * try {
         * ObjectMapper objectMapper = new ObjectMapper();
         * JsonNode jsonNode = objectMapper.readTree(userJson);
         * if (jsonNode.has("id")) {
         * Integer userId = jsonNode.get("id").asInt();
         * System.out.println("OwnerAuthHelper - 從 JSON 解析出 userId: " + userId);
         * return userId;
         * }
         * } catch (Exception jsonException) {
         * // 如果不是 JSON 格式，嘗試從 toString() 格式解析
         * // 格式：user [id=1, email=xxx, ...]
         * System.out.println("OwnerAuthHelper - JSON 解析失敗，嘗試 toString() 格式");
         * if (userJson.contains("id=")) {
         * try {
         * // 使用正則表達式提取 id
         * java.util.regex.Pattern pattern =
         * java.util.regex.Pattern.compile("id=(\\d+)");
         * java.util.regex.Matcher matcher = pattern.matcher(userJson);
         * if (matcher.find()) {
         * Integer userId = Integer.parseInt(matcher.group(1));
         * System.out.println("OwnerAuthHelper - 從 toString() 格式解析出 userId: " + userId);
         * return userId;
         * }
         * } catch (Exception parseException) {
         * // 解析失敗，繼續嘗試 Cookie
         * System.err.println("OwnerAuthHelper - 解析 userJson 失敗: " + userJson);
         * parseException.printStackTrace();
         * }
         * } else {
         * System.err.println("OwnerAuthHelper - userJson 格式不符合預期: " + userJson);
         * }
         * }
         * } else {
         * System.err.println("OwnerAuthHelper - validateEncryptedToken 返回空值");
         * }
         * } catch (Exception e) {
         * // Token 無效或解析失敗，繼續嘗試 Cookie
         * System.err.println("OwnerAuthHelper - 驗證 Authorization token 失敗: " +
         * e.getMessage());
         * e.printStackTrace();
         * }
         * }
         * } else {
         * // 調試：檢查是否有 Authorization header
         * if (authHeader != null) {
         * System.err.println("OwnerAuthHelper - Authorization header 格式不正確: "
         * + authHeader.substring(0, Math.min(50, authHeader.length())));
         * } else {
         * System.out.println("OwnerAuthHelper - 沒有 Authorization header");
         * }
         * }
         * 
         * // 從 Cookie 中獲取 token（後端傳統方式）
         * Cookie[] cookies = request.getCookies();
         * if (cookies == null) {
         * return null;
         * }
         * 
         * for (Cookie cookie : cookies) {
         * if ("token".equals(cookie.getName())) {
         * String token = cookie.getValue();
         * if (token != null && !token.trim().isEmpty()) {
         * try {
         * // 使用組員的 JsonWebTokenUtils 驗證 token
         * String userJson = jsonWebTokenUtils.validateEncryptedToken(token);
         * if (userJson == null || userJson.trim().isEmpty()) {
         * return null;
         * }
         * 
         * // 嘗試解析為 JSON
         * try {
         * ObjectMapper objectMapper = new ObjectMapper();
         * JsonNode jsonNode = objectMapper.readTree(userJson);
         * if (jsonNode.has("id")) {
         * return jsonNode.get("id").asInt();
         * }
         * } catch (Exception jsonException) {
         * // 如果不是 JSON 格式，嘗試從 toString() 格式解析
         * // 格式：user [id=1, email=xxx, ...]
         * if (userJson.contains("id=")) {
         * try {
         * // 使用正則表達式提取 id
         * java.util.regex.Pattern pattern =
         * java.util.regex.Pattern.compile("id=(\\d+)");
         * java.util.regex.Matcher matcher = pattern.matcher(userJson);
         * if (matcher.find()) {
         * return Integer.parseInt(matcher.group(1));
         * }
         * } catch (Exception parseException) {
         * // 解析失敗
         * return null;
         * }
         * }
         * }
         * } catch (Exception e) {
         * // Token 無效或解析失敗
         * return null;
         * }
         * }
         * break;
         * }
         * }
         */

        System.out.println("OwnerAuthHelper - 未能從 header 獲取到 userId");
        return null;
    }

    /**
     * 檢查用戶是否為房東
     * 檢查條件：role_id = 3 或角色名稱 = "房東" 或 "業者"
     */
    public boolean isOwner(HttpServletRequest request) {
        Integer userId = getUserIdFromRequest(request);
        if (userId == null) {
            System.out.println("OwnerAuthHelper.isOwner - userId 為 null");
            return false;
        }

        try {
            User user = userService.findUserById(userId);
            if (user == null) {
                System.out.println("OwnerAuthHelper.isOwner - 用戶不存在，userId: " + userId);
                return false;
            }

            // 強制載入角色關係（避免 lazy loading 問題）
            // 訪問 getRoles() 會觸發 lazy loading，但需要確保在事務中
            List<Role> roles = user.getRoles();
            if (roles != null) {
                roles.size(); // 觸發 lazy loading，確保角色被載入
            }

            if (roles == null || roles.isEmpty()) {
                System.out.println("OwnerAuthHelper.isOwner - 用戶沒有角色，userId: " + userId);
                return false;
            }

            // 調試：輸出所有角色
            System.out.println("OwnerAuthHelper.isOwner - 用戶角色列表 (userId: " + userId + "):");
            for (Role role : roles) {
                if (role != null) {
                    System.out.println("  - role_id: " + role.getId() + ", name: " + role.getName());
                } else {
                    System.out.println("  - role: null");
                }
            }

            boolean isOwner = roles.stream()
                    .anyMatch(role -> role != null && (
                    // 檢查 role_id = 3 (HOTEL_OWNER)
                    (role.getId() != null && role.getId() == 3) ||
                    // 檢查角色名稱
                            "房東".equals(role.getName()) ||
                            "業者".equals(role.getName())));

            System.out.println("OwnerAuthHelper.isOwner - 檢查結果: " + isOwner + " (userId: " + userId + ")");
            return isOwner;
        } catch (Exception e) {
            System.err.println("OwnerAuthHelper.isOwner - 檢查失敗 (userId: " + userId + "): " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 檢查用戶是否有特定角色
     */
    public boolean hasRole(HttpServletRequest request, String roleName) {
        Integer userId = getUserIdFromRequest(request);
        if (userId == null) {
            return false;
        }

        try {
            User user = userService.findUserById(userId);
            if (user == null || user.getRoles() == null || user.getRoles().isEmpty()) {
                return false;
            }
            return user.getRoles().stream()
                    .anyMatch(role -> role != null && roleName.equals(role.getName()));
        } catch (Exception e) {
            return false;
        }
    }
}
