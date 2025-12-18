package tw.com.ispan.eeit.ho_back.facility;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 設施 REST API Controller
 */
@RestController
@RequestMapping("/api/facilities")
public class FacilityController {

    @Autowired
    private FacilityRepository facilityRepository;

    /**
     * 獲取所有設施
     * GET /api/facilities
     * 
     * @return 設施列表
     */
    @GetMapping
    public ResponseEntity<?> getAllFacilities() {
        List<Facility> facilities = facilityRepository.findAll();
        List<FacilityDTO> dtos = facilities.stream()
                .map(f -> {
                    FacilityDTO dto = new FacilityDTO();
                    dto.setId(f.getId());
                    dto.setName(f.getName());
                    return dto;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /**
     * 根據 ID 獲取單一設施
     * GET /api/facilities/{id}
     * 
     * @param id 設施 ID
     * @return 設施資料
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getFacilityById(@PathVariable Integer id) {
        return facilityRepository.findById(id)
                .map(f -> {
                    FacilityDTO dto = new FacilityDTO();
                    dto.setId(f.getId());
                    dto.setName(f.getName());
                    return ResponseEntity.ok(dto);
                })
                .orElse(ResponseEntity.notFound().build());

    }
}
