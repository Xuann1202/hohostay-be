package tw.com.ispan.eeit.ho_back.rphoto;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/support/rphotos")
public class RphotoController {

    private final RphotoService rphotoService;

    public RphotoController(RphotoService rphotoService) {
        this.rphotoService = rphotoService;
    }

    /* 查全部回覆訊息資料庫的照片 */
    @GetMapping
    public List<RphotoBean> getAllRPhotos() {
        return rphotoService.findAll();
    }

    /* 查特定一張照片id的完整資料 */
    @GetMapping("/{id}")
    public ResponseEntity<RphotoBean> getRPhotoById(@PathVariable Integer id) {
        RphotoBean rphotoBean = rphotoService.findById(id);
        if (rphotoBean != null) {
            return ResponseEntity.status(HttpStatus.OK).body(rphotoBean);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    // 11.14 取得某回覆的所有照片
    @GetMapping("/replyId/{replyId}")
    public List<RphotoBean> getPhotosByReply(@PathVariable Integer replyId) {
        return rphotoService.findByReplyId(replyId);
    }

    // 11.14 取得圖片（原圖 byte 用）
    @GetMapping("/photo/{photoId}")
    public ResponseEntity<byte[]> getReplyPhoto(@PathVariable Integer photoId) {
        try {
            byte[] image = rphotoService.getPhotoBytes(photoId);

            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .body(image);

        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /* 11.14 移出新增方法，新增只透過reply進行 */
    // @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    // public ResponseEntity<RphotoBean> uploadPhoto(
    // @RequestParam("file") MultipartFile file,
    // @RequestParam("replyId") Integer replyId) {

    // RphotoBean photo = rphotoService.savePhoto(file, replyId);

    // return ResponseEntity.status(HttpStatus.CREATED).body(photo);
    // }

    /* 刪除 */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteRPhoto(@NonNull @PathVariable Integer id) {
        rphotoService.deleteById(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

}
