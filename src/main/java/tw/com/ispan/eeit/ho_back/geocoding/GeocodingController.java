package tw.com.ispan.eeit.ho_back.geocoding;

// 已停用的導入（保留以便未來重新啟用）
// import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;  // 方法簽名需要
// import org.springframework.web.bind.annotation.*;
// import tw.com.ispan.eeit.ho_back.geocoding.GeocodingService;

// import java.math.BigDecimal;
// import java.util.HashMap;
import java.util.Map;  // 方法簽名需要

/**
 * 地理編碼 API Controller
 * 提供地址轉換為經緯度的 API 端點
 * 
 * 注意：前端已使用 Google Maps API 進行地理編碼，此 Controller 已不再使用。
 * 如需重新啟用，請取消註解 @RestController 和 @RequestMapping 註解。
 */
// @RestController  // 已停用：前端已使用 Google Maps API
// @RequestMapping("/api/geocode")  // 已停用
public class GeocodingController {

    // @Autowired  // 已停用
    // private GeocodingService geocodingService;  // 已停用

    /**
     * 將地址轉換為經緯度
     * POST /api/geocode
     * 
     * 注意：此方法已停用，前端已使用 Google Maps API 進行地理編碼。
     * 
     * @param request 包含完整地址的請求體 { address: string } 或 { address: string, city?:
     *                string, district?: string }（向後兼容）
     * @return 經緯度座標 { latitude: number, longitude: number, address: string }
     */
    // @PostMapping  // 已停用
    public ResponseEntity<?> geocode(@RequestBody Map<String, String> request) {
        // 此方法已停用，前端已使用 Google Maps API 進行地理編碼
        throw new UnsupportedOperationException("此 API 已停用，前端已使用 Google Maps API 進行地理編碼");
        
        /* 已停用的代碼
        String address = request.get("address");
        String city = request.get("city"); // 可選（向後兼容）
        String district = request.get("district"); // 可選（向後兼容）

        // 輸出接收到的請求參數，方便除錯
        System.out.println("=== GeocodingController 收到請求 ===");
        System.out.println("address: " + address);
        System.out.println("city: " + city);
        System.out.println("district: " + district);
        System.out.println("使用的服務: " + geocodingService.getClass().getSimpleName());

        if (address == null || address.isBlank()) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "地址不能為空");
            return ResponseEntity.badRequest().body(error);
        }

        try {
            // 簡化：如果 address 已經是完整地址，直接使用；否則組合地址
            BigDecimal[] coordinates;
            if (city != null && !city.isBlank() || district != null && !district.isBlank()) {
                // 向後兼容：如果提供了 city 或 district，使用舊的方法
                System.out.println("使用 geocode(address, city, district) 方法");
                coordinates = geocodingService.geocode(address, city, district);
            } else {
                // 簡化：直接使用完整地址
                System.out.println("使用 geocode(address) 方法");
                coordinates = geocodingService.geocode(address);
            }

            // 輸出返回的座標，方便除錯
            System.out.println("返回的座標: " + coordinates[0].doubleValue() + ", " + coordinates[1].doubleValue());
            System.out.println("=== GeocodingController 處理完成 ===");

            Map<String, Object> response = new HashMap<>();
            response.put("latitude", coordinates[0].doubleValue());
            response.put("longitude", coordinates[1].doubleValue());
            response.put("address", address);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            String errorMessage = e.getMessage();
            // 如果是地址驗證失敗，返回 400 Bad Request
            if (errorMessage != null && errorMessage.contains("地址驗證失敗")) {
                error.put("error", errorMessage);
                return ResponseEntity.badRequest().body(error);
            }
            // 其他錯誤返回 500
            error.put("error", "地理編碼失敗：" + errorMessage);
            return ResponseEntity.status(500).body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "地理編碼失敗：" + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
        */
    }
}
