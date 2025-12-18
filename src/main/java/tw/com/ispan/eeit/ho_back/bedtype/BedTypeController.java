
package tw.com.ispan.eeit.ho_back.bedtype;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 床型 REST API Controller
 */
@RestController
@RequestMapping("/api/bed-types")
public class BedTypeController {

    @Autowired
    private BedTypeRepository bedTypeRepository;

    /**
     * 獲取所有床型
     * GET /api/bed-types
     * 
     * @return 床型列表
     */
    @GetMapping
    public ResponseEntity<?> getAllBedTypes() {
        List<BedType> bedTypes = bedTypeRepository.findAll();
        return ResponseEntity.ok(bedTypes);
    }

    /**
     * 根據 ID 獲取單一床型
     * GET /api/bed-types/{id}
     * 
     * @param id 床型 ID
     * @return 床型資料
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getBedTypeById(@PathVariable Integer id) {
        return bedTypeRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
