package tw.com.ispan.eeit.ho_back.rphoto;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;

@Service
@Transactional
public class RphotoService {

    @Value("${upload.reply.path}")
    private String uploadPath;

    private final RphotoRepository rphotoRepository;

    public RphotoService(RphotoRepository rphotoRepository) {
        this.rphotoRepository = rphotoRepository;
    }

    /* 查多筆 */
    public List<RphotoBean> findAll() {
        return rphotoRepository.findAll();
    }

    /* 查一筆 */
    public RphotoBean findById(Integer id) {
        if (id == null) {
            return null;
        }
        return rphotoRepository.findById(id).orElse(null);
    }

    // 查特定回覆底下的所有圖片
    public List<RphotoBean> findByReplyId(@NonNull Integer replyId) {
        return rphotoRepository.findBySReply_ReplyId(replyId);
    }

    // 取得單張圖片 bytes（給 Lightbox 放大用）
    public byte[] getPhotoBytes(Integer photoId) throws IOException {
        RphotoBean photo = rphotoRepository.findById(photoId)
                .orElseThrow(() -> new RuntimeException("Photo not found"));
        String fullPath = uploadPath + photo.getName();
        return Files.readAllBytes(Paths.get(fullPath));
    }

    // 11.14 移除RPhoto裡面的照片新增方法，只保留查跟刪
    // /* 新增 */
    // public RphotoBean create(@NonNull RphotoBean rphotoBean) {
    // return rphotoRepository.save(rphotoBean);
    // }

    // /* 新增照片方法 */
    // public RphotoBean savePhoto(MultipartFile file, Integer replyId) {

    // // 1. 找到對應的回覆
    // SReplyBean reply = sReplyRepository.findById(replyId)
    // .orElseThrow(() -> new RuntimeException("Reply not found"));

    // try {
    // // 2. 建立存檔名稱
    // String fileName = UUID.randomUUID().toString() + "_" +
    // file.getOriginalFilename();
    // Path folder = Paths.get(uploadPath);

    // // 3. 如果資料夾不存在，自動建立
    // if (!Files.exists(folder)) {
    // Files.createDirectories(folder);
    // }

    // // 4. 實際存檔
    // Path target = folder.resolve(fileName);
    // Files.copy(file.getInputStream(), target,
    // StandardCopyOption.REPLACE_EXISTING);

    // // 5. 存照片資料進資料庫
    // RphotoBean photo = new RphotoBean();
    // photo.setName(fileName);
    // photo.setUrl(fileName);
    // photo.setsReply(reply);

    // return rphotoRepository.save(photo);

    // } catch (IOException e) {
    // e.printStackTrace();
    // throw new RuntimeException("Failed to save image: " + e.getMessage());
    // }
    // }

    // /* 更新 */
    // public RphotoBean update(@NonNull Integer id, RphotoBean updateBean) {
    // Optional<RphotoBean> optional = rphotoRepository.findById(id);
    // if (optional.isPresent()) {
    // RphotoBean existing = optional.get();
    // existing.setName(updateBean.getName());
    // existing.setUrl(updateBean.getUrl());
    // return rphotoRepository.save(existing);
    // }
    // return null;
    // }

    /* 刪除 */
    public void deleteById(@NonNull Integer id) {
        rphotoRepository.deleteById(id);
    }

}
