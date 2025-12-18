package tw.com.ispan.eeit.ho_back.roomtypebedtype;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 房型床型配置 REST API Controller
 * 
 * 提供房型與床型組合配置的 RESTful API 端點
 */
@RestController
@RequestMapping("/api/room-type-bed-types")
public class RoomTypeBedTypeController {

    @Autowired
    private RoomTypeBedTypeService roomTypeBedTypeService;

    /**
     * 創建房型床型配置
     * POST /api/room-type-bed-types
     * 
     * @param dto 配置資料
     * @return 創建後的配置
     */
    @PostMapping
    public ResponseEntity<?> createRoomTypeBedType(
            @Valid @RequestBody RoomTypeBedTypeDTO dto) {
        RoomTypeBedTypeDTO created = roomTypeBedTypeService.createRoomTypeBedType(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * 更新房型床型配置
     * PUT /api/room-type-bed-types/{id}
     * 
     * @param id  配置 ID
     * @param dto 更新的配置資料
     * @return 更新後的配置
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateRoomTypeBedType(
            @PathVariable Integer id,
            @Valid @RequestBody RoomTypeBedTypeDTO dto) {
        RoomTypeBedTypeDTO updated = roomTypeBedTypeService.updateRoomTypeBedType(id, dto);
        return ResponseEntity.ok(updated);
    }

    /**
     * 刪除房型床型配置
     * DELETE /api/room-type-bed-types/{id}
     * 
     * @param id 配置 ID
     * @return 刪除成功訊息
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteRoomTypeBedType(@PathVariable Integer id) {
        roomTypeBedTypeService.deleteRoomTypeBedType(id);
        return ResponseEntity.ok(java.util.Map.of("message", "配置已成功刪除"));
    }

    /**
     * 根據 ID 查詢單一配置
     * GET /api/room-type-bed-types/{id}
     * 
     * @param id 配置 ID
     * @return 配置資料
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getRoomTypeBedTypeById(@PathVariable Integer id) {
        RoomTypeBedTypeDTO dto = roomTypeBedTypeService.getRoomTypeBedTypeById(id);
        return ResponseEntity.ok(dto);
    }

    /**
     * 查詢所有配置
     * GET /api/room-type-bed-types
     * 
     * @return 所有配置列表
     */
    @GetMapping
    public ResponseEntity<?> getAllRoomTypeBedTypes() {
        List<RoomTypeBedTypeDTO> configs = roomTypeBedTypeService.getAllRoomTypeBedTypes();
        return ResponseEntity.ok(configs);
    }

    /**
     * 根據房型 ID 查詢配置
     * GET /api/room-type-bed-types/room-type/{roomTypeId}
     * 
     * @param roomTypeId 房型 ID
     * @return 該房型的所有配置列表
     */
    @GetMapping("/room-type/{roomTypeId}")
    public ResponseEntity<?> getRoomTypeBedTypesByRoomTypeId(
            @PathVariable Integer roomTypeId) {
        List<RoomTypeBedTypeDTO> configs = roomTypeBedTypeService.getRoomTypeBedTypesByRoomTypeId(roomTypeId);
        return ResponseEntity.ok(configs);
    }

    /**
     * 根據床型 ID 查詢配置
     * GET /api/room-type-bed-types/bed-type/{bedTypeId}
     * 
     * @param bedTypeId 床型 ID
     * @return 該床型的所有配置列表
     */
    @GetMapping("/bed-type/{bedTypeId}")
    public ResponseEntity<?> getRoomTypeBedTypesByBedTypeId(
            @PathVariable Integer bedTypeId) {
        List<RoomTypeBedTypeDTO> configs = roomTypeBedTypeService.getRoomTypeBedTypesByBedTypeId(bedTypeId);
        return ResponseEntity.ok(configs);
    }
}
