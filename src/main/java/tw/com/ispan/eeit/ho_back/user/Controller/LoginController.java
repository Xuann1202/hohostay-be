package tw.com.ispan.eeit.ho_back.user.Controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import tw.com.ispan.eeit.ho_back.user.User;
import tw.com.ispan.eeit.ho_back.user.UserService;
import tw.com.ispan.eeit.ho_back.util.JsonWebTokenUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * 用戶端登入 Controller
 * 提供用戶端登入功能（需要密碼驗證）
 * 
 * 用途：用戶端登入（需要密碼驗證）
 * - 適用於：一般用戶登入
 * - 登入方式：使用 email + password
 * - Token 格式：使用 JsonWebTokenUtils (nimbus-jose-jwt) 生成的加密 token (JWE)
 * 
 * 注意：此端點與 AuthController (/api/auth/login) 不同：
 * - LoginController：用戶端登入，需要 email + password
 * - AuthController：管理後台登入，不需要密碼
 * 
 * ⚠️ 注意：此 Controller 使用 JsonWebTokenUtils，與 AuthController 使用的 JwtUtil 不兼容
 * 建議：未來統一 JWT 工具類時，需要遷移此 Controller 的 token 生成邏輯
 */
@RestController
public class LoginController {

    @Autowired
    UserService userService;

    @Autowired
    private JsonWebTokenUtils jsonWebTokenUtils;

