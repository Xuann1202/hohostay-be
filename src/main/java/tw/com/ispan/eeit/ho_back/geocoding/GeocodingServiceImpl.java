package tw.com.ispan.eeit.ho_back.geocoding;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
// import org.springframework.stereotype.Service;  // 已停用
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

/**
 * 真實的地理編碼服務實現
 * 使用 OpenStreetMap Nominatim API（免費）
 * 
 * 注意：Nominatim API 有使用限制：
 * - 每秒最多 1 個請求
 * - 每天最多 2500 個請求
 * - 需要設置 User-Agent header
 * 
 * 如果要改用 Google Geocoding API，請：
 * 1. 註解掉 @Service 註解（或刪除此類）
 * 2. 確保 GoogleGeocodingServiceImpl 有 @Service 註解
 * 3. 在 application.yml 中配置 google.geocoding.api.key
 */
// @Service  // 如果要使用 Google API，請註解掉這行
public class GeocodingServiceImpl implements GeocodingService {

    private static final String NOMINATIM_API_URL = "https://nominatim.openstreetmap.org/search";
    private final RestTemplate restTemplate;

    public GeocodingServiceImpl() {
        this.restTemplate = new RestTemplate();
    }

    @Override
    public BigDecimal[] geocode(String address) {
        if (address == null || address.isBlank()) {
            return new BigDecimal[] { BigDecimal.ZERO, BigDecimal.ZERO };
        }

        try {
            // 組合完整地址（添加 Taiwan 以提高準確性）
            String fullAddress = address;
            if (!address.contains("台灣") && !address.contains("Taiwan") && !address.contains("臺灣")) {
                fullAddress = address + ", Taiwan";
            }

            // 構建請求 URL（增加 limit 以獲取多個結果，方便選擇最符合的）
            String url = NOMINATIM_API_URL + "?q=" +
                    java.net.URLEncoder.encode(fullAddress, java.nio.charset.StandardCharsets.UTF_8) +
                    "&format=json&limit=5&addressdetails=1&countrycodes=tw";

            // 設置請求頭（Nominatim API 要求必須設置 User-Agent）
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "HotelBookingPlatform/1.0 (Contact: admin@example.com)");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            // 發送請求
            ParameterizedTypeReference<List<Map<String, Object>>> typeRef = new ParameterizedTypeReference<List<Map<String, Object>>>() {
            };
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    typeRef);

            List<Map<String, Object>> results = response.getBody();

            // 調試：輸出搜尋結果
            System.out.println("地理編碼搜尋結果數量: " + (results != null ? results.size() : 0));
            if (results != null && !results.isEmpty()) {
                System.out.println("搜尋地址: " + fullAddress);
                for (int i = 0; i < Math.min(results.size(), 3); i++) {
                    Map<String, Object> result = results.get(i);
                    System.out.println("結果 " + (i + 1) + ": " + result.get("display_name"));
                }
            }

