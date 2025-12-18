package tw.com.ispan.eeit.ho_back.user.Controller;

import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import tw.com.ispan.eeit.ho_back.properties.UserStatusProperties;
import tw.com.ispan.eeit.ho_back.role.Role;
import tw.com.ispan.eeit.ho_back.role.RoleRepository;
import tw.com.ispan.eeit.ho_back.user.GoogleUserDto;
import tw.com.ispan.eeit.ho_back.user.User;
import tw.com.ispan.eeit.ho_back.user.UserService;
import tw.com.ispan.eeit.ho_back.util.JsonWebTokenUtils;

@Controller
@PropertySource("classpath:hotel_platform.properties")
public class GoogleAuthController {

    @Autowired
    UserService userService;
    @Autowired
    JsonWebTokenUtils jsonWebTokenUtils;
    @Autowired
    UserStatusProperties userStatusProperties;
    @Autowired
    RoleRepository roleRepository;
    @Autowired
    ModelMapper modelMapper;

    @Value("${front.path}")
    String frontPath;

    @Value("${back.path}")
    String backPath;

    @Value("${google.client.id}")
    String clientId;

    @Value("${google.redirect.uri}")
    String redirectUri;
    
    @Value("${google.client.secret}")
    String clientSecret;

    // 將使用者導向 Google 登入
    @GetMapping("google/login")
    public String googleLogin() {
        String authUri = "https://accounts.google.com/o/oauth2/v2/auth?"
                + "scope=" + "openid%20email%20profile"
                + "&response_type=" + "code"
                + "&prompt=consent"
                + "&access_type=offline"
                + "&client_id=" + clientId
                + "&redirect_uri=" + redirectUri;
        return "redirect:" + authUri;
    }

    @GetMapping("/google/callback")
    public String googleCallback(@RequestParam String code, HttpServletResponse response) {

        RestClient restClient = RestClient.create();

        // 1. 使用者同意後取得 code 換取 token
        String queryString = UriComponentsBuilder.newInstance()
                .queryParam("code", code)
                .queryParam("client_id", clientId)
                .queryParam("client_secret", clientSecret)
                .queryParam("grant_type", "authorization_code")
                .queryParam("redirect_uri", redirectUri)
                .build()
                .getQuery();

        String responseBody = restClient.post()
                .uri("https://oauth2.googleapis.com/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(queryString)
                .retrieve()
                .body(String.class);

        ObjectMapper objectMapper = new ObjectMapper();
        String resultString = "";
        String jwtToken = null; // ⭐ 統一只在這裡宣告一次，後面都只做賦值

        try {
            // 2. 取得 Google user 資料
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            String accessToken = jsonNode.get("access_token").asText();

            resultString = restClient.get()
                    .uri("https://www.googleapis.com/oauth2/v1/userinfo?alt=json&access_token=" + accessToken)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(String.class);

        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            // 3. 將取得的資料存進資料庫
            User user = null;
            GoogleUserDto googleUser = objectMapper.readValue(resultString, GoogleUserDto.class);

            if (!userService.isEmailExisted(googleUser.getEmail())) {
                // 新使用者
                user = modelMapper.map(googleUser, User.class);
                user.setStatus(userStatusProperties.getVerify());

                List<Role> roles = user.getRoles();
                Role role = roleRepository.getReferenceById(2); // 一般會員
                user.addRole(role);
                user.setRoles(roles);

                userService.create(user);
            } else {
                // 已存在 → 更新資料
                user = userService.findByEmail(googleUser.getEmail());
                modelMapper.map(googleUser, user);
                userService.update(user);
            }

            // 4. 產生 JWT，寫入 Cookie
            String userJson = objectMapper.writeValueAsString(user);
            jwtToken = jsonWebTokenUtils.createEncryptedToken(userJson);

            Cookie cookie = new Cookie("token", jwtToken);
            cookie.setPath("/");
            cookie.setMaxAge(3600); // 1 小時
            cookie.setAttribute("SameSite", "Lax");
            response.addCookie(cookie);

            System.out.println("儲存在 cookie 的 token = " + jwtToken);

        } catch (Exception e) {
            e.printStackTrace();
        }

        // 5. 最後 redirect 回前端
        // 有 token 的情況，多帶一個 query param，前端想用就用，不想用就看 cookie
        if (jwtToken != null) {
            return "redirect:" + frontPath + "/user/login?token=" + jwtToken;
        } else {
            return "redirect:" + frontPath + "/user/login";
        }
    }
}
