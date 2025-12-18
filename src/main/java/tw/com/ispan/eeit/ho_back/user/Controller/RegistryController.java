package tw.com.ispan.eeit.ho_back.user.Controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import tw.com.ispan.eeit.ho_back.user.UserRegistryDto;
import tw.com.ispan.eeit.ho_back.user.UserService;

@Controller
@PropertySource("classpath:hotel_platform.properties")
public class RegistryController {
    @Autowired
    UserService userService;

    @Value("${front.path}")
    String frontPath;

    // 驗證使用者資料後發送驗證信，並建立使用者
    @ResponseBody
    @PostMapping("/api/validate")
    public ResponseEntity<?> checkEmail(@RequestBody @Valid UserRegistryDto userDto, BindingResult bindingResult,
            HttpSession session) {
        Map<String, String> response = userService.isError(userDto, bindingResult);
        System.out.println("response=" + response);
        if (response.isEmpty()) {
            // 寄送使用者驗證資料
            userService.sendMail(userDto);
            session.setAttribute("userDto", userDto);
            return ResponseEntity.status(HttpStatus.OK).body("請到信箱驗證");
        } else {
            return new ResponseEntity<Map<String, String>>(response, HttpStatus.BAD_REQUEST);
        }
    }

    // 驗證信箱
    @GetMapping("/api/verify")
    public String verifyEmail(@RequestParam String token, HttpSession session) {
        try {
            userService.verifyEmail(token);
            session.removeAttribute("userDto");
            return "redirect:" + frontPath + "/user/registry?verify=true";
        } catch (Exception e) {
            return "redirect:" + frontPath + "/user/registry?verify=false";
        }
    }

    // 重新發送驗證信
    @GetMapping("/api/resendVerifyEmail")
    public ResponseEntity<String> resendVerifyEmail(HttpSession session) {
        try {
            UserRegistryDto userDto = (UserRegistryDto) session.getAttribute("userDto");
            System.out.println(userDto);
            userService.resendVerifyEmail(userDto.getEmail());
            return ResponseEntity.ok().body("已重新發送驗證信至" + userDto.getEmail());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/api/back/resendEmail")
    public ResponseEntity<String> resendEmail(String email) {
        userService.resendVerifyEmail(email);
        return ResponseEntity.ok("發送成功");
    }

}