            if (results != null && !results.isEmpty()) {
                // 嘗試找到最符合的結果
                Map<String, Object> bestResult = null;
                int bestScore = 0;

                // 提取地址中的城市和行政區（用於驗證）
                String cityPart = "";
                if (address.contains("市")) {
                    int cityIndex = address.indexOf("市");
                    cityPart = address.substring(0, cityIndex + 1);
                } else if (address.contains("縣")) {
                    int cityIndex = address.indexOf("縣");
                    cityPart = address.substring(0, cityIndex + 1);
                }

                String districtPart = "";
                if (address.contains("區")) {
                    int districtIndex = address.indexOf("區");
                    int start = Math.max(0, districtIndex - 5);
                    districtPart = address.substring(start, districtIndex + 1);
                } else if (address.contains("鄉")) {
                    int districtIndex = address.indexOf("鄉");
                    int start = Math.max(0, districtIndex - 5);
                    districtPart = address.substring(start, districtIndex + 1);
                } else if (address.contains("鎮")) {
                    int districtIndex = address.indexOf("鎮");
                    int start = Math.max(0, districtIndex - 5);
                    districtPart = address.substring(start, districtIndex + 1);
                }

                // 標準化城市名稱（處理「台北」vs「臺北」）
                String normalizedCityPart = normalizeCityName(cityPart);

                // 為每個結果評分，選擇最符合的
                for (Map<String, Object> result : results) {
                    String displayName = (String) result.get("display_name");
                    if (displayName == null)
                        continue;

                    // 標準化顯示名稱
                    String normalizedDisplayName = normalizeCityName(displayName);

                    // 首先檢查城市是否匹配（必須匹配，否則直接跳過）
                    boolean cityMatches = false;
                    if (!cityPart.isEmpty()) {
                        // 提取城市名稱（不含「市」或「縣」）
                        String cityName = cityPart.replace("市", "").replace("縣", "");
                        String normalizedCityName = normalizeCityName(cityName);

                        // 檢查多種可能的匹配方式
                        cityMatches = displayName.contains(cityPart) || // 完整城市名稱（如「台北市」）
                                displayName.contains("臺" + cityName) || // 「臺北市」
                                displayName.contains("台" + cityName) || // 「台北市」
                                normalizedDisplayName.contains(normalizedCityPart) || // 標準化後的完整城市
                                normalizedDisplayName.contains(normalizedCityName); // 標準化後的城市名稱
                    } else {
                        // 如果地址中沒有城市信息，則所有結果都視為城市匹配
                        cityMatches = true;
                    }

                    // 如果城市不匹配，直接跳過這個結果
                    if (!cityMatches) {
                        continue;
                    }

                    int score = 0;

                    // 如果完整地址包含在結果中，給予高分
                    if (displayName.contains(address)) {
                        score += 100;
                    }
                    
                    // 檢查地址的主要部分（去除城市和行政區後的道路和門牌）
                    String addressMainPart = address;
                    if (!cityPart.isEmpty()) {
                        addressMainPart = addressMainPart.replace(cityPart, "").trim();
                    }
                    if (!districtPart.isEmpty()) {
                        addressMainPart = addressMainPart.replace(districtPart, "").trim();
                    }
                    if (!addressMainPart.isEmpty() && displayName.contains(addressMainPart)) {
                        score += 60; // 如果包含主要地址部分，給予高分
                    }

                    // 如果包含城市部分，加分（已經通過城市匹配檢查，所以這裡肯定匹配）
                    if (!cityPart.isEmpty() && displayName.contains(cityPart)) {
                        score += 50;
                    }

                    // 如果包含行政區部分，加分
                    if (!districtPart.isEmpty() && displayName.contains(districtPart)) {
                        score += 40;
                    }

                    // 檢查是否包含道路關鍵字（路、街、巷等）
                    if (address.contains("路") && displayName.contains("路")) {
                        score += 20;
                        // 如果包含「一段」、「二段」等，額外加分
                        if (address.contains("一段") && displayName.contains("一段")) {
                            score += 10;
                        } else if (address.contains("二段") && displayName.contains("二段")) {
                            score += 10;
                        } else if (address.contains("三段") && displayName.contains("三段")) {
                            score += 10;
                        }
                    } else if (address.contains("街") && displayName.contains("街")) {
                        score += 20;
                    } else if (address.contains("巷") && displayName.contains("巷")) {
                        score += 20;
                    }
                    
                    // 檢查門牌號碼是否匹配（如果地址中包含數字）
                    String addressNumber = extractNumberFromAddress(address);
                    if (addressNumber != null && !addressNumber.isEmpty() && displayName.contains(addressNumber)) {
                        score += 15;
                    }

                    // 選擇分數最高的結果
                    if (score > bestScore) {
                        bestScore = score;
                        bestResult = result;
                    }
                }

                // 調試：輸出最佳匹配結果
                if (bestResult != null) {
                    System.out.println("最佳匹配結果: " + bestResult.get("display_name") + ", 分數: " + bestScore);
                } else {
                    System.out.println("未找到匹配結果，嘗試放寬條件...");
                }

                // 如果沒有找到合理的結果，嘗試放寬條件
                if (bestResult == null || bestScore < 20) {
                    // 如果城市匹配但分數較低，仍然使用第一個城市匹配的結果
                    for (Map<String, Object> result : results) {
                        String displayName = (String) result.get("display_name");
                        if (displayName == null) continue;
                        
                        // 檢查城市是否匹配（放寬條件）
                        boolean cityMatches = false;
                        if (!cityPart.isEmpty()) {
                            String cityName = cityPart.replace("市", "").replace("縣", "");
                            String normalizedCityName = normalizeCityName(cityName);
                            String normalizedDisplayName = normalizeCityName(displayName);
                            
                            cityMatches = displayName.contains(cityPart) ||
                                    normalizedDisplayName.contains(normalizedCityName) ||
                                    displayName.contains("臺" + cityName) ||
                                    displayName.contains("台" + cityName);
                        } else {
                            cityMatches = true;
                        }
                        
                        // 如果城市匹配，計算一個基本分數
                        if (cityMatches) {
                            int fallbackScore = 30; // 城市匹配的基本分數
                            
                            // 檢查是否包含行政區
                            if (!districtPart.isEmpty() && displayName.contains(districtPart)) {
                                fallbackScore += 20;
                            }
                            
                            // 檢查是否包含道路關鍵字
                            if (address.contains("路") && displayName.contains("路")) {
                                fallbackScore += 15;
                            }
                            
                            // 如果分數更高，使用這個結果
                            if (fallbackScore > bestScore) {
                                bestScore = fallbackScore;
                                bestResult = result;
                            }
                        }
                    }
                    
                    // 如果還是找不到，嘗試使用第一個結果（只要在台灣範圍內）
                    if (bestResult == null && !results.isEmpty()) {
                        System.out.println("使用第一個搜尋結果作為備選方案");
                        bestResult = results.get(0);
                        bestScore = 10; // 設置一個基本分數
                    }
                    
                    // 如果還是找不到，拋出異常
                    if (bestResult == null) {
                        // 輸出調試信息
                        System.err.println("地址匹配失敗，搜尋結果：");
                        for (Map<String, Object> result : results) {
                            System.err.println("  - " + result.get("display_name"));
                        }
                        throw new RuntimeException("無法找到符合的地址結果，請確認地址是否正確。建議：\n1. 檢查城市和行政區是否正確\n2. 確認道路名稱和門牌號碼是否正確\n3. 嘗試使用更完整的地址格式");
                    }
                }

                String latStr = bestResult.get("lat").toString();
                String lonStr = bestResult.get("lon").toString();

                double latitude = Double.parseDouble(latStr);
                double longitude = Double.parseDouble(lonStr);

                // 驗證座標是否在台灣範圍內
                // 台灣實際範圍：緯度 21.9-25.3，經度 119.3-122.0
                // 使用稍寬的範圍以容納邊界情況：緯度 21.5-25.5，經度 119.0-122.0
                if (latitude < 21.5 || latitude > 25.5 || longitude < 119.0 || longitude > 122.0) {
                    System.err.println("警告：返回的座標超出台灣範圍: " + latitude + ", " + longitude);
                    throw new RuntimeException("返回的座標超出台灣範圍");
                }

                return new BigDecimal[] {
                        BigDecimal.valueOf(latitude).setScale(5, RoundingMode.HALF_UP),
                        BigDecimal.valueOf(longitude).setScale(5, RoundingMode.HALF_UP)
                };
            }
        } catch (RestClientException e) {
            System.err.println("地理編碼 API 調用失敗: " + e.getMessage());
            e.printStackTrace();
            // 拋出異常，讓 Controller 處理錯誤
            throw new RuntimeException("無法連接到地理編碼服務： " + e.getMessage() + 
                "。請確認網絡連接是否正常，或 OpenStreetMap Nominatim API 是否可訪問。", e);
        } catch (RuntimeException e) {
            // 如果是我們自己拋出的異常（如地址驗證失敗），直接拋出
            throw e;
        } catch (Exception e) {
            System.err.println("地理編碼處理錯誤: " + e.getMessage());
            e.printStackTrace();
            // 拋出異常，讓 Controller 處理錯誤
            throw new RuntimeException("地理編碼處理錯誤: " + e.getMessage(), e);
        }

        // 如果沒有找到結果，拋出異常
        throw new RuntimeException("無法找到符合的地址結果，請確認地址是否正確。");
    }

    @Override
    public BigDecimal[] geocode(String address, String city, String district) {
        // 先調用基本的地理編碼方法
        BigDecimal[] coordinates = geocode(address);

        // 如果提供了 city 和 district，進行驗證
        if (city != null && !city.isBlank() && district != null && !district.isBlank()) {
            try {
                // 使用反向地理編碼驗證
                double lat = coordinates[0].doubleValue();
                double lon = coordinates[1].doubleValue();

                String reverseUrl = "https://nominatim.openstreetmap.org/reverse?format=json&lat=" +
                        lat + "&lon=" + lon + "&zoom=18&addressdetails=1";

                HttpHeaders headers = new HttpHeaders();
                headers.set("User-Agent", "HotelBookingPlatform/1.0 (Contact: admin@example.com)");
                HttpEntity<String> entity = new HttpEntity<>(headers);

                ParameterizedTypeReference<Map<String, Object>> typeRef = new ParameterizedTypeReference<Map<String, Object>>() {
                };
                ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                        reverseUrl,
                        HttpMethod.GET,
                        entity,
                        typeRef);

                Map<String, Object> data = response.getBody();
                if (data != null) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> addressDetails = (Map<String, Object>) data.get("address");
                    String displayName = (String) data.get("display_name");

                    if (addressDetails != null) {
                        String returnedCity = (String) addressDetails.getOrDefault("city",
                                addressDetails.getOrDefault("county", ""));
                        String returnedDistrict = (String) addressDetails.getOrDefault("suburb",
                                addressDetails.getOrDefault("district",
                                        addressDetails.getOrDefault("city_district", "")));

                        // 標準化城市名稱（處理「台北」vs「臺北」）
                        String normalizedCity = normalizeCityName(city);
                        String normalizedReturnedCity = normalizeCityName(returnedCity);
                        String normalizedDisplayName = normalizeCityName(displayName != null ? displayName : "");

                        // 檢查城市是否匹配（使用標準化後的比較，並處理「台北」vs「臺北」）
                        boolean cityMatch = false;
                        if (displayName != null) {
                            // 檢查完整地址中是否包含城市
                            cityMatch = displayName.contains(city) ||
                                    normalizedDisplayName.contains(normalizedCity) ||
                                    displayName.contains("臺" + normalizedCity.replace("台", "")) ||
                                    displayName.contains("台" + normalizedCity.replace("臺", ""));
                        }
                        if (!cityMatch && returnedCity != null && !returnedCity.isEmpty()) {
                            cityMatch = returnedCity.contains(city) ||
                                    normalizedReturnedCity.contains(normalizedCity) ||
                                    normalizedCity.contains(normalizedReturnedCity);
                        }

                        // 檢查行政區是否匹配
                        boolean districtMatch = false;
                        if (displayName != null) {
                            // 檢查完整地址中是否包含行政區
                            districtMatch = displayName.contains(district);
                        }
                        if (!districtMatch && returnedDistrict != null && !returnedDistrict.isEmpty()) {
                            districtMatch = returnedDistrict.contains(district) ||
                                    district.contains(returnedDistrict);
                        }

                        // 城市必須匹配，行政區如果匹配更好，但不匹配也不一定錯誤（可能地址格式不同）
                        if (!cityMatch) {
                            throw new RuntimeException("地址驗證失敗：返回的地址「" + displayName +
                                    "」中的城市與選擇的「" + city + "」不一致");
                        }

                        // 如果行政區不匹配，記錄警告但不拋出異常（因為可能地址格式不同）
                        if (!districtMatch) {
                            System.err.println("地址驗證警告：返回的地址「" + displayName +
                                    "」中的行政區與選擇的「" + district + "」可能不一致，但城市匹配，繼續使用");
                        }
                    }
                }
            } catch (RestClientException e) {
                System.err.println("反向地理編碼驗證失敗: " + e.getMessage());
                // 如果反向地理編碼 API 調用失敗，記錄警告但不阻止使用座標
                // 因為可能只是 API 暫時不可用，而不是地址不正確
                System.err.println("警告：無法驗證返回的座標，但繼續使用（可能是 API 暫時不可用）");
            } catch (RuntimeException e) {
                // 如果是驗證不匹配的異常（城市不匹配），直接拋出
                if (e.getMessage() != null && e.getMessage().contains("地址驗證失敗")) {
                    throw e;
                }
                // 其他運行時異常，記錄警告但不阻止使用座標
                System.err.println("地址驗證警告: " + e.getMessage());
            } catch (Exception e) {
                System.err.println("地址驗證錯誤: " + e.getMessage());
                // 其他錯誤，記錄警告但不阻止使用座標
                System.err.println("警告：地址驗證過程中發生錯誤，但繼續使用座標");
            }
        }

        return coordinates;
    }

    /**
     * 標準化城市名稱（處理「台北」vs「臺北」）
     */
    private String normalizeCityName(String cityName) {
        if (cityName == null)
            return "";
        return cityName.replace("臺", "台").replace("台", "台");
    }

    /**
     * 從地址中提取門牌號碼
     */
    private String extractNumberFromAddress(String address) {
        if (address == null || address.isEmpty())
            return null;
        
        // 匹配數字開頭的門牌號碼（例如：「396號」、「396」）
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+)(?:號)?");
        java.util.regex.Matcher matcher = pattern.matcher(address);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

}

