package tw.com.ispan.eeit.ho_back.hotel;

import java.util.List;
import java.util.Map;
import java.math.BigDecimal;
import java.time.LocalDate;

public interface HotelService {
        List<HotelRoomIdsDTO> getHotelAndRoomIdsByOwner(Integer userId);

        List<Integer> getHotelIdsByOwner(Integer userId);

        // --- 核心 CRUD 操作 (強化安全) ---

        // 新增：需要操作者ID進行權限檢查
        HotelDTO createHotel(Integer userId, HotelDTO dto);

        // 修改：需要操作者ID和飯店ID進行權限檢查與部分更新邏輯
        HotelDTO updateHotel(Integer userId, Integer hotelId, HotelDTO dto);

        // 刪除：需要操作者ID和飯店ID進行權限檢查
        void deleteHotel(Integer userId, Integer hotelId);

        // --- 後台房東查詢 ---
        // 獲取該用戶擁有的所有飯店 (後台用)
        List<HotelDTO> getHotelsByOwner(Integer userId);

        // 獲取該用戶擁有的所有飯店（分頁版本）
        Map<String, Object> getHotelsByOwner(Integer userId, int page, int size);

        // 獲取該用戶擁有的所有飯店（分頁版本，支持地區篩選）
        Map<String, Object> getHotelsByOwner(Integer userId, int page, int size, Integer cityId, Integer districtId);

        // 獲取該用戶擁有的所有飯店（分頁版本，支持地區篩選和排序）
        Map<String, Object> getHotelsByOwner(Integer userId, int page, int size, Integer cityId, Integer districtId,
                        String sortBy, String sortOrder);

        // 獲取該用戶擁有的所有飯店（分頁版本，支持多條件篩選和排序）
        Map<String, Object> getHotelsByOwner(Integer userId, int page, int size, Integer cityId, Integer districtId,
                        Boolean businessStatus, Integer hotelTypeId, String sortBy, String sortOrder);

        // 獲取單一飯店詳情 (後台用，需檢查擁有權)
        HotelDTO getHotelForOwner(Integer userId, Integer hotelId);

        // --- 前台公開查詢 ---
        // 根據飯店ID獲取飯店詳情 (公開用)
        HotelDTO getHotelById(Integer hotelId);
        // // 獲取所有飯店列表 (公開用)
        // List<HotelDTO> getAllHotels();

        public List<HotelDetailDto> findHotelByCity(String cityName);

        public List<GetNearbyHotelProjection> findByLonLat(BigDecimal latitude, BigDecimal longitude,
                        LocalDate checkInDate,
                        LocalDate checkOutDate, Integer guestNumber);

}
