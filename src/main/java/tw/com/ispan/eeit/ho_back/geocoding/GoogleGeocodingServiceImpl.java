package tw.com.ispan.eeit.ho_back.geocoding;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
// import org.springframework.stereotype.Service;  // 已停用
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Google 地理編碼服務實現
 * 使用 Google Geocoding API 將地址轉換為經緯度座標
 * 
 * 注意：前端已使用 Google Maps API 進行地理編碼，此服務已不再使用。
 * 如需重新啟用，請取消註解 @Service 和 @Primary 註解。
 */
// @Service // 已停用：前端已使用 Google Maps API
// @org.springframework.context.annotation.Primary // 已停用
public class GoogleGeocodingServiceImpl implements GeocodingService {

    private static final String GOOGLE_GEOCODE_API_URL = "https://maps.googleapis.com/maps/api/geocode/json";
    private static final double TAIWAN_MIN_LAT = 21.5;
    private static final double TAIWAN_MAX_LAT = 25.5;
    private static final double TAIWAN_MIN_LNG = 119.0;
    private static final double TAIWAN_MAX_LNG = 122.0;

    private final RestTemplate restTemplate;

    @Value("${google.geocoding.api.key:}")
    private String apiKey;

    public GoogleGeocodingServiceImpl() {
        org.springframework.http.client.SimpleClientHttpRequestFactory factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000); // 增加連線超時時間
        factory.setReadTimeout(10000); // 增加讀取超時時間
        this.restTemplate = new RestTemplate(factory);
    }

    @Override
    public BigDecimal[] geocode(String address) {
        validateInput(address);
        validateApiKey();

        // 調試：確認 API Key 是否正確讀取
        if (apiKey != null && !apiKey.isBlank()) {
            String maskedKey = apiKey.length() > 10
                    ? apiKey.substring(0, 5) + "..." + apiKey.substring(apiKey.length() - 5)
                    : "***";
            System.out.println("✓ API Key 已讀取: " + maskedKey);
        } else {
            System.err.println("❌ API Key 未讀取！");
        }

        String trimmedAddress = address.trim();

        // 清理重複的城市名稱
        trimmedAddress = cleanDuplicateCityName(trimmedAddress);

        // 直接調用 Google Geocoding API，就像在 Google Maps 上搜尋一樣
        // 使用最簡單的地址格式，讓 Google API 自己處理

        // 嘗試多種地址格式（包含郵遞區號）
        // 根據成功的 API 響應，正確格式是：408台灣臺中市南屯區文心路一段500號
        String normalizedAddress = normalizeAddress(trimmedAddress); // 臺→台
        String addressWithPostalCode = addPostalCodeIfMissing(normalizedAddress, null);
        String originalWithPostalCode = addPostalCodeIfMissing(trimmedAddress, null);

        // 構建「郵遞區號 + 台灣 + 地址」格式（根據成功範例）
        String postalCodeWithTaiwan = buildPostalCodeWithTaiwanFormat(originalWithPostalCode);
        String normalizedPostalCodeWithTaiwan = buildPostalCodeWithTaiwanFormat(addressWithPostalCode);

        // 嘗試多種格式，優先使用「郵遞區號 + 台灣 + 地址」格式
        String[] addressFormats = {
                postalCodeWithTaiwan, // 優先：郵遞區號 + 台灣 + 原始地址（根據成功範例）
                normalizedPostalCodeWithTaiwan, // 郵遞區號 + 台灣 + 標準化地址
                originalWithPostalCode, // 郵遞區號 + 原始地址
                addressWithPostalCode, // 郵遞區號 + 標準化地址
                trimmedAddress + ", 台灣", // 原始地址 + 台灣
                normalizedAddress + ", 台灣", // 標準化地址 + 台灣
                trimmedAddress + ", Taiwan", // 原始地址 + Taiwan
                normalizedAddress + ", Taiwan", // 標準化地址 + Taiwan
                trimmedAddress, // 原始地址
                normalizedAddress // 標準化地址
        };

        for (String queryAddress : addressFormats) {
            System.out.println("=== 直接調用 Google Geocoding API ===");
            System.out.println("地址: " + queryAddress);

            try {
                // 構建 API 請求 URL
                String encodedAddress = URLEncoder.encode(queryAddress, StandardCharsets.UTF_8);
                String url = String.format("%s?address=%s&key=%s&language=zh-TW&region=tw",
                        GOOGLE_GEOCODE_API_URL, encodedAddress, apiKey);

                ParameterizedTypeReference<Map<String, Object>> typeRef = new ParameterizedTypeReference<Map<String, Object>>() {
                };

                ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                        url, HttpMethod.GET, null, typeRef);

                Map<String, Object> data = response.getBody();
                if (data == null) {
                    continue; // 繼續嘗試下一個格式
                }

                String status = (String) data.get("status");
                System.out.println("API 狀態: " + status);

                if ("OK".equals(status)) {
                    // 直接使用第一個結果（Google API 已經按相關性排序）
                    try {
                        return extractCoordinatesWithValidation(data, trimmedAddress);
                    } catch (RuntimeException e) {
                        // 如果這個格式的結果不夠精確，繼續嘗試下一個格式
                        System.out.println("⚠️ " + e.getMessage() + "，繼續嘗試其他格式");
                        continue;
                    }
                } else if ("REQUEST_DENIED".equals(status)) {
                    handleRequestDenied(data);
                } else {
                    // ZERO_RESULTS 或其他錯誤，繼續嘗試下一個格式
                    String errorMessage = (String) data.get("error_message");
                    System.out.println("API 狀態: " + status + (errorMessage != null ? " - " + errorMessage : ""));
                    continue;
                }
            } catch (Exception e) {
                System.err.println("嘗試地址格式失敗: " + queryAddress + " - " + e.getMessage());
                continue; // 繼續嘗試下一個格式
            }
        }

        // 所有格式都失敗
        throw new RuntimeException("無法找到地址的座標。請確認地址是否正確，或在 Google Maps 上測試此地址。");
    }

    /**
     * 提取座標並驗證精確度（拒絕 APPROXIMATE 結果）
     */
    @SuppressWarnings("unchecked")
    private BigDecimal[] extractCoordinatesWithValidation(Map<String, Object> response, String originalAddress) {
        List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("results");
        if (results == null || results.isEmpty()) {
            throw new RuntimeException("API 返回空結果");
        }

        // 嘗試找到最精確的結果
        Map<String, Object> bestResult = null;
        String bestLocationType = null;

        for (Map<String, Object> result : results) {
            String formattedAddress = (String) result.get("formatted_address");
            if (formattedAddress == null)
                continue;

            // 跳過只包含國家名稱的結果
            String normalized = formattedAddress.trim();
            if (normalized.equals("台灣") || normalized.equals("Taiwan") || normalized.equals("臺灣")) {
                continue;
            }

            Map<String, Object> geometry = (Map<String, Object>) result.get("geometry");
            if (geometry == null)
                continue;

            String locationType = (String) geometry.get("location_type");

            // 優先選擇 ROOFTOP 或 RANGE_INTERPOLATED，拒絕 APPROXIMATE
            if ("ROOFTOP".equals(locationType) || "RANGE_INTERPOLATED".equals(locationType)) {
                bestResult = result;
                bestLocationType = locationType;
                break; // 找到精確的結果，直接使用
            } else if (bestResult == null && !"APPROXIMATE".equals(locationType)) {
                // 如果還沒有找到結果，且不是 APPROXIMATE，也可以考慮
                bestResult = result;
                bestLocationType = locationType;
            }
        }

        // 如果沒有找到精確的結果，檢查是否有包含道路信息的結果
        if (bestResult == null) {
            for (Map<String, Object> result : results) {
                String formattedAddress = (String) result.get("formatted_address");
                if (formattedAddress == null)
                    continue;

                // 檢查是否包含道路信息
                if (formattedAddress.contains("路") || formattedAddress.contains("街") ||
                        formattedAddress.contains("道")) {
                    Map<String, Object> geometry = (Map<String, Object>) result.get("geometry");
                    String locationType = geometry != null ? (String) geometry.get("location_type") : null;

                    // 即使不是最精確的，如果包含道路信息也可以接受
                    if (!"APPROXIMATE".equals(locationType) ||
                            (originalAddress.contains("路") || originalAddress.contains("街") ||
                                    originalAddress.contains("道"))) {
                        bestResult = result;
                        bestLocationType = locationType;
                        System.out.println("⚠️ 使用包含道路信息的結果，location_type: " + locationType);
                        break;
                    }
                }
            }
        }

        if (bestResult == null) {
            throw new RuntimeException("無法找到精確的座標。Google API 返回的結果都是近似值（APPROXIMATE），" +
                    "請確認地址是否正確，或在 Google Maps 上測試此地址。");
        }

        // 將最佳結果移到第一位
        results.remove(bestResult);
        results.add(0, bestResult);

        System.out.println("✓ 使用最精確的結果，location_type: " + bestLocationType);
        return extractCoordinates(response);
    }

    /**
     * 移除地址中的門牌號碼（例如：文心路一段500號 -> 文心路一段）
     */
    @SuppressWarnings("unused")
    private String removeHouseNumber(String address) {
        // 移除門牌號碼模式：數字+號、數字號、No.數字等
        return address.replaceAll("\\d+號", "")
                .replaceAll("\\d+号", "")
                .replaceAll("No\\.?\\s*\\d+", "")
                .replaceAll("\\d+$", "")
                .trim();
    }

    /**
     * 如果地址中沒有郵遞區號，嘗試添加（根據城市）
     * 例如：臺中市南屯區文心路一段500號 -> 408臺中市南屯區文心路一段500號
     */
    private String addPostalCodeIfMissing(String address, String city) {
        // 檢查地址開頭是否已有郵遞區號（3位數字）
        if (address.matches("^\\d{3}.*")) {
            return address; // 已有郵遞區號，不需要添加
        }

        // 根據城市添加郵遞區號（常見的郵遞區號）
        if (city != null && !city.isBlank()) {
            String cityName = city.replace("臺", "台").replace("市", "").replace("縣", "");
            // 常見城市的郵遞區號範圍
            if (cityName.contains("台中") || cityName.contains("臺中")) {
                // 台中市南屯區的郵遞區號是 408
                return "408" + address;
            } else if (cityName.contains("台北") || cityName.contains("臺北")) {
                // 台北市的郵遞區號範圍是 100-116
                return "100" + address; // 使用常見的 100
            } else if (cityName.contains("新北")) {
                // 新北市的郵遞區號範圍是 207-253
                return "220" + address; // 使用常見的 220
            } else if (cityName.contains("桃園")) {
                return "330" + address;
            } else if (cityName.contains("台南") || cityName.contains("臺南")) {
                return "700" + address;
            } else if (cityName.contains("高雄")) {
                return "800" + address;
            }
        }

        return address; // 如果無法確定郵遞區號，返回原地址
    }

    /**
     * 構建「郵遞區號 + 台灣 + 地址」格式（根據成功的 API 響應範例）
     * 例如：408臺中市南屯區文心路一段500號 -> 408台灣臺中市南屯區文心路一段500號
     */
    private String buildPostalCodeWithTaiwanFormat(String address) {
        // 如果地址已經有郵遞區號，在郵遞區號後添加「台灣」
        if (address.matches("^\\d{3}.*")) {
            // 郵遞區號已經在開頭，在郵遞區號後插入「台灣」
            // 例如：408臺中市... -> 408台灣臺中市...
            return address.replaceFirst("^(\\d{3})", "$1台灣");
        }
        return address; // 如果沒有郵遞區號，返回原地址
    }

    @Override
    public BigDecimal[] geocode(String address, String city, String district) {
        // 如果提供了 city，使用它來添加郵遞區號
        validateInput(address);
        validateApiKey();

        String trimmedAddress = address.trim();

        // 清理重複的城市名稱
        trimmedAddress = cleanDuplicateCityName(trimmedAddress);

        // 嘗試多種地址格式（包含郵遞區號，使用提供的 city 參數）
        // 根據成功的 API 響應，正確格式是：408台灣臺中市南屯區文心路一段500號
        String normalizedAddress = normalizeAddress(trimmedAddress); // 臺→台
        String addressWithPostalCode = addPostalCodeIfMissing(normalizedAddress, city);
        String originalWithPostalCode = addPostalCodeIfMissing(trimmedAddress, city);

        // 構建「郵遞區號 + 台灣 + 地址」格式（根據成功範例）
        String postalCodeWithTaiwan = buildPostalCodeWithTaiwanFormat(originalWithPostalCode);
        String normalizedPostalCodeWithTaiwan = buildPostalCodeWithTaiwanFormat(addressWithPostalCode);

        // 嘗試多種格式，優先使用「郵遞區號 + 台灣 + 地址」格式
        String[] addressFormats = {
                postalCodeWithTaiwan, // 優先：郵遞區號 + 台灣 + 原始地址（根據成功範例）
                normalizedPostalCodeWithTaiwan, // 郵遞區號 + 台灣 + 標準化地址
                originalWithPostalCode, // 郵遞區號 + 原始地址
                addressWithPostalCode, // 郵遞區號 + 標準化地址
                trimmedAddress + ", 台灣", // 原始地址 + 台灣
                normalizedAddress + ", 台灣", // 標準化地址 + 台灣
                trimmedAddress + ", Taiwan", // 原始地址 + Taiwan
                normalizedAddress + ", Taiwan", // 標準化地址 + Taiwan
                trimmedAddress, // 原始地址
                normalizedAddress // 標準化地址
        };

        for (String queryAddress : addressFormats) {
            System.out.println("=== 直接調用 Google Geocoding API ===");
            System.out.println("地址: " + queryAddress);

            try {
                // 構建 API 請求 URL
                String encodedAddress = URLEncoder.encode(queryAddress, StandardCharsets.UTF_8);
                String url = String.format("%s?address=%s&key=%s&language=zh-TW&region=tw",
                        GOOGLE_GEOCODE_API_URL, encodedAddress, apiKey);

                ParameterizedTypeReference<Map<String, Object>> typeRef = new ParameterizedTypeReference<Map<String, Object>>() {
                };

                ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                        url, HttpMethod.GET, null, typeRef);

                Map<String, Object> data = response.getBody();
                if (data == null) {
                    System.err.println("❌ API 返回空響應");
                    continue;
                }

                String status = (String) data.get("status");
                String errorMessage = (String) data.get("error_message");

                // 輸出完整的響應（僅在調試時）
                System.out.println("API 狀態: " + status + (errorMessage != null ? " - " + errorMessage : ""));
                if (!"OK".equals(status)) {
                    // 輸出響應的完整內容以便調試
                    System.out.println("完整響應: " + data.toString());

                    // 如果是 ZERO_RESULTS，建議用戶檢查地址格式
                    if ("ZERO_RESULTS".equals(status)) {
                        System.out.println("💡 提示：如果這個地址在 Google Maps 上可以找到，可能是 API Key 的限制問題");
                        System.out.println("   請檢查 Google Cloud Console 中的 API Key 設定：");
                        System.out.println("   1. 應用程式限制是否設定為「無」或包含您的 IP 地址");
                        System.out.println("   2. API 配額是否已用完");
                    }
                }

                if ("OK".equals(status)) {
                    try {
                        return extractCoordinatesWithValidation(data, trimmedAddress);
                    } catch (RuntimeException e) {
                        System.out.println("⚠️ " + e.getMessage() + "，繼續嘗試其他格式");
                        continue;
                    }
                } else if ("REQUEST_DENIED".equals(status)) {
                    handleRequestDenied(data);
                } else if ("OVER_QUERY_LIMIT".equals(status)) {
                    // API 配額已用完或請求頻率過高
                    System.err.println("⚠️ Google Geocoding API 配額已用完或請求頻率過高");
                    System.err.println("錯誤訊息: " + (errorMessage != null ? errorMessage : "無詳細錯誤訊息"));
                    System.err.println("解決方案：");
                    System.err.println("1. 檢查 Google Cloud Console 中的 API 配額設定");
                    System.err.println("2. 確認是否超過每分鐘/每天的請求限制");
                    System.err.println("3. 考慮升級 API 配額或添加請求延遲");
                    throw new RuntimeException("Google Geocoding API 配額已用完，請稍後再試");
                } else if ("ZERO_RESULTS".equals(status)) {
                    // 地址找不到，繼續嘗試下一個格式
                    System.out.println("⚠️ 地址格式 \"" + queryAddress + "\" 找不到結果，繼續嘗試其他格式");
                    continue;
                } else {
                    // 其他錯誤狀態
                    System.err.println("⚠️ Google Geocoding API 返回錯誤狀態: " + status);
                    System.err.println("錯誤訊息: " + (errorMessage != null ? errorMessage : "無詳細錯誤訊息"));
                    continue;
                }
            } catch (Exception e) {
                System.err.println("嘗試地址格式失敗: " + queryAddress + " - " + e.getMessage());
                continue;
            }
        }

        throw new RuntimeException("無法找到地址的座標。請確認地址是否正確，或在 Google Maps 上測試此地址。");
    }

    /**
     * 使用 components 參數進行地理編碼（更精確）
     */
    @SuppressWarnings("unused")
    private BigDecimal[] geocodeWithComponents(String address, String city, String district) {
        validateInput(address);
        validateApiKey();

        String trimmedAddress = address.trim();
        String cleanedAddress = cleanDuplicateCityName(trimmedAddress);
        String normalizedAddress = normalizeAddress(cleanedAddress);

        // 構建地址格式變體（包含郵遞區號格式）
        // 台灣郵遞區號格式：3位數字 + 地址（例如：408臺中市南屯區文心路一段500號）
        String[] addressVariants = {
                cleanedAddress,
                normalizedAddress,
                // 嘗試添加郵遞區號格式（如果地址中沒有郵遞區號）
                addPostalCodeIfMissing(cleanedAddress, city),
                addPostalCodeIfMissing(normalizedAddress, city),
                cleanedAddress + ", Taiwan",
                normalizedAddress + ", Taiwan"
        };

        for (int i = 0; i < addressVariants.length; i++) {
            String addressVariant = addressVariants[i];

            try {
                // 使用 components 參數來更精確地指定地址組件
                String encodedAddress = URLEncoder.encode(addressVariant, StandardCharsets.UTF_8);

                // 構建 components 參數（只使用 country，避免過於嚴格）
                String components = "country:TW";

                String url = String.format("%s?address=%s&components=%s&key=%s&language=zh-TW&region=tw",
                        GOOGLE_GEOCODE_API_URL, encodedAddress, components, apiKey);

                System.out.printf("=== Geocoding API with Components (嘗試 %d/%d) ===%n", i + 1, addressVariants.length);
                System.out.println("地址: " + addressVariant);
                System.out.println("Components: " + components);

                ParameterizedTypeReference<Map<String, Object>> typeRef = new ParameterizedTypeReference<Map<String, Object>>() {
                };

                ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                        url, HttpMethod.GET, null, typeRef);

                Map<String, Object> data = response.getBody();
                if (data == null) {
                    continue;
                }

                String status = (String) data.get("status");
                System.out.println("狀態: " + status);

                if ("OK".equals(status)) {
                    // 檢查結果是否有效，如果有效但精確度不夠，會返回 false 繼續嘗試
                    if (isResultValid(data, cleanedAddress)) {
                        // 再次檢查精確度，如果還是 APPROXIMATE 且缺少道路信息，繼續嘗試
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> results = (List<Map<String, Object>>) data.get("results");
                        if (results != null && !results.isEmpty()) {
                            Map<String, Object> firstResult = results.get(0);
                            @SuppressWarnings("unchecked")
                            Map<String, Object> geometry = (Map<String, Object>) firstResult.get("geometry");
                            String locationType = geometry != null ? (String) geometry.get("location_type") : null;
                            String formattedAddress = (String) firstResult.get("formatted_address");

                            // 如果結果是 APPROXIMATE 且缺少道路信息，繼續嘗試其他格式
                            // 拒絕 APPROXIMATE 結果，要求更精確的座標（ROOFTOP 或 RANGE_INTERPOLATED）
                            if ("APPROXIMATE".equals(locationType) && formattedAddress != null) {
                                boolean hasRoad = formattedAddress.contains("路") || formattedAddress.contains("街") ||
                                        formattedAddress.contains("道");
                                // 如果缺少道路信息，或者即使有道路信息但還是 APPROXIMATE，都繼續嘗試
                                if (!hasRoad || (cleanedAddress.contains("路") || cleanedAddress.contains("街") ||
                                        cleanedAddress.contains("道"))) {
                                    System.out.println("⚠️ 結果精確度不夠（APPROXIMATE），繼續嘗試其他格式以獲得更精確的座標");
                                    continue;
                                }
                            }
                        }
                        return extractCoordinates(data);
                    }
                    continue;
                } else if ("REQUEST_DENIED".equals(status)) {
                    handleRequestDenied(data);
                }
            } catch (Exception e) {
                System.err.println("嘗試地址格式失敗: " + addressVariant + " - " + e.getMessage());
            }
        }

        throw new RuntimeException("使用 components 參數無法找到地址的座標");
    }

    /**
     * 驗證輸入地址
     */
    private void validateInput(String address) {
        if (address == null || address.isBlank()) {
            throw new RuntimeException("地址不能為空");
        }
    }

    /**
     * 驗證 API Key
     */
    private void validateApiKey() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new RuntimeException("Google Geocoding API Key 未配置。請在 application.yml 中設置 google.geocoding.api.key");
        }
    }

    /**
     * 清理重複的城市名稱
     * 例如：臺中市南屯區台中市文心路... -> 臺中市南屯區文心路...
     */
    private String cleanDuplicateCityName(String address) {
        // 台灣城市列表（包含「臺」和「台」兩種寫法）
        String[] cities = {
                "臺北市", "台北市", "新北市", "桃園市", "臺中市", "台中市",
                "臺南市", "台南市", "高雄市", "基隆市", "新竹市", "嘉義市",
                "新竹縣", "苗栗縣", "彰化縣", "南投縣", "雲林縣", "嘉義縣",
                "屏東縣", "宜蘭縣", "花蓮縣", "臺東縣", "台東縣", "澎湖縣", "金門縣", "連江縣"
        };

        String cleaned = address;

        // 找出第一個城市名稱
        String firstCity = null;
        int firstCityIndex = -1;
        for (String city : cities) {
            int index = cleaned.indexOf(city);
            if (index >= 0 && (firstCityIndex < 0 || index < firstCityIndex)) {
                firstCity = city;
                firstCityIndex = index;
            }
        }

        // 如果找到第一個城市，移除後面重複的城市名稱
        if (firstCity != null && firstCityIndex == 0) {
            // 找出城市名稱的結束位置（「市」或「縣」之後）
            int cityEndIndex = firstCityIndex + firstCity.length();

            // 檢查後面是否還有相同的城市名稱（可能是「臺」和「台」的不同寫法）
            for (String city : cities) {
                // 只檢查與第一個城市對應的城市（例如：臺中市和台中市）
                if (city.equals(firstCity) ||
                        (city.replace("臺", "台").equals(firstCity.replace("臺", "台")) &&
                                !city.equals(firstCity))) {
                    int duplicateIndex = cleaned.indexOf(city, cityEndIndex);
                    if (duplicateIndex > 0) {
                        // 移除重複的城市名稱
                        cleaned = cleaned.substring(0, duplicateIndex) +
                                cleaned.substring(duplicateIndex + city.length());
                        System.out.println("移除重複的城市名稱: " + city);
                        break;
                    }
                }
            }
        }

        return cleaned;
    }

    /**
     * 標準化地址（將「臺」轉換為「台」）
     */
    private String normalizeAddress(String address) {
        return address.replace("臺", "台");
    }

    /**
     * 構建多種地址格式變體
     * 優先使用原始地址（保留「臺」），標準化地址（「台」）作為備選
     */
    @SuppressWarnings("unused")
    private String[] buildAddressVariants(String original, String normalized) {
        return new String[] {
                original, // 優先：原始地址（保留「臺」）
                normalized, // 備選：標準化地址（「台」）
                original + ", Taiwan", // 原始 + Taiwan
                normalized + ", Taiwan", // 標準化 + Taiwan
                original + ", 台灣", // 原始 + 台灣
                normalized + ", 台灣", // 標準化 + 台灣
                "台灣" + original, // 台灣 + 原始
                "台灣" + normalized, // 台灣 + 標準化
                "Taiwan, " + original, // Taiwan + 原始
                "Taiwan, " + normalized // Taiwan + 標準化
        };
    }

    /**
     * 調用 Google Geocoding API
     */
    @SuppressWarnings("unused")
    private Map<String, Object> callGeocodingApi(String address, int attempt, int total) {
        try {
            String encodedAddress = URLEncoder.encode(address, StandardCharsets.UTF_8);
            String url = String.format("%s?address=%s&key=%s&language=zh-TW&region=tw",
                    GOOGLE_GEOCODE_API_URL, encodedAddress, apiKey);

            System.out.printf("=== Geocoding API (嘗試 %d/%d) ===%n", attempt, total);
            System.out.println("地址: " + address);

            ParameterizedTypeReference<Map<String, Object>> typeRef = new ParameterizedTypeReference<Map<String, Object>>() {
            };

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url, HttpMethod.GET, null, typeRef);

            Map<String, Object> data = response.getBody();
            if (data != null) {
                String status = (String) data.get("status");
                System.out.println("狀態: " + status);
            }

            return data;
        } catch (Exception e) {
            throw new RuntimeException("調用 Geocoding API 失敗: " + e.getMessage(), e);
        }
    }

    /**
     * 檢查返回結果是否有效
     * 如果第一個結果太簡單，嘗試檢查其他結果
     */
    @SuppressWarnings("unchecked")
    private boolean isResultValid(Map<String, Object> response, String originalAddress) {
        List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("results");
        if (results == null || results.isEmpty()) {
            return false;
        }

        // 輸出所有返回結果，方便除錯
        System.out.println("返回的結果數量: " + results.size());
        for (int i = 0; i < Math.min(results.size(), 5); i++) {
            Map<String, Object> result = results.get(i);
            String formattedAddress = (String) result.get("formatted_address");
            System.out.println("  結果 " + (i + 1) + ": " + formattedAddress);
        }

        // 嘗試找到最合適的結果（不僅僅是第一個）
        for (int i = 0; i < results.size(); i++) {
            Map<String, Object> result = results.get(i);
            String formattedAddress = (String) result.get("formatted_address");
            if (formattedAddress == null) {
                continue;
            }

            String normalized = formattedAddress.trim();

            // 跳過只包含國家名稱的結果
            if (normalized.equals("台灣") || normalized.equals("Taiwan") || normalized.equals("臺灣")) {
                System.out.println("  跳過結果 " + (i + 1) + "（只有國家名稱）");
                continue;
            }

            // 檢查是否包含城市或縣
            boolean hasCityOrCounty = normalized.contains("市") || normalized.contains("縣") ||
                    normalized.contains("City") || normalized.contains("County");

            // 如果包含城市/縣，或者包含輸入地址的城市關鍵字，則認為有效
            if (hasCityOrCounty || containsCityKeyword(normalized, originalAddress)) {
                // 檢查結果的精確度
                Map<String, Object> geometry = (Map<String, Object>) result.get("geometry");
                String locationType = geometry != null ? (String) geometry.get("location_type") : null;

                // 檢查是否包含道路信息（如果輸入地址包含道路）
                boolean hasRoadInfo = false;
                if (originalAddress.contains("路") || originalAddress.contains("街") ||
                        originalAddress.contains("道") || originalAddress.contains("巷") ||
                        originalAddress.contains("弄")) {
                    hasRoadInfo = normalized.contains("路") || normalized.contains("街") ||
                            normalized.contains("道") || normalized.contains("巷") ||
                            normalized.contains("弄");
                } else {
                    // 如果輸入地址沒有道路信息，則不需要檢查
                    hasRoadInfo = true;
                }

                // 如果結果是 APPROXIMATE 且缺少道路信息，且輸入地址有道路信息，則認為不夠精確
                // 拒絕 APPROXIMATE 結果，要求更精確的座標
                if ("APPROXIMATE".equals(locationType)) {
                    if (!hasRoadInfo && (originalAddress.contains("路") || originalAddress.contains("街") ||
                            originalAddress.contains("道"))) {
                        System.out.println("  跳過結果 " + (i + 1) + "（精確度太低，缺少道路信息）");
                        continue; // 繼續尋找更精確的結果
                    } else if (!hasRoadInfo) {
                        // 即使輸入地址沒有道路信息，如果結果是 APPROXIMATE 且缺少道路信息，也跳過
                        System.out.println("  跳過結果 " + (i + 1) + "（精確度太低，APPROXIMATE 且缺少道路信息）");
                        continue;
                    }
                }

                // 如果這不是第一個結果，更新 response 中的 results，使用這個結果
                if (i > 0) {
                    System.out.println("✓ 使用更合適的結果（第 " + (i + 1) + " 個）");
                    // 將這個結果移到第一位
                    Map<String, Object> bestResult = results.remove(i);
                    results.add(0, bestResult);
                }
                return true;
            } else {
                System.out.println("  跳過結果 " + (i + 1) + "（不包含城市信息）");
            }
        }

        // 所有結果都不合適
        System.err.println("⚠️ 所有返回結果都不包含有效的城市信息，繼續嘗試其他格式");
        return false;
    }

    /**
     * 檢查返回地址是否包含輸入地址的城市關鍵字
     */
    private boolean containsCityKeyword(String formattedAddress, String originalAddress) {
        if (originalAddress.contains("市")) {
            int index = originalAddress.indexOf("市");
            String cityName = originalAddress.substring(0, index + 1);
            return formattedAddress.contains(cityName) ||
                    formattedAddress.contains(cityName.replace("臺", "台"));
        } else if (originalAddress.contains("縣")) {
            int index = originalAddress.indexOf("縣");
            String countyName = originalAddress.substring(0, index + 1);
            return formattedAddress.contains(countyName) ||
                    formattedAddress.contains(countyName.replace("臺", "台"));
        }
        return false;
    }

    /**
     * 從 API 響應中提取座標
     */
    @SuppressWarnings("unchecked")
    private BigDecimal[] extractCoordinates(Map<String, Object> response) {
        List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("results");
        if (results == null || results.isEmpty()) {
            throw new RuntimeException("API 返回空結果");
        }

        Map<String, Object> firstResult = results.get(0);
        Map<String, Object> geometry = (Map<String, Object>) firstResult.get("geometry");

        if (geometry == null) {
            throw new RuntimeException("返回結果中沒有座標信息");
        }

        Map<String, Object> location = (Map<String, Object>) geometry.get("location");
        if (location == null) {
            throw new RuntimeException("返回結果中沒有座標信息");
        }

        Double lat = ((Number) location.get("lat")).doubleValue();
        Double lng = ((Number) location.get("lng")).doubleValue();

        // 驗證座標是否在台灣範圍內
        validateCoordinates(lat, lng);

        String formattedAddress = (String) firstResult.get("formatted_address");
        String locationType = (String) geometry.get("location_type");

        System.out.println("✓ 成功取得座標");
        System.out.println("地址: " + formattedAddress);
        System.out.println("座標: " + lat + ", " + lng);
        System.out.println("精確度: " + locationType);

        if ("APPROXIMATE".equals(locationType)) {
            System.out.println("⚠️ 警告：返回的座標是近似值，可能不夠精確");
        }

        return new BigDecimal[] {
                BigDecimal.valueOf(lat).setScale(8, RoundingMode.HALF_UP),
                BigDecimal.valueOf(lng).setScale(8, RoundingMode.HALF_UP)
        };
    }

    /**
     * 驗證座標是否在台灣範圍內
     */
    private void validateCoordinates(double lat, double lng) {
        if (lat < TAIWAN_MIN_LAT || lat > TAIWAN_MAX_LAT ||
                lng < TAIWAN_MIN_LNG || lng > TAIWAN_MAX_LNG) {
            throw new RuntimeException(
                    String.format("返回的座標超出台灣範圍: %.6f, %.6f", lat, lng));
        }
    }

    /**
     * 處理 REQUEST_DENIED 錯誤
     */
    private void handleRequestDenied(Map<String, Object> response) {
        String errorMessage = (String) response.get("error_message");
        String detailedError = "Google Geocoding API Key 授權失敗: " +
                (errorMessage != null ? errorMessage : "此 IP 地址或應用程式未授權使用此 API Key");

        System.err.println("⚠️ " + detailedError);
        System.err.println("解決方案：");
        System.err.println("1. 前往 Google Cloud Console -> APIs & Services -> Credentials");
        System.err.println("2. 找到您的 API Key，點擊編輯");
        System.err.println("3. 在「應用程式限制」中，選擇「IP 位址」或「無」");
        System.err.println("4. 如果選擇「IP 位址」，請添加後端服務器的 IP 地址");
        System.err.println("5. 確認已啟用「Geocoding API」");

        throw new RuntimeException(detailedError);
    }
}
