package tw.com.ispan.eeit.ho_back.user.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpSession;
import tw.com.ispan.eeit.ho_back.common.exception.TokenVerifyException;
import tw.com.ispan.eeit.ho_back.user.UserService;

@Controller
@PropertySource("classpath:hotel_platform.properties")
public class ForgotPasswordController {
    @Autowired
    UserService userService;

    @Value("${front.path}")
    String frontPath;

    // 忘記密碼
    @PostMapping("/api/forgotPassword")
    public ResponseEntity<String> forgotPassword(String email, HttpSession session) {
        try {
            userService.forgotPassword(email);
            session.setAttribute("email", email);
            return ResponseEntity.ok().body("請至" + email + "收取驗證信");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 接收忘記密碼驗證信
    @GetMapping("/api/forgotPassword")
    public String verifyEmail(@RequestParam String token) {
        try {
            userService.verifyEmail(token);
            return "redirect:" + frontPath + "/user/resetPassword";
        } catch (Exception e) {
            return "redirect:" + frontPath + "/user/forgotPassword?verify=false";
        }
    }

    // 重設密碼
    @PostMapping("/api/resetPassword")
    public ResponseEntity<?> resetPassword(String password, String checkPassword, HttpSession session) {
        try {
            // userService.verifyEmail(token);
            System.out.println(session.getAttribute("email"));
            userService.resetPassword((String) session.getAttribute("email"), password, checkPassword);
            session.removeAttribute("email");
            System.out.println(session.getAttribute("email"));
            return ResponseEntity.ok().body("設定密碼成功");
        } catch (TokenVerifyException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