    // 登入
    @PostMapping("/api/login")
    public ResponseEntity<?> login(String email, String password) {
        Map<String, Object> response = new HashMap<>();
        User user = userService.loginResult(email, password);
        String userJson = null;
        if (user != null) {
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                // 處理JSON 辨認 LocalDate的問題
                objectMapper.registerModule(new JavaTimeModule());
                userJson = objectMapper.writeValueAsString(user);
                System.out.println("登入後userJson=" + userJson);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
            String token = jsonWebTokenUtils.createEncryptedToken(userJson);
            response.put("success", "true");
            response.put("id", user.getId());
            response.put("userLastName", user.getLastName());
            response.put("userFirstName", user.getFirstName());
            response.put("email", user.getEmail());
            response.put("phone", user.getPhoneNumber());
            response.put("gender", user.getGender()); // ✅ 添加性別欄位
            response.put("dateOfBirth", user.getDateOfBirth()); // ✅ 添加生日欄位
            response.put("address", user.getAddress()); // ✅ 添加地址欄位
            response.put("token", token);
            response.put("role", user.getRoles());
            response.put("photo", user.getImage());
            return new ResponseEntity<Map<String, Object>>(response, HttpStatus.OK);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    // 登出
    @GetMapping("/api/logout")
    public ResponseEntity<String> logout(HttpServletResponse response) {
        Cookie cookie = new Cookie("token", null);
        cookie.setMaxAge(0);
        cookie.setPath("/");
        response.addCookie(cookie);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    // google登入後前端取資料顯示在畫面上
    @GetMapping("/api/user")
    public ResponseEntity<?> getUser(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @CookieValue(value = "token", required = false) String jwtCookie) {

        String jwtToken = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            jwtToken = authHeader.substring(7);

            // --------------------------------------------------------
            System.out.println("=== 從 Header 取得的 Token ===");
            System.out.println("完整長度: " + jwtToken.length());
            System.out.println("完整內容: " + jwtToken);
            System.out.println("Token 前50字元: " + jwtToken.substring(0, Math.min(50,
                    jwtToken.length())));
            System.out.println("Token 後50字元: " + jwtToken.substring(Math.max(0,
                    jwtToken.length() - 50)));

        } else if (jwtCookie != null && !jwtCookie.isBlank()) {
            jwtToken = jwtCookie;
            // --------------------------------------------------------
            System.out.println("=== 從 Header 取得的 Token ===");
            System.out.println("完整長度: " + jwtToken.length());
            System.out.println("完整內容: " + jwtToken);
            System.out.println("Token 前50字元: " + jwtToken.substring(0, Math.min(50,
                    jwtToken.length())));
            System.out.println("Token 後50字元: " + jwtToken.substring(Math.max(0,
                    jwtToken.length() - 50)));
        }

        if (jwtToken != null && jwtToken.trim().length() != 0) {
            try {
                String userJson = jsonWebTokenUtils.validateEncryptedToken(jwtToken);
                System.out.println("解析後的 userJson: " + userJson);

                // 從json拿userId
                Integer userId = null;
                try {
                    ObjectMapper objectMapper = new ObjectMapper();
                    JsonNode jsonNode = objectMapper.readTree(userJson);
                    if (jsonNode.has("id")) {
                        userId = jsonNode.get("id").asInt();
                    }
                } catch (Exception e) {
                    System.err.println("解析 userJson 失敗: " + e.getMessage());
                    e.printStackTrace();
                }

                // ✅ 檢查 userId 是否為 null
                if (userId == null) {
                    System.err.println("無法從 token 中解析 userId");
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body("無法從 token 中解析用戶 ID，請重新登入");
                }

                User user = userService.findUserById(userId);
                if (user == null) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body("找不到用戶 ID: " + userId);
                }

                Map<String, Object> response = new HashMap<>();
                response.put("success", "true");
                response.put("id", user.getId());
                response.put("userLastName", user.getLastName());
                response.put("userFirstName", user.getFirstName());
                response.put("email", user.getEmail());
                response.put("phone", user.getPhoneNumber());
                response.put("gender", user.getGender()); // ✅ 添加性別欄位
                response.put("dateOfBirth", user.getDateOfBirth()); // ✅ 添加生日欄位
                response.put("address", user.getAddress()); // ✅ 添加地址欄位
                response.put("token", jwtToken);
                response.put("role", user.getRoles());
                response.put("photo", user.getImage());
                return new ResponseEntity<Map<String, Object>>(response, HttpStatus.OK);
            } catch (Exception e) {
                System.err.println("驗證 token 或查詢用戶失敗: " + e.getMessage());
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Token 無效或已過期，請重新登入");
            }
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("沒有jwt token");
    }

    // 更新用戶個人資料
    @PutMapping("/api/users/me")
    public ResponseEntity<?> updateUserProfile(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @CookieValue(value = "token", required = false) String jwtCookie,
            @org.springframework.web.bind.annotation.RequestBody Map<String, Object> requestData) {

        try {
            // 獲取 token
            String jwtToken = null;
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                jwtToken = authHeader.substring(7);
            } else if (jwtCookie != null && !jwtCookie.isBlank()) {
                jwtToken = jwtCookie;
            }

            if (jwtToken == null || jwtToken.trim().length() == 0) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("沒有jwt token");
            }

            // 從 token 獲取 userId
            String userJson = jsonWebTokenUtils.validateEncryptedToken(jwtToken);
            Integer userId = null;
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode jsonNode = objectMapper.readTree(userJson);
                if (jsonNode.has("id")) {
                    userId = jsonNode.get("id").asInt();
                }
            } catch (Exception e) {
                System.err.println("解析 userJson 失敗: " + e.getMessage());
                e.printStackTrace();
            }

            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("無法從 token 中解析用戶 ID，請重新登入");
            }

