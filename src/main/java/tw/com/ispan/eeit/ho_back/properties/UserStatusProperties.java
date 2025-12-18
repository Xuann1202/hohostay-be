package tw.com.ispan.eeit.ho_back.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "user.status")
@PropertySource("classpath:hotel_platform.properties")
public class UserStatusProperties {
    private Integer suspend;
    private Integer notVerify;
    private Integer verify;

}