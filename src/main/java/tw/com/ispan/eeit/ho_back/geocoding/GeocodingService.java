package tw.com.ispan.eeit.ho_back.geocoding;

import java.math.BigDecimal;

public interface GeocodingService {
    /**
     * Convert address to latitude/longitude.
     * Returns an array: [latitude, longitude]
     */
    BigDecimal[] geocode(String address);

    /**
     * Convert address to latitude/longitude with validation.
     * Returns an array: [latitude, longitude]
     * 
     * @param address  完整地址
     * @param city     城市名稱（用於驗證，可選）
     * @param district 行政區名稱（用於驗證，可選）
     * @return 座標陣列 [緯度, 經度]
     * @throws RuntimeException 如果地址驗證失敗
     */
    BigDecimal[] geocode(String address, String city, String district);
}

