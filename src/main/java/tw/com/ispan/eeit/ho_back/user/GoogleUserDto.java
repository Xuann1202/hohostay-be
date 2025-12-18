package tw.com.ispan.eeit.ho_back.user;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GoogleUserDto {
    @JsonProperty("id")
    private String googleId;
    private String loginProvider = "GOOGLE";
    private String email;
    @JsonProperty("given_name")
    private String firstName;
    @JsonProperty("family_name")
    private String lastName;
    private String image;
}
