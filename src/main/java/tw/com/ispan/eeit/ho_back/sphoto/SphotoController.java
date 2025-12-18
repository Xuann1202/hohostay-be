package tw.com.ispan.eeit.ho_back.sphoto;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/support/photos")
public class SphotoController {

    private final SphotoService sphotoService;

    public SphotoController(SphotoService sphotoService) {
        this.sphotoService = sphotoService;
    }

    /* 查全部 */
    @GetMapping
    public List<SphotoBean> getAllSPhotos() {
        return sphotoService.findAll();
    }

    /* 查一筆 */
    @GetMapping("/{id}")
    public ResponseEntity<SphotoBean> getSPhotoById(@PathVariable Integer id) {
        SphotoBean sphotoBean = sphotoService.findById(id);
        if (sphotoBean != null) {
            return ResponseEntity.status(HttpStatus.OK).body(sphotoBean);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    /* 新增 */
    @PostMapping
    public ResponseEntity<SphotoBean> createSPhoto(@RequestBody SphotoBean newSphoto) {
        SphotoBean created = sphotoService.create(newSphoto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // /* 修改 */
    // @PutMapping("/{id}")
    // public ResponseEntity<SphotoBean> updateSPhoto(@PathVariable Integer id,
    // @RequestBody SphotoBean updateBean) {
    // SphotoBean updated = sphotoService.update(id, updateBean);
    // if (updated != null) {
    // return ResponseEntity.status(HttpStatus.OK).body(updated);
    // }
    // return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    // }

    /* 刪除 */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSPhoto(@NonNull @PathVariable Integer id) {
        sphotoService.deleteById(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

}
