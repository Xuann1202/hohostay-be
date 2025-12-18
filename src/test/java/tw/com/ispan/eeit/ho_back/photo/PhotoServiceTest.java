package tw.com.ispan.eeit.ho_back.photo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class PhotoServiceTest {
    @Autowired
    PhotoService photoService;

    @Test
    public void findPhotosByHotelId() {
        System.out.println(photoService.findPhotosByHotelId(1));
    }
}
