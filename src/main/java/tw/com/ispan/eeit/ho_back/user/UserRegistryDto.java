package tw.com.ispan.eeit.ho_back.user;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserRegistryDto {
    @Pattern(regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$", message = "請輸入正確的email格式")
    @NotBlank(message = "電子郵件為必填欄位")
    private String email;
    @Size(min = 8, max = 20, message = "長度必須在8-20之間")
    @NotBlank(message = "密碼為必填欄位")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*\\W).+$", message = "密碼須包含大寫、小寫英文字母、數字、特殊符號")
    private String password;
    @NotBlank(message = "名字為必填欄位")
    private String firstName;
    @NotBlank(message = "姓氏為必填欄位")
    private String lastName;
    @NotBlank(message = "請再輸入一次密碼")
    private String checkPassword;
    private String phoneNumber;
    private LocalDate dateOfBirth;
    private String address;
    private String gender;
    private String image;
    private String token;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime tokenExpireTime;

    // private Integer districtId;

    @Override
    public String toString() {
        return "user [Wemail=" + email + ", first_name=" + firstName + ", last_name=" + lastName
                + ", phone_number=" + phoneNumber + ", date_of_birth=" + dateOfBirth + ", address=" + address
                + ", gender=" + gender + "]";
    }
}
