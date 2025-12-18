package tw.com.ispan.eeit.ho_back.district;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import tw.com.ispan.eeit.ho_back.city.CityRepository;

import java.util.List;
import java.util.Map;

/**
 * District REST API Controller
 * 
 * 提供行政區查詢的 RESTful API 端點
 */
@RestController
@RequestMapping("/api/districts")
public class DistrictController {

    @Autowired
    private DistrictRepository districtRepository;

    @Autowired
    private CityRepository cityRepository;

    /**
     * 獲取所有行政區
     * GET /api/districts
     * 
     * @return 行政區列表
     */
    @GetMapping
    public ResponseEntity<?> getAllDistricts() {
        try {
            // 修改：使用 JOIN FETCH 預載入 City 關聯，避免 LAZY 載入問題
            List<District> districts = districtRepository.findAllWithCity();
            // 由於使用了 @JsonIgnore，避免了循環引用的問題
            return ResponseEntity.ok(districts);
        } catch (Exception e) {
            // 修改：添加錯誤處理，返回更好的錯誤訊息
            e.printStackTrace(); // 輸出錯誤堆疊以便調試
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "獲取行政區列表失敗: " + e.getMessage()));
        }
    }

    /**
     * 根據行政區名稱獲取行政區資訊
     * GET /api/districts/by-name?name={districtName}&cityName={cityName}
     * 
     * @param name     行政區名稱
     * @param cityName 城市名稱（可選，用於區分同名行政區，例如「大安區」在台北市和台中市都存在）
     * @return 行政區資訊
     */
    @GetMapping("/by-name")
    public ResponseEntity<?> getDistrictByName(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String cityName) {

        // 驗證 name 參數
        if (name == null || name.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "行政區名稱 (name) 參數不能為空"));
        }

        // 如果提供了城市名稱，先查找城市，再找該城市下的行政區
        if (cityName != null && !cityName.isBlank()) {
            // 查找城市
            var cityOpt = cityRepository.findByName(cityName);
            if (cityOpt.isPresent()) {
                // 查找該城市下的所有行政區
                List<District> districts = districtRepository.findByCity(cityOpt.get());
                // 過濾匹配的行政區名稱
                var districtOpt = districts.stream()
                        .filter(d -> d.getName().equals(name))
                        .findFirst();

                if (districtOpt.isPresent()) {
                    return ResponseEntity.ok(districtOpt.get());
                }
            }
        }

        // 如果沒有提供城市名稱或找不到，使用原來的邏輯（可能會返回第一個匹配的）
        return districtRepository.findByName(name)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 根據城市 ID 獲取該城市的所有行政區
     * GET /api/districts/by-city?cityId={cityId}
     * 
     * @param cityId 城市 ID
     * @return 行政區列表
     */
    @GetMapping("/by-city")
    public ResponseEntity<?> getDistrictsByCity(@RequestParam Integer cityId) {
        List<District> districts = districtRepository.findByCityId(cityId);
        return ResponseEntity.ok(districts);
    }

    /**
     * 根據城市名稱獲取該城市的所有行政區
     * GET /api/districts/by-city-name?cityName={cityName}
     * 
     * @param cityName 城市名稱
     * @return 行政區列表
     */
    @GetMapping("/by-city-name")
    public ResponseEntity<?> getDistrictsByCityName(@RequestParam String cityName) {
        if (cityName == null || cityName.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "城市名稱 (cityName) 參數不能為空"));
        }

        // 查找城市（先嘗試原始名稱，如果找不到則嘗試標準化後的名稱）
        var cityOpt = cityRepository.findByName(cityName);

        // 如果找不到，嘗試將「台」轉換為「臺」或「臺」轉換為「台」
        if (cityOpt.isEmpty()) {
            String normalizedName = cityName.replace("台", "臺");
            if (!normalizedName.equals(cityName)) {
                cityOpt = cityRepository.findByName(normalizedName);
            }
        }
        if (cityOpt.isEmpty()) {
            String normalizedName = cityName.replace("臺", "台");
            if (!normalizedName.equals(cityName)) {
                cityOpt = cityRepository.findByName(normalizedName);
            }
        }

        if (cityOpt.isEmpty()) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "找不到城市: " + cityName));
        }

        // 查找該城市的所有行政區
        List<District> districts = districtRepository.findByCity(cityOpt.get());
        return ResponseEntity.ok(districts);
    }
}
