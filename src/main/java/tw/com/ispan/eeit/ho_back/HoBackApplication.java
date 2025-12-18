package tw.com.ispan.eeit.ho_back;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class HoBackApplication {

	public static void main(String[] args) {
		SpringApplication.run(HoBackApplication.class, args);
	}

}
