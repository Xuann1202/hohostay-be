package tw.com.ispan.eeit.ho_back.user.Controller;

import java.io.IOException;
import java.nio.file.Files;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import tw.com.ispan.eeit.ho_back.user.User;
import tw.com.ispan.eeit.ho_back.user.UserService;

import tw.com.ispan.eeit.ho_back.util.ReadImage;

@RestController
public class UserController {
    @Autowired
    UserService userService;

    @GetMapping("/users")
    public ResponseEntity<Page<User>> findAllUser(Integer pageNumber, Integer size) {
        Page<User> user = userService.findAll(pageNumber, size);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/users/search")
    public ResponseEntity<?> findByNameOrEmail(Integer pageNumber, Integer size, String keyword) {
        Page<User> user = userService.findByNameOrEmail(pageNumber, size, keyword);
        return ResponseEntity.ok(user);
    }

    // 取得使用者圖片
    @GetMapping("api/user/photo")
    public ResponseEntity<byte[]> getPhoto(@RequestParam String photoUrl) throws IOException {
        ClassPathResource notFoundImgFile = new ClassPathResource("static/images/" + "no-profile-picture.png");
        byte[] image = null;
        // 從網路取圖片
        if (photoUrl.startsWith("http://") || photoUrl.startsWith("https://")) {
            image = ReadImage.readFromUrl(photoUrl);
            System.out.println("是http://");
            if (image != null) {
                String contentType = ReadImage.getContentType(photoUrl);
                return ResponseEntity.ok().header("Content-Type", contentType).body(image);
            }
        }
        // 從本地取圖片
        if (photoUrl != null && photoUrl.length() != 0) {
            ClassPathResource imgFile = new ClassPathResource("static/images/" + photoUrl);
            System.out.println(imgFile);
            if (imgFile.exists()) {
                image = Files.readAllBytes(imgFile.getFile().toPath());
                String contentType = ReadImage.getContentType(photoUrl);
                return ResponseEntity.ok().header("Content-Type", contentType).body(image);
            }

        }
        image = Files.readAllBytes(notFoundImgFile.getFile().toPath());
        return ResponseEntity.ok()
                .header("Content-Type", "image/jpeg")
                .body(image);
    }

}
