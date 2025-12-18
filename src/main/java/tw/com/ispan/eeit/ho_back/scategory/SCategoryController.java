package tw.com.ispan.eeit.ho_back.scategory;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/support/category")
public class SCategoryController {

    private final SCategoryService sCategoryService;

    public SCategoryController(SCategoryService sCategoryService) {
        this.sCategoryService = sCategoryService;
    }

    /* 查多筆 */

    @GetMapping
    public List<SCategoryBean> getAllSCategory() {
        return sCategoryService.findAll();
    }

    /* 查一筆 */
    @GetMapping("/{id}")
    public ResponseEntity<SCategoryBean> getSCategoryById(@NonNull @PathVariable Integer id) {
        SCategoryBean category = sCategoryService.findById(id);
        if (category != null) {
            return ResponseEntity.status(HttpStatus.OK).body(category);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    /* 新增類別 */
    @PostMapping
    public ResponseEntity<SCategoryBean> createSCategory(
            @RequestBody SCategoryBean category,
            @RequestHeader("userId") Integer userId) {

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        SCategoryBean created = sCategoryService.create(category, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /* 修改類別 */

    @PutMapping("/{id}")
    public ResponseEntity<SCategoryBean> updateSCategory(
            @PathVariable Integer id,
            @RequestBody SCategoryBean category,
            @RequestHeader("userId") Integer userId) {

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        SCategoryBean updated = sCategoryService.update(id, category, userId);

        if (updated != null) {
            return ResponseEntity.ok(updated);
        }
        return ResponseEntity.notFound().build();
    }

    /* 刪除類別 */

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSCategory(
            @PathVariable Integer id,
            @RequestHeader("userId") Integer userId) {

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (sCategoryService.isCategoryInUse(id)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("此分類已有案件使用，無法刪除");
        }

        sCategoryService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

}
