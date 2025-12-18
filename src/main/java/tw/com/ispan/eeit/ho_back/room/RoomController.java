package tw.com.ispan.eeit.ho_back.room;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import tw.com.ispan.eeit.ho_back.inventory.dto.HotelQueryDto;

import java.util.List;
import java.util.Map;

/**
 * 房間 REST API Controller
 */
@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    @Autowired
    private RoomService roomService;

    /**
     * 創建房間
     * POST /api/rooms?hotelId={hotelId}
     * 
     * @param hotelId 飯店 ID
     * @param dto     房間資料
     * @return 創建後的房間
     */
    @PostMapping
    public ResponseEntity<?> createRoom(
            @RequestParam Integer hotelId,
            @Valid @RequestBody RoomDTO dto) {
        RoomDTO created = roomService.createRoom(hotelId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * 更新房間
     * PUT /api/rooms/{roomId}
     * 
     * @param roomId 房間 ID
     * @param dto    更新的房間資料
     * @return 更新後的房間
     */
    @PutMapping("/{roomId}")
    public ResponseEntity<?> updateRoom(
            @PathVariable Integer roomId,
            @Valid @RequestBody RoomDTO dto) {
        RoomDTO updated = roomService.updateRoom(roomId, dto);
        return ResponseEntity.ok(updated);
    }

    /**
     * 刪除房間
     * DELETE /api/rooms/{roomId}
     * 
     * @param roomId 房間 ID
     * @return 刪除成功訊息
     */
    @DeleteMapping("/{roomId}")
    public ResponseEntity<?> deleteRoom(@PathVariable Integer roomId) {
        roomService.deleteRoom(roomId);
        return ResponseEntity.ok(Map.of("message", "房間已成功刪除"));
    }

    /**
     * 根據 ID 查詢房間
     * GET /api/rooms/{roomId}
     * 
     * @param roomId 房間 ID
     * @return 房間資料
     */
    @GetMapping("/{roomId}")
    public ResponseEntity<?> getRoomById(@PathVariable Integer roomId) {
        RoomDTO room = roomService.getRoomById(roomId);
        return ResponseEntity.ok(room);
    }

    /**
     * 根據飯店 ID 查詢所有房間（公開API，會過濾停業飯店）
     * GET /api/rooms/hotel/{hotelId}
     * 
     * @param hotelId 飯店 ID
     * @return 房間列表
     */
    @GetMapping("/hotel/{hotelId}")
    public ResponseEntity<?> getRoomsByHotelId(@PathVariable Integer hotelId) {
        List<RoomDTO> rooms = roomService.getRoomsByHotelId(hotelId);
        return ResponseEntity.ok(rooms);
    }

    /**
     * 根據飯店 ID 查詢所有房間（後台管理用，不過濾停業狀態）
     * GET /api/rooms/owner/hotel/{hotelId}
     * 
     * @param hotelId 飯店 ID
     * @return 房間列表
     */
    @GetMapping("/owner/hotel/{hotelId}")
    public ResponseEntity<?> getRoomsByHotelIdForOwner(@PathVariable Integer hotelId) {
        List<RoomDTO> rooms = roomService.getRoomsByHotelIdForOwner(hotelId);
        return ResponseEntity.ok(rooms);
    }

    @GetMapping("/roomDetail")
    public ResponseEntity<?> getRoomDetail(@RequestParam List<Integer> roomIds,
            HotelQueryDto query) {
        try {
            List<RoomDetailDto> roomDetail = roomService.findRoomInventoryByRoomIds(roomIds, query);
            return ResponseEntity.status(HttpStatus.OK).body(roomDetail);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
}
