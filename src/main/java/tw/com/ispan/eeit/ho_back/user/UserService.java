package tw.com.ispan.eeit.ho_back.user;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

import jakarta.transaction.Transactional;
import tw.com.ispan.eeit.ho_back.common.exception.LoginException;
import tw.com.ispan.eeit.ho_back.common.exception.TokenVerifyException;
import tw.com.ispan.eeit.ho_back.properties.UserStatusProperties;
import tw.com.ispan.eeit.ho_back.role.Role;
import tw.com.ispan.eeit.ho_back.role.RoleRepository;

@Service
@Transactional
@PropertySource("classpath:hotel_platform.properties")
public class UserService {
    private final ModelMapper modelMapper = new ModelMapper();
    @Autowired
    UserRepository userRepository;
    @Autowired
    private EmailService emailService;
    @Autowired
    BCryptPasswordEncoder bCryptPasswordEncoder;
    @Autowired
    UserStatusProperties userStatusProperties;
    @Autowired
    RoleRepository roleRepository;
    @Value("${back.path}")
    private String backPath;

    String emailRegrex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
    String passwordRegrex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*\\W).{8,20}$";

    // 登入
    public User loginResult(String email, String password) {
        Map<String, String> errorMessages = new HashMap<>();
        User user = null;
        if (email != null && email.trim().length() != 0 && !email.matches(emailRegrex)) {
            errorMessages.put("loginError", "請檢查email格式");
        } else {
            if ((email == null || email.trim().length() == 0) && (password == null || password.trim().length() == 0)) {
                errorMessages.put("loginError", "請輸入電子郵件及密碼");
            } else {
                if (password == null || password.trim().length() == 0) {
                    errorMessages.put("loginError", "請輸入密碼");
                }
                if (email == null || email.trim().length() == 0) {
                    errorMessages.put("loginError", "請輸入電子郵件");
                }
            }
        }
        // 到資料庫檢查帳號密碼是否正確，且status要為verify
        if (errorMessages.isEmpty()) {
            user = userRepository.findByEmail(email);
            if (user == null || !password.equals(user.getPassword())) {
                errorMessages.put("loginError", "帳號或密碼錯誤");
            } else if (user.getStatus() != userStatusProperties.getVerify()) {
                errorMessages.put("loginError", "帳號尚未完成信箱驗證");
            }
            System.out.println(user);
        }
        if (!errorMessages.isEmpty()) {
            throw new LoginException(errorMessages);
        } else {
            return user;
        }
    }

    // 註冊時驗證表單
    public Map<String, String> isError(UserRegistryDto userDto, BindingResult bindingResult) {
        Map<String, String> errors = new HashMap<>();
        if (bindingResult.hasErrors()) {
            List<FieldError> fieldErrors = bindingResult.getFieldErrors();
            for (FieldError fieldError : fieldErrors) {
                errors.put(fieldError.getField(), fieldError.getDefaultMessage());
            }
        }
        // 密碼是否一致
        if (userDto.getCheckPassword() != null && userDto.getCheckPassword().trim().length() != 0) {
            if (!userDto.getPassword().equals(userDto.getCheckPassword())) {
                errors.put("checkPassword", "密碼不一致");
            }
        }
        // 信箱是否存在
        if (userRepository.existsByEmail(userDto.getEmail())) {
            errors.put("email", "電子郵件已存在");
        }
        return errors;
    }

    // 寄驗證信,並存入使用者資料
    public void sendMail(UserRegistryDto userDto) {
        if (userDto != null) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime expireTime = now.plusMinutes(10);
            userDto.setToken(UUID.randomUUID().toString());
            userDto.setTokenExpireTime(expireTime);
            User user = modelMapper.map(userDto, User.class);
            // 密碼不加密，直接存儲明文（僅用於測試）
            List<Role> roles = user.getRoles();
            Role role = roleRepository.getReferenceById(2);
            user.addRole(role);
            user.setRoles(roles);
            userRepository.save(user);
            String token = userDto.getToken();
            String sendTo = userDto.getEmail();
            String subject = "帳號驗證信";
            String content = "<h3>您好，歡迎註冊HoHoStay</h3><p>請點擊以下連結完成驗證：</p>"
                    + "<a href='" + backPath + "/api/verify?token=" + token + "''>點我驗證</a>";
            emailService.sendValidateMail(sendTo, subject, content);
        } else {
            throw new RuntimeException("使用者資料有誤，無法寄送驗證信");
        }
    }

    // 重新發送驗證信
    public void resendVerifyEmail(String email) {
        if (email != null && email.length() != 0) {
            User user = userRepository.findByEmail(email);
            if (user != null) {
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime expireTime = now.plusMinutes(10);
                user.setToken(UUID.randomUUID().toString());
                user.setTokenExpireTime(expireTime);
                userRepository.save(user);
                String token = user.getToken();
                String sendTo = user.getEmail();
                String subject = "HoHoStay帳號驗證信";
                String content = "<h3>您好，歡迎註冊HoHoStay</h3><p>請點擊以下連結完成驗證：</p>"
                        + "<a href='" + backPath + "/api/verify?token=" + token + "''>點我驗證</a>";
                emailService.sendValidateMail(sendTo, subject, content);
            }
        } else {
            throw new RuntimeException("請輸入電子郵件");
        }
    }

    // 驗證email
    public User verifyEmail(String token) {
        // 透過token找user
        if (token != null && token.length() != 0) {
            User user = userRepository.findByToken(token);
            if (user == null) {
                throw new TokenVerifyException("無效的驗證碼");
            } else {
                // 檢查token是否過期
                LocalDateTime now = LocalDateTime.now();
                if (user.getTokenExpireTime().isBefore(now)) {
                    throw new TokenVerifyException("驗證碼失效");
                } else {
                    user.setStatus(userStatusProperties.getVerify());
                    userRepository.save(user);
                    return user;
                }
            }
        }
        return null;
    }

    // 忘記密碼
    public void forgotPassword(String email) {
        if (email != null && email.length() != 0) {
            User user = userRepository.findByEmail(email);
            // 檢查email是否存在且帳號已啟用
            if (user != null && user.getStatus().equals(userStatusProperties.getVerify())) {

                // 是，生成token發送驗證信
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime expireTime = now.plusMinutes(10);
                user.setToken(UUID.randomUUID().toString());
                user.setTokenExpireTime(expireTime);
                userRepository.save(user);
                String token = user.getToken();
                String sendTo = user.getEmail();
                String subject = "HoHoStay忘記密碼驗證信";
                String content = "<h3>您好，此為忘記密碼驗證信</h3><p>請點擊以下連結重設密碼：</p>"
                        + "<a href='" + backPath + "/api/forgotPassword?token=" + token + "''>重設密碼</a>";
                emailService.sendValidateMail(sendTo, subject, content);
            } else {
                // 否，使用者不存在
                throw new RuntimeException("使用者不存在");
            }
        } else {
            throw new RuntimeException("請輸入帳號密碼");
        }
    }

    // 重設密碼
    public void resetPassword(String email, String password, String checkPassword) {
        if (password != null && password.length() != 0 && checkPassword != null
                && checkPassword.length() != 0) {
            User user = userRepository.findByEmail(email);
            System.out.println(user);
            if (user != null) {
                // 檢查密碼是否一致
                if (password.equals(checkPassword)) {
                    // 檢查密碼是否符合規則
                    if (password.matches(passwordRegrex)) {
                        user.setPassword(password); // 密碼不加密，直接存儲明文（僅用於測試）
                        userRepository.save(user);
                    } else {
                        throw new RuntimeException("密碼長度須為8-20且包含大寫、小寫英文字母、數字、特殊符號");
                    }
                } else {
                    throw new RuntimeException("密碼不一致");
                }
            } else {
                throw new RuntimeException("使用者不存在");
            }
        } else {
            throw new RuntimeException("請填寫密碼");
        }
    }

    // 修改密碼（需要驗證舊密碼）
    public void changePassword(Integer userId, String oldPassword, String newPassword, String confirmPassword) {
        if (userId == null) {
            throw new RuntimeException("用戶 ID 不能為空");
        }

        if (oldPassword == null || oldPassword.trim().isEmpty()) {
            throw new RuntimeException("請輸入目前密碼");
        }

        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw new RuntimeException("請輸入新密碼");
        }

        if (confirmPassword == null || confirmPassword.trim().isEmpty()) {
            throw new RuntimeException("請確認新密碼");
        }

        // 查找用戶
        User user = findUserById(userId);
        if (user == null) {
            throw new RuntimeException("使用者不存在");
        }

        // 驗證舊密碼是否正確
        if (!oldPassword.equals(user.getPassword())) {
            throw new RuntimeException("目前密碼不正確");
        }

        // 檢查新密碼是否與舊密碼相同
        if (oldPassword.equals(newPassword)) {
            throw new RuntimeException("新密碼不能與目前密碼相同");
        }

        // 檢查兩次新密碼輸入是否一致
        if (!newPassword.equals(confirmPassword)) {
            throw new RuntimeException("兩次密碼輸入不一致");
        }

        // 檢查新密碼長度（至少8碼）
        if (newPassword.length() < 8) {
            throw new RuntimeException("密碼至少需要 8 碼");
        }

        // 檢查新密碼是否包含英文和數字（簡化版驗證，不要求特殊符號）
        boolean hasLetter = newPassword.matches(".*[a-zA-Z].*");
        boolean hasNumber = newPassword.matches(".*[0-9].*");

        if (!hasLetter || !hasNumber) {
            throw new RuntimeException("密碼必須包含英文和數字");
        }

        // 更新密碼（不加密，直接存儲明文，僅用於測試）
        user.setPassword(newPassword);
        userRepository.save(user);
    }

    // 建立使用者(google login)
    public void create(User user) {
        if (user != null && user.getEmail() != null && user.getEmail().length() != 0) {
            userRepository.save(user);
        }
    }

    // 更新使用者
    public void update(User user) {
        if (user != null && userRepository.existsById(user.getId())) {
            userRepository.save(user);
        }
    }

    // 檢查email是否存在
    public Boolean isEmailExisted(String email) {
        if (userRepository.existsByEmail(email)) {
            return true;
        } else {
            return false;
        }
    }

    public User findUserById(Integer id) {
        Optional<User> op = userRepository.findById(id);
        if (op.isPresent()) {
            User user = op.get();
            // 強制載入角色關係（避免 lazy loading 問題）
            if (user.getRoles() != null) {
                user.getRoles().size(); // 觸發 lazy loading
            }
            return user;
        } else {
            return null;
        }
    }

    public User findByEmail(String email) {
        if (userRepository.existsByEmail(email)) {
            return userRepository.findByEmail(email);
        } else {
            return null;
        }
    }

    // 找所有使用者
    public Page<User> findAll(Integer pageNumber, Integer size) {
        Pageable pageable = PageRequest.of(pageNumber, size);
        return userRepository.findAll(pageable);
    }

    public Page<User> findByNameOrEmail(Integer pageNumber, Integer size, String keyword) {
        Pageable pageable = PageRequest.of(pageNumber, size);
        return userRepository.findByNameOrEmail(keyword, pageable);
    }

    /**
     * 為用戶添加房東角色（role_id = 3, HOTEL_OWNER）
     * 允許一般使用者（role_id = 2）註冊成為房東，添加後用戶將同時擁有兩個角色
     * 
     * @param userId 用戶 ID
     * @return 更新後的用戶對象（包含所有角色）
     */
    public User addOwnerRole(Integer userId) {
        if (userId == null) {
            throw new RuntimeException("用戶 ID 不能為空");
        }

        User user = findUserById(userId);
        if (user == null) {
            throw new RuntimeException("用戶不存在");
        }

        // 檢查用戶是否已經是房東（檢查 role_id = 3 或角色名稱）
        boolean isAlreadyOwner = user.getRoles().stream()
                .anyMatch(role -> role != null && ((role.getId() != null && role.getId() == 3) ||
                        "房東".equals(role.getName()) ||
                        "業者".equals(role.getName())));

        if (isAlreadyOwner) {
            throw new RuntimeException("用戶已經是房東");
        }

        // 查找房東角色（優先使用 role_id = 3，如果沒有則使用名稱查找）
        Optional<Role> ownerRoleOpt = roleRepository.findById(3);

        // 如果找不到，嘗試使用名稱查找
        if (ownerRoleOpt.isEmpty()) {
            ownerRoleOpt = roleRepository.findByName("房東");
        }

        // 如果還是找不到，嘗試使用「業者」名稱
        if (ownerRoleOpt.isEmpty()) {
            ownerRoleOpt = roleRepository.findByName("業者");
        }

        if (ownerRoleOpt.isEmpty()) {
            throw new RuntimeException("房東角色不存在，請聯繫管理員");
        }

        Role ownerRole = ownerRoleOpt.get();

        // 添加房東角色（不會移除現有角色，用戶將同時擁有所有角色）
        // 例如：一般使用者（role_id = 2）添加房東角色（role_id = 3）後，將同時擁有兩個角色
        try {
            user.addRole(ownerRole);
            userRepository.save(user);

            // 重新載入用戶以確保角色關係正確載入
            user = findUserById(userId);
        } catch (RuntimeException e) {
            // 如果角色已存在，直接返回用戶（不拋出異常，因為這表示操作已成功）
            if (e.getMessage() != null && e.getMessage().contains("已有此角色")) {
                return user;
            }
            throw e;
        }

        return user;
    }
}
