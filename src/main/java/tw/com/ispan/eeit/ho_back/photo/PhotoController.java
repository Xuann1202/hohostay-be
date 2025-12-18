package tw.com.ispan.eeit.ho_back.photo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.bind.annotation.RequestParam;

import tw.com.ispan.eeit.ho_back.util.ReadImage;

@RestController
@RequestMapping("/api/photos")
public class PhotoController {

    @Autowired
    private PhotoService photoService;

    @Value("${upload.base-path}")
    private String uploadBasePath;

    /**
     * 獲取飯店的所有照片
     * GET /api/photos/hotel/{hotelId}
     */
    @GetMapping("/hotel/{hotelId}")
    public ResponseEntity<?> getPhotosByHotelId(@PathVariable Integer hotelId) {
        List<PhotoDTO> photos = photoService.getPhotosByHotelId(hotelId);
        return ResponseEntity.ok(photos);
    }

    /**
     * 根據 ID 獲取照片
     * GET /api/photos/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getPhotoById(@PathVariable Integer id) {
        PhotoDTO photo = photoService.getPhotoById(id);
        return ResponseEntity.ok(photo);
    }

    /**
     * 創建照片
     * POST /api/photos/hotel/{hotelId}
     */
    @PostMapping("/hotel/{hotelId}")
    public ResponseEntity<?> createPhoto(
            @PathVariable Integer hotelId,
            @RequestBody PhotoDTO dto) {
        // 修改：驗證 URL 是否為空
        if (dto.getUrl() == null || dto.getUrl().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "照片 URL 不能為空"));
        }
        PhotoDTO photo = photoService.createPhoto(hotelId, dto);
        return ResponseEntity.ok(photo);
    }

    /**
     * 批次創建照片
     * POST /api/photos/hotel/{hotelId}/batch
     */
    @PostMapping("/hotel/{hotelId}/batch")
    public ResponseEntity<?> createPhotos(
            @PathVariable Integer hotelId,
            @RequestBody List<PhotoDTO> dtos) {
        // 修改：添加詳細的驗證和錯誤處理
        try {
            // 驗證請求體
            if (dtos == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "請求體不能為 null"));
            }
            if (dtos.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "照片列表不能為空"));
            }

            // 驗證每個照片資料
            for (int i = 0; i < dtos.size(); i++) {
                PhotoDTO dto = dtos.get(i);
                if (dto == null) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("error", String.format("第 %d 張照片資料不能為 null", i + 1)));
                }
                String url = dto.getUrl();
                if (url == null || url.trim().isEmpty()) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("error", String.format("第 %d 張照片 URL 不能為空", i + 1)));
                }
                // 修改：驗證 URL 長度（資料庫限制為 255）
                if (url.length() > 255) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("error", String.format("第 %d 張照片 URL 長度超過 255 字元限制", i + 1)));
                }
            }

            // 調用服務層
            List<PhotoDTO> photos = photoService.createPhotos(hotelId, dtos);
            return ResponseEntity.ok(photos);
        } catch (Exception e) {
            // 修改：添加詳細的錯誤處理和日誌
            System.err.println("創建照片失敗 - HotelId: " + hotelId);
            System.err.println("錯誤訊息: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "創建照片失敗: " + e.getMessage()));
        }
    }

    /**
     * 更新照片
     * PUT /api/photos/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updatePhoto(
            @PathVariable Integer id,
            @RequestBody PhotoDTO dto) {
        PhotoDTO photo = photoService.updatePhoto(id, dto);
        return ResponseEntity.ok(photo);
    }

    /**
     * 刪除照片
     * DELETE /api/photos/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePhoto(@PathVariable Integer id) {
        photoService.deletePhoto(id);
        return ResponseEntity.ok(Map.of("message", "Photo deleted successfully"));
    }

    /**
     * 設定封面照片
     * PUT /api/photos/hotel/{hotelId}/cover/{photoId}
     */
    @PutMapping("/hotel/{hotelId}/cover/{photoId}")
    public ResponseEntity<?> setCoverPhoto(
            @PathVariable Integer hotelId,
            @PathVariable Integer photoId) {
        PhotoDTO photo = photoService.setCoverPhoto(hotelId, photoId);
        return ResponseEntity.ok(photo);
    }

    /**
     * 調整照片順序
     * PUT /api/photos/{id}/order
     */
    @PutMapping("/{id}/order")
    public ResponseEntity<?> updatePhotoOrder(
            @PathVariable Integer id,
            @RequestBody Map<String, Integer> request) {
        Integer newOrder = request.get("displayOrder");
        if (newOrder == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "displayOrder is required"));
        }
        PhotoDTO photo = photoService.updatePhotoOrder(id, newOrder);
        return ResponseEntity.ok(photo);
    }

    // 前端來拿圖片
    @GetMapping("/hotel")
    public ResponseEntity<byte[]> getPhoto(@RequestParam String photoUrl) throws IOException {
        ClassPathResource notFoundImgFile = new ClassPathResource("static/images/" + "no-image.jpg");
        System.out.println("photoUrl: " + photoUrl);
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
        if (photoUrl != null) {
            // 處理 /uploads/ 路徑（上傳的圖片）
            if (photoUrl.startsWith("/uploads/") || photoUrl.startsWith("uploads/")) {
                try {
                    // 移除前導斜線（如果有的話）
                    String cleanPath = photoUrl.startsWith("/") ? photoUrl.substring(1) : photoUrl;
                    // 構建完整路徑：uploadBasePath + 相對路徑（去掉 uploads/ 前綴）
                    String relativePath = cleanPath.replaceFirst("^uploads/", "");
                    Path filePath = Paths.get(uploadBasePath, relativePath);

                    System.out.println("嘗試讀取上傳圖片: " + photoUrl + " -> " + filePath);
                    if (Files.exists(filePath) && Files.isRegularFile(filePath)) {
                        image = Files.readAllBytes(filePath);
                        String contentType = ReadImage.getContentType(photoUrl);
                        return ResponseEntity.ok().header("Content-Type", contentType).body(image);
                    } else {
                        System.out.println("上傳圖片不存在: " + filePath);
                    }
                } catch (Exception e) {
                    System.err.println("讀取上傳圖片失敗: " + photoUrl + ", 錯誤: " + e.getMessage());
                    e.printStackTrace();
                }
            }

            // 處理 static/images/ 路徑（測試圖片或預設圖片）
            ClassPathResource imgFile = new ClassPathResource("static/images/" + photoUrl);
            System.out.println("嘗試讀取 static/images/: " + photoUrl);
            if (imgFile.exists()) {
                try {
                    // 使用 getInputStream() 而不是 getFile().toPath()，避免 JAR 打包後無法讀取
                    image = imgFile.getInputStream().readAllBytes();
                    String contentType = ReadImage.getContentType(photoUrl);
                    return ResponseEntity.ok().header("Content-Type", contentType).body(image);
                } catch (IOException e) {
                    System.err.println("讀取 static/images/ 圖片失敗: " + photoUrl + ", 錯誤: " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                System.out.println("static/images/ 圖片不存在: " + photoUrl);
            }
        }

        // 如果都找不到，返回 no-image.jpg
        try {
            image = notFoundImgFile.getInputStream().readAllBytes();
        } catch (IOException e) {
            System.err.println("讀取 no-image.jpg 失敗: " + e.getMessage());
            e.printStackTrace();
            // 如果連 no-image.jpg 都讀不到，返回空響應
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .header("Content-Type", "image/jpeg")
                .body(image);
    }

}
