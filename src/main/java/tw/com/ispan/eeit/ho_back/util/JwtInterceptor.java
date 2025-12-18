package tw.com.ispan.eeit.ho_back.util;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtInterceptor implements HandlerInterceptor {
    @Autowired
    private JsonWebTokenUtils jsonWebTokenUtils;

    @Override
    public boolean preHandle(HttpServletRequest request,
            HttpServletResponse response, Object handler) throws Exception {
        String method = request.getMethod();
        if ("OPTIONS".equals(method)) {
            return true;
        }

        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            String token = auth.substring(7);
            try {
                String json = jsonWebTokenUtils.validateEncryptedToken(token);
                if (json != null && json.length() != 0) {
                    JSONObject user = new JSONObject(json);
                    Integer userId = user.getInt("id");
                    request.setAttribute("userId", userId);
                    System.out.println("JwtInterceptor - 成功解析 token，設置 userId: " + userId);
                    return true;
                } else {
                    System.err.println("JwtInterceptor - token 驗證返回空值");
                }
            } catch (Exception e) {
                // JWT token 解析失敗（可能是密鑰不匹配、token 格式錯誤等）
                // 記錄錯誤但不阻止請求，讓後續邏輯處理
                System.err.println(
                        "JwtInterceptor - token 解析失敗: " + e.getClass().getSimpleName() + ": " + e.getMessage());
                if (e.getCause() != null) {
                    System.err.println("JwtInterceptor - 根本原因: " + e.getCause().getClass().getSimpleName() + ": "
                            + e.getCause().getMessage());
                }
                // 不設置 userId attribute，讓 OwnerAuthHelper 嘗試其他方式獲取
            }
        } else {
            System.out.println("JwtInterceptor - 沒有 Authorization header 或格式不正確");
        }

        // 如果沒有有效的 token，仍然允許請求繼續（由 Controller 層決定是否需要認證）
        // 這樣可以讓 OwnerAuthHelper 嘗試其他方式獲取 userId
        return true;
    }
}