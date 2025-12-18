package tw.com.ispan.eeit.ho_back.hoteltype;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 飯店類型 REST API Controller
 */
@RestController
@RequestMapping("/api/hotel-types")
public class HotelTypeController {

    @Autowired
    private HotelTypeRepository hotelTypeRepository;

    /**
     * 獲取所有飯店類型
     * GET /api/hotel-types
     * 
     * @return 飯店類型列表
     */
    @GetMapping
    public ResponseEntity<?> getAllHotelTypes() {
        List<HotelType> hotelTypes = hotelTypeRepository.findAll();
        // 轉換為 DTO
        List<HotelTypeDTO> dtos = hotelTypes.stream()
                .map(ht -> {
                    HotelTypeDTO dto = new HotelTypeDTO();
                    dto.setId(ht.getId());
                    dto.setType(ht.getType());
                    return dto;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /**
     * 根據 ID 獲取單一飯店類型
     * GET /api/hotel-types/{id}
     * 
     * @param id 飯店類型 ID
     * @return 飯店類型資料
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getHotelTypeById(@PathVariable Integer id) {
        return hotelTypeRepository.findById(id)
                .map(ht -> {
                    HotelTypeDTO dto = new HotelTypeDTO();
                    dto.setId(ht.getId());
                    dto.setType(ht.getType());
                    return ResponseEntity.ok(dto);
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