            // 查找用戶
            User user = userService.findUserById(userId);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("找不到用戶 ID: " + userId);
            }

            // 更新用戶資料
            // 處理 name 欄位（拆分為 firstName 和 lastName）
            if (requestData.containsKey("name")) {
                String name = (String) requestData.get("name");
                if (name != null && !name.trim().isEmpty()) {
                    String[] nameParts = name.trim().split("\\s+", 2);
                    if (nameParts.length >= 2) {
                        user.setFirstName(nameParts[0]);
                        user.setLastName(nameParts[1]);
                    } else {
                        user.setFirstName(nameParts[0]);
                        user.setLastName("");
                    }
                }
            }

            // 更新其他欄位
            if (requestData.containsKey("email")) {
                user.setEmail((String) requestData.get("email"));
            }
            if (requestData.containsKey("phone") || requestData.containsKey("phoneNumber")) {
                String phone = (String) (requestData.get("phone") != null ? requestData.get("phone")
                        : requestData.get("phoneNumber"));
                user.setPhoneNumber(phone);
            }
            if (requestData.containsKey("birthday") || requestData.containsKey("dateOfBirth")) {
                Object dateObj = requestData.get("dateOfBirth") != null ? requestData.get("dateOfBirth")
                        : requestData.get("birthday");
                if (dateObj != null) {
                    try {
                        String dateStr = dateObj.toString();
                        if (!dateStr.isEmpty()) {
                            // 處理多種日期格式
                            java.time.LocalDate dateOfBirth = null;
                            if (dateStr.contains("/")) {
                                // 處理 "yyyy/MM/dd" 格式
                                String[] parts = dateStr.split("/");
                                if (parts.length == 3) {
                                    dateOfBirth = java.time.LocalDate.of(
                                            Integer.parseInt(parts[0]),
                                            Integer.parseInt(parts[1]),
                                            Integer.parseInt(parts[2]));
                                }
                            } else {
                                // 處理 "yyyy-MM-dd" 格式
                                dateOfBirth = java.time.LocalDate.parse(dateStr);
                            }
                            if (dateOfBirth != null) {
                                user.setDateOfBirth(dateOfBirth);
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("日期格式錯誤: " + dateObj + ", 錯誤: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
            if (requestData.containsKey("address")) {
                user.setAddress((String) requestData.get("address"));
            }
            if (requestData.containsKey("gender")) {
                String gender = (String) requestData.get("gender");
                // 確保 gender 是 M, F, 或 O
                if (gender != null && (gender.equals("M") || gender.equals("F") || gender.equals("O"))) {
                    user.setGender(gender);
                }
            }

            // 保存更新
            userService.update(user);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "個人資料更新成功");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("更新用戶資料失敗: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("更新失敗: " + e.getMessage());
        }
    }

    // 上傳用戶頭像
    @PostMapping("/api/users/me/avatar")
    public ResponseEntity<?> uploadUserAvatar(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @CookieValue(value = "token", required = false) String jwtCookie,
            @RequestParam("image") MultipartFile file) {

        try {
            // 獲取 token
            String jwtToken = null;
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                jwtToken = authHeader.substring(7);
            } else if (jwtCookie != null && !jwtCookie.isBlank()) {
                jwtToken = jwtCookie;
            }

            if (jwtToken == null || jwtToken.trim().length() == 0) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("沒有jwt token");
            }

            // 從 token 獲取 userId
            String userJson = jsonWebTokenUtils.validateEncryptedToken(jwtToken);
            Integer userId = null;
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode jsonNode = objectMapper.readTree(userJson);
                if (jsonNode.has("id")) {
                    userId = jsonNode.get("id").asInt();
                }
            } catch (Exception e) {
                System.err.println("解析 userJson 失敗: " + e.getMessage());
                e.printStackTrace();
            }

            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("無法從 token 中解析用戶 ID，請重新登入");
            }

            // 驗證文件類型
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("只支援圖片格式");
            }

            // 驗證文件大小（5MB）
            if (file.getSize() > 5 * 1024 * 1024) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("檔案大小不能超過 5MB");
            }

            // 查找用戶
            User user = userService.findUserById(userId);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("找不到用戶 ID: " + userId);
            }

            // 定義文件名：user_(user_id).jpg
            String filename = "user_" + userId + ".jpg";

            // 獲取 static/images 目錄路徑
            // 使用 ClassPathResource 獲取 resources/static/images 目錄
            Path imagesPath = null;

            try {
                org.springframework.core.io.ClassPathResource resource = new org.springframework.core.io.ClassPathResource(
                        "static/images");

                // 嘗試獲取文件系統路徑（開發環境）
                if (resource.exists()) {
                    try {
                        // 開發環境：直接獲取文件系統路徑
                        imagesPath = Paths.get(resource.getFile().getAbsolutePath());
                        System.out.println("使用資源路徑: " + imagesPath);
                    } catch (IOException e) {
                        // JAR 打包後無法直接獲取文件系統路徑，使用項目根目錄
                        String projectRoot = System.getProperty("user.dir");
                        imagesPath = Paths.get(projectRoot, "src", "main", "resources", "static", "images");
                        System.out.println("使用項目根目錄: " + imagesPath);
                    }
                } else {
                    // 如果資源不存在，使用項目根目錄
                    String projectRoot = System.getProperty("user.dir");
                    imagesPath = Paths.get(projectRoot, "src", "main", "resources", "static", "images");
                    System.out.println("資源不存在，使用項目根目錄: " + imagesPath);
                }
            } catch (Exception e) {
                // 如果所有方法都失敗，使用項目根目錄
                String projectRoot = System.getProperty("user.dir");
                imagesPath = Paths.get(projectRoot, "src", "main", "resources", "static", "images");
                System.err.println("獲取路徑失敗，使用項目根目錄: " + imagesPath);
            }

            // 確保目錄存在
            if (!Files.exists(imagesPath)) {
                Files.createDirectories(imagesPath);
                System.out.println("創建目錄: " + imagesPath);
            }

            // 刪除舊的頭像文件（如果存在）
            Path oldFilePath = imagesPath.resolve(filename);
            if (Files.exists(oldFilePath)) {
                try {
                    Files.delete(oldFilePath);
                    System.out.println("已刪除舊頭像: " + oldFilePath);
                } catch (IOException e) {
                    System.err.println("刪除舊頭像失敗: " + e.getMessage());
                    // 繼續執行，不影響新文件上傳
                }
            }

            // 保存新文件
            Path newFilePath = imagesPath.resolve(filename);
            Files.copy(file.getInputStream(), newFilePath, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("頭像已保存: " + newFilePath);

            // 更新用戶的 image 欄位為文件名（不含路徑，因為會通過 /api/user/photo 端點訪問）
            user.setImage(filename);
            userService.update(user);
            System.out.println("用戶 image 欄位已更新為: " + filename);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "頭像上傳成功");
            response.put("filename", filename);
            response.put("url", filename); // 返回文件名，前端可以通過 /api/user/photo?photoUrl=filename 訪問

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            System.err.println("上傳頭像失敗: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("上傳失敗: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("上傳頭像失敗: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("上傳失敗: " + e.getMessage());
        }
    }

    // 修改密碼
    @PutMapping("/api/users/me/password")
    public ResponseEntity<?> changePassword(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @CookieValue(value = "token", required = false) String jwtCookie,
            @org.springframework.web.bind.annotation.RequestBody Map<String, Object> requestData) {

        try {
            // 獲取 token
            String jwtToken = null;
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                jwtToken = authHeader.substring(7);
            } else if (jwtCookie != null && !jwtCookie.isBlank()) {
                jwtToken = jwtCookie;
            }

            if (jwtToken == null || jwtToken.trim().length() == 0) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("沒有jwt token");
            }

            // 從 token 獲取 userId
            String userJson = jsonWebTokenUtils.validateEncryptedToken(jwtToken);
            Integer userId = null;
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode jsonNode = objectMapper.readTree(userJson);
                if (jsonNode.has("id")) {
                    userId = jsonNode.get("id").asInt();
                }
            } catch (Exception e) {
                System.err.println("解析 userJson 失敗: " + e.getMessage());
                e.printStackTrace();
            }

            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("無法從 token 中解析用戶 ID，請重新登入");
            }

            // 獲取密碼參數
            String oldPassword = (String) requestData.get("oldPassword");
            String newPassword = (String) requestData.get("newPassword");
            String confirmPassword = (String) requestData.get("confirmPassword");

            // 調用 UserService 修改密碼
            userService.changePassword(userId, oldPassword, newPassword, confirmPassword);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "密碼修改成功");
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            System.err.println("修改密碼失敗: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            System.err.println("修改密碼失敗: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "修改密碼失敗: " + e.getMessage()));
        }
    }

    // 驗證信箱是否重複
    @PostMapping("/api/checkemail")
    public Map<String, String> checkEmail(String email) {
        Map<String, String> result = new HashMap<>();
        String regrex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
        if (email != null && email.length() != 0) {
            if (email.matches(regrex)) {
                if (userService.isEmailExisted(email)) {
                    result.put("isEmailExisted", "true");
                    result.put("message", "電子郵件已存在");
                } else {
                    result.put("isEmailExisted", "false");
                    result.put("message", "此電子郵件可使用");
                }
            } else {
                result.put("message", "請輸入正確的email格式");
            }
        } else {
            result.put("message", "電子郵件為必填");
        }
        return result;
    }

}
