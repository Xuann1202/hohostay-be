package tw.com.ispan.eeit.ho_back.hotel;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import tw.com.ispan.eeit.ho_back.inventory.dto.HotelQueryDto;
import tw.com.ispan.eeit.ho_back.util.OwnerAuthHelper;

/**
 * Hotel REST API Controller
 * 
 * 提供飯店管理的 RESTful API 端點
 */
@RestController
@RequestMapping("/api/hotels")
public class HotelController {

    @Autowired
    HotelService hotelService;
    @Autowired
    HotelPageService hotelPageService;

    @Autowired
    private OwnerAuthHelper ownerAuthHelper;

    /**
     * 取得房東所有飯店ID + 房型ID
     */
    @GetMapping("/user/rooms")
    public ResponseEntity<?> getHotelsAndRooms(
            @RequestHeader(value = "userId", required = false) Integer userId) {

        if (userId == null) {
            return ResponseEntity.badRequest().body("缺少 userId");
        }

        List<HotelRoomIdsDTO> result = hotelService.getHotelAndRoomIdsByOwner(userId);

        return ResponseEntity.ok(result);
    }
    // =========================================================================
    // I. 飯店 CRUD 操作 (需要身份驗證)
    // =========================================================================

    /**
     * 新增飯店
     * POST /api/hotels
     * 需要房東角色才能新增飯店
     * 
     * @param request HTTP 請求（用於獲取 JWT token 中的用戶 ID）
     * @param dto     飯店資料
     * @return 新建的飯店資料
     */
    @PostMapping
    public ResponseEntity<?> createHotel(
            HttpServletRequest request,
            @Valid @RequestBody HotelDTO dto) {
        // 統一的房東認證檢查
        ResponseEntity<?> authError = checkOwnerAuth(request);
        if (authError != null) {
            return authError;
        }

        Integer userId = ownerAuthHelper.getUserIdFromRequest(request);
        HotelDTO created = hotelService.createHotel(userId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * 修改飯店
     * PUT /api/hotels/{hotelId}
     * 需要房東角色，且只能修改自己的飯店
     * 
     * @param request HTTP 請求（用於獲取 JWT token 中的用戶 ID）
     * @param hotelId 飯店 ID
     * @param dto     更新的飯店資料
     * @return 更新後的飯店資料
     */
    @PutMapping("/{hotelId}")
    public ResponseEntity<?> updateHotel(
            HttpServletRequest request,
            @PathVariable Integer hotelId,
            @RequestBody HotelDTO dto) {
        // 統一的房東認證檢查
        ResponseEntity<?> authError = checkOwnerAuth(request);
        if (authError != null) {
            return authError;
        }

        Integer userId = ownerAuthHelper.getUserIdFromRequest(request);
        HotelDTO updated = hotelService.updateHotel(userId, hotelId, dto);
        return ResponseEntity.ok(updated);
    }

    /**
     * 刪除飯店 (邏輯刪除 - 設定 businessStatus = 0)
     * DELETE /api/hotels/{hotelId}
     * 需要房東角色，且只能刪除自己的飯店
     * 
     * @param request HTTP 請求（用於獲取 JWT token 中的用戶 ID）
     * @param hotelId 飯店 ID
     * @return 刪除成功訊息
     */
    @DeleteMapping("/{hotelId}")
    public ResponseEntity<?> deleteHotel(
            HttpServletRequest request,
            @PathVariable Integer hotelId) {
        // 統一的房東認證檢查
        ResponseEntity<?> authError = checkOwnerAuth(request);
        if (authError != null) {
            return authError;
        }

        Integer userId = ownerAuthHelper.getUserIdFromRequest(request);
        hotelService.deleteHotel(userId, hotelId);
        return ResponseEntity.ok(Map.of("message", "飯店已成功停用 (邏輯刪除)"));
    }

    // =========================================================================
    // II. 後台查詢 (業者專用)
    // =========================================================================

    /**
     * 查詢業者擁有的所有飯店（支持分頁和多條件篩選）
     * GET
     * /api/hotels/owner?page=1&size=3&cityId=1&districtId=2&businessStatus=true&hotelTypeId=1
     * 從 JWT token 獲取用戶 ID，返回該用戶擁有的飯店
     * 
     * @param request        HTTP 請求（用於獲取 JWT token 中的用戶 ID）
     * @param page           頁碼（從 1 開始），可選，默認為 1
     * @param size           每頁大小，可選，默認為 3
     * @param cityId         城市 ID（可選，用於篩選）
     * @param districtId     行政區 ID（可選，用於篩選）
     * @param businessStatus 營業狀態（可選，true=營業中，false=停業中）
     * @param hotelTypeId    飯店類型 ID（可選，用於篩選）
     * @param sortBy         排序欄位（可選）：name（名稱）、created_time（建立時間）、updated_time（更新時間）、location（地區）、business_status（營業狀態）
     * @param sortOrder      排序方向（可選）：asc（升序）、desc（降序），默認為 desc
     * @return 分頁的飯店列表
     */
    @GetMapping("/owner")
    public ResponseEntity<?> getHotelsByOwner(
            HttpServletRequest request,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "3") int size,
            @RequestParam(value = "cityId", required = false) Integer cityId,
            @RequestParam(value = "districtId", required = false) Integer districtId,
            @RequestParam(value = "businessStatus", required = false) Boolean businessStatus,
            @RequestParam(value = "hotelTypeId", required = false) Integer hotelTypeId,
            @RequestParam(value = "sortBy", required = false) String sortBy,
            @RequestParam(value = "sortOrder", defaultValue = "desc") String sortOrder) {
        // 統一的房東認證檢查
        ResponseEntity<?> authError = checkOwnerAuth(request);
        if (authError != null) {
            return authError;
        }

        Integer userId = ownerAuthHelper.getUserIdFromRequest(request);

        // 驗證分頁參數
        if (page < 1) {
            page = 1;
        }
        if (size < 1) {
            size = 3;
        }
        if (size > 50) {
            size = 50; // 限制最大每頁數量
        }

        // 驗證排序參數
        if (sortBy != null && !sortBy.isEmpty()) {
            String[] validSortFields = { "name", "created_time", "updated_time", "location", "business_status" };
            boolean isValid = false;
            for (String field : validSortFields) {
                if (field.equalsIgnoreCase(sortBy)) {
                    isValid = true;
                    break;
                }
            }
            if (!isValid) {
                sortBy = null; // 無效的排序欄位，使用默認排序
            }
        }

        if (sortOrder != null && !sortOrder.isEmpty()) {
            if (!"asc".equalsIgnoreCase(sortOrder) && !"desc".equalsIgnoreCase(sortOrder)) {
                sortOrder = "desc"; // 無效的排序方向，使用默認降序
            }
        } else {
            sortOrder = "desc";
        }

        // 讓全局異常處理器統一處理所有異常
        Map<String, Object> result = hotelService.getHotelsByOwner(userId, page, size, cityId, districtId,
                businessStatus, hotelTypeId, sortBy, sortOrder);
        return ResponseEntity.ok(result);
    }

    /**
     * 查詢業者的單一飯店詳情
     * GET /api/hotels/owner/{hotelId}
     * 從 JWT token 獲取用戶 ID，只能查詢自己的飯店
     * 
     * @param request HTTP 請求（用於獲取 JWT token 中的用戶 ID）
     * @param hotelId 飯店 ID
     * @return 飯店詳細資料
     */
    @GetMapping("/owner/{hotelId}")
    public ResponseEntity<?> getHotelForOwner(
            HttpServletRequest request,
            @PathVariable Integer hotelId) {
        // 統一的房東認證檢查
        ResponseEntity<?> authError = checkOwnerAuth(request);
        if (authError != null) {
            return authError;
        }

        Integer userId = ownerAuthHelper.getUserIdFromRequest(request);
        HotelDTO hotel = hotelService.getHotelForOwner(userId, hotelId);
        return ResponseEntity.ok(hotel);
    }

    // =========================================================================
    // III. 前台公開查詢
    // =========================================================================

    /**
     * 根據飯店 ID 查詢飯店詳情 (公開)
     * GET /api/hotels/{hotelId}
     * 
     * @param hotelId 飯店 ID
     * @return 飯店詳細資料
     */
    @GetMapping("/{hotelId}")
    public ResponseEntity<?> getHotelById(@PathVariable Integer hotelId) {
        // 讓全局異常處理器統一處理所有異常
        HotelDTO hotel = hotelService.getHotelById(hotelId);
        return ResponseEntity.ok(hotel);
    }

    // =========================================================================
    // IV. 飯店設施管理
    // =========================================================================

    /**
     * 獲取飯店的所有設施
     * GET /api/hotels/{hotelId}/facilities
     * 
     * @param hotelId 飯店 ID
     * @return 設施 ID 列表
     */
    @GetMapping("/{hotelId}/facilities")
    public ResponseEntity<?> getHotelFacilities(@PathVariable Integer hotelId) {
        HotelDTO hotel = hotelService.getHotelById(hotelId);
        return ResponseEntity.ok(hotel.getFacilityIds());
    }

    /**
     * 更新飯店的設施
     * PUT /api/hotels/{hotelId}/facilities
     * 需要房東角色，且只能修改自己的飯店
     * 
     * @param httpRequest HTTP 請求（用於獲取 JWT token 中的用戶 ID）
     * @param hotelId     飯店 ID
     * @param request     包含 facilityIds 的請求體
     * @return 更新成功訊息
     */
    @PutMapping("/{hotelId}/facilities")
    public ResponseEntity<?> updateHotelFacilities(
            HttpServletRequest httpRequest,
            @PathVariable Integer hotelId,
            @RequestBody Map<String, Object> request) {
        // 統一的房東認證檢查
        ResponseEntity<?> authError = checkOwnerAuth(httpRequest);
        if (authError != null) {
            return authError;
        }

        Integer userId = ownerAuthHelper.getUserIdFromRequest(httpRequest);

        @SuppressWarnings("unchecked")
        List<Integer> facilityIds = (List<Integer>) request.get("facilityIds");

        HotelDTO dto = new HotelDTO();
        dto.setFacilityIds(facilityIds);

        // 使用 updateHotel 方法更新設施（會檢查用戶是否擁有該飯店）
        hotelService.updateHotel(userId, hotelId, dto);

        return ResponseEntity.ok(Map.of("message", "飯店設施已更新"));
    }

    @GetMapping("/recommended")
    public ResponseEntity<?> searchRecommendedHotel(String cityName) {
        cityName = cityName.replace("台", "臺");
        List<HotelDetailDto> hotels = hotelService.findHotelByCity(cityName);
        System.out.println(hotels);
        return ResponseEntity.status(HttpStatus.OK).body(hotels);
    }

    @GetMapping("/hotelInfo")
    public ResponseEntity<?> hotelPageInfo(Integer hotelId, HotelQueryDto query) {
        try {
            HotelDetailPageDto result = hotelPageService.hotelPageInfo(hotelId, query);
            return ResponseEntity.status(HttpStatus.OK).body(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/map")
    public ResponseEntity<?> getMethodName(BigDecimal latitude, BigDecimal longitude, LocalDate checkInDate,
            LocalDate checkOutDate, Integer guestNumber) {
        List<GetNearbyHotelProjection> hotels = hotelService.findByLonLat(latitude, longitude, checkInDate,
                checkOutDate, guestNumber);
        return ResponseEntity.status(HttpStatus.OK).body(hotels);
    }

    /**
     * 統一的房東認證檢查
     */
    private ResponseEntity<?> checkOwnerAuth(HttpServletRequest request) {
        Integer userId = ownerAuthHelper.getUserIdFromRequest(request);

        if (userId == null) {
            // 調試：檢查 Authorization header
            String authHeader = request.getHeader("Authorization");
            System.err.println("HotelController.checkOwnerAuth - userId 為 null");
            System.err.println(
                    "HotelController.checkOwnerAuth - Authorization header: " + (authHeader != null ? "存在" : "不存在"));
            if (authHeader != null) {
                System.err.println("HotelController.checkOwnerAuth - Authorization header 內容: "
                        + authHeader.substring(0, Math.min(50, authHeader.length())));
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "未登入或 token 無效"));
        }

        if (!ownerAuthHelper.isOwner(request)) {
            System.err.println("HotelController.checkOwnerAuth - 用戶 " + userId + " 不是房東");
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "只有房東才能執行此操作"));
        }

        return null;
    }
}

// =========================================================================
// V. 輔助方法
// =========================================================================
// 注意：錯誤處理已移至 GlobalExceptionHandler，此處不再需要 errorResponse 方法
