package tw.com.ispan.eeit.ho_back.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "booking.status")
@PropertySource("classpath:hotel_platform.properties")
public class BookingStatusProperties {
    private Integer unpaid;
    private Integer paid;
    private Integer cancel;
    private Integer complete;
}
