package tw.com.ispan.eeit.ho_back.roomtype;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 房型分類 REST API Controller
 */
@RestController
@RequestMapping("/api/room-types")
public class RoomTypeController {

    @Autowired
    private RoomTypeRepository roomTypeRepository;

    /**
     * 獲取所有房型分類
     * GET /api/room-types
     * 
     * @return 房型分類列表
     */
    @GetMapping
    public ResponseEntity<?> getAllRoomTypes() {
        List<RoomType> roomTypes = roomTypeRepository.findAll();
        return ResponseEntity.ok(roomTypes);
    }

    /**
     * 根據 ID 獲取單一房型分類
     * GET /api/room-types/{id}
     * 
     * @param id 房型分類 ID
     * @return 房型分類資料
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getRoomTypeById(@PathVariable Integer id) {
        return roomTypeRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
