package tw.com.ispan.eeit.ho_back.audit_logs;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;
import tw.com.ispan.eeit.ho_back.util.OwnerAuthHelper;

/**
 * 請求上下文
 * 用於獲取當前請求的相關信息（IP、用戶 ID 等）
 */
@Component
@RequestScope
public class RequestContext {

    private final HttpServletRequest request;
    private final OwnerAuthHelper ownerAuthHelper;

    public RequestContext(HttpServletRequest request, OwnerAuthHelper ownerAuthHelper) {
        this.request = request;
        this.ownerAuthHelper = ownerAuthHelper;
    }

    /**
     * 獲取客戶端 IP 地址
     * 支援多種 header（用於反向代理環境）：
     * 1. X-Forwarded-For（最常用）
     * 2. X-Real-IP（Nginx 等）
     * 3. Proxy-Client-IP（Apache 等）
     * 4. WL-Proxy-Client-IP（WebLogic）
     * 
     * 如果都沒有，使用 request.getRemoteAddr()
     * 對於 IPv6 loopback (0:0:0:0:0:0:0:1)，轉換為 192.168.25.152
     */
    public String getClientIp() {
        // 優先檢查 X-Forwarded-For（最常用於反向代理）
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            String ip = xff.split(",")[0].trim();
            return normalizeIp(ip);
        }

        // 檢查 X-Real-IP（Nginx 等）
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return normalizeIp(xRealIp.trim());
        }

        // 檢查 Proxy-Client-IP（Apache 等）
        String proxyClientIp = request.getHeader("Proxy-Client-IP");
        if (proxyClientIp != null && !proxyClientIp.isBlank()) {
            return normalizeIp(proxyClientIp.trim());
        }

        // 檢查 WL-Proxy-Client-IP（WebLogic）
        String wlProxyClientIp = request.getHeader("WL-Proxy-Client-IP");
        if (wlProxyClientIp != null && !wlProxyClientIp.isBlank()) {
            return normalizeIp(wlProxyClientIp.trim());
        }

        // 最後使用 request.getRemoteAddr()
        String remoteAddr = request.getRemoteAddr();
        return normalizeIp(remoteAddr);
    }

    /**
     * 標準化 IP 地址
     * 將 IPv6 loopback 轉換為更易讀的格式
     */
    private String normalizeIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return "0.0.0.0";
        }

        // IPv6 loopback 地址轉換為 192.168.25.152
        if (ip.equals("0:0:0:0:0:0:0:1") || ip.equals("::1")) {
            return "127.0.0.1";
        }

        // IPv4 loopback 地址保持不變
        if (ip.equals("127.0.0.1") || ip.equals("192.168.25.152")) {
            return "127.0.0.1";
        }

        return ip;
    }

    /**
     * 獲取當前用戶 ID
     * 從 JWT token 或 request attribute 中獲取
     * 
     * 實現方式：
     * 1. 優先從 request attribute 獲取（由 JwtAuthenticationFilter 設置）
     * 2. 如果沒有，嘗試從 Authorization header 解析 JWT token
     * 3. 如果都無法獲取，返回 null（將使用默認用戶 ID）
     */
    public Long getCurrentUserId() {
        Integer userId = ownerAuthHelper.getUserIdFromRequest(request);
        return userId != null ? userId.longValue() : null;
    }
}
