package tw.com.ispan.eeit.ho_back.user.Controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import tw.com.ispan.eeit.ho_back.user.User;
import tw.com.ispan.eeit.ho_back.user.UserService;
import tw.com.ispan.eeit.ho_back.util.OwnerAuthHelper;

/**
 * 註冊成為房東 Controller
 * 提供用戶立即開通房東角色的功能
 */
@RestController
public class BecomeHostController {

    @Autowired
    UserService userService;

    @Autowired
    private OwnerAuthHelper ownerAuthHelper;

    /**
     * 註冊成為房東
     * 從 Cookie 中的 token 獲取用戶 ID，然後為該用戶添加房東角色
     */
    @PostMapping("/api/become-host")
    public ResponseEntity<?> becomeHost(HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();

        try {
            // 調試：檢查 Authorization header
            String authHeader = request.getHeader("Authorization");
            System.out.println("BecomeHostController - Authorization header: " + (authHeader != null ? "存在" : "不存在"));
            if (authHeader != null) {
                System.out.println("BecomeHostController - Authorization header 值: "
                        + authHeader.substring(0, Math.min(50, authHeader.length())) + "...");
            }

            // 使用 OwnerAuthHelper 從請求中獲取用戶 ID（支持 Cookie 和 Authorization header）
            Integer userId = ownerAuthHelper.getUserIdFromRequest(request);
            System.out.println("BecomeHostController - 獲取的 userId: " + userId);

            if (userId == null) {
                response.put("success", false);
                response.put("message", "請先登入");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            // 為用戶添加房東角色
            User user = userService.addOwnerRole(userId);

            response.put("success", true);
            response.put("message", "成功註冊成為房東！");
            response.put("user", user);
            response.put("roles", user.getRoles());

            return ResponseEntity.status(HttpStatus.OK).body(response);

        } catch (RuntimeException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "系統錯誤，請稍後再試");
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

}
