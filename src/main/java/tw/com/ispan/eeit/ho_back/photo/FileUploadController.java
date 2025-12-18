package tw.com.ispan.eeit.ho_back.photo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 文件上傳 Controller
 * 修改：提供圖片上傳功能，將圖片保存到本地或雲端儲存
 * 
 * 注意：這是一個簡單的實現範例
 * 生產環境建議使用：
 * - 雲端儲存服務（AWS S3、Cloudinary、Supabase Storage 等）
 * - 或配置專門的文件伺服器
 */
@RestController
@RequestMapping("/api/upload")
public class FileUploadController {

    @Value("${upload.hotel.path}")
    private String hotelUploadPath;

    /**
     * 上傳圖片
     * POST /api/upload/image
     * 
     * @param file 圖片文件
     * @return 圖片 URL
     */
    @PostMapping("/image")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            // 驗證文件類型
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "只支援圖片格式"));
            }

            // 驗證文件大小（5MB）
            if (file.getSize() > 5 * 1024 * 1024) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "檔案大小不能超過 5MB"));
            }

            // 生成唯一檔名
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : ".jpg";
            String filename = UUID.randomUUID().toString() + extension;

            // 創建上傳目錄（如果不存在）
            // 使用 hotelUploadPath，將照片存儲在 hotel 子目錄中
            Path uploadPath = Paths.get(hotelUploadPath);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // 保存文件
            Path filePath = uploadPath.resolve(filename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // 返回文件 URL（包含 hotel 子目錄）
            String fileUrl = "/uploads/hotel/" + filename;

            Map<String, String> response = new HashMap<>();
            response.put("url", fileUrl);
            response.put("filename", filename);

            return ResponseEntity.ok(response);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "上傳失敗：" + e.getMessage()));
        }
    }
}
