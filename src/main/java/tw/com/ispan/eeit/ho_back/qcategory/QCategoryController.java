package tw.com.ispan.eeit.ho_back.qcategory;

import java.util.List;
import java.util.Map;

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
import tw.com.ispan.eeit.ho_back.user.User;

@RestController
@RequestMapping("/api/questions/category")
public class QCategoryController {

    private final QCategoryService qCategoryService;

    public QCategoryController(QCategoryService qCategoryService) {
        this.qCategoryService = qCategoryService;
    }

    /* 查全部 */
    @GetMapping
    public ResponseEntity<List<QCategoryBean>> findAllQCategory() {
        return ResponseEntity.ok(qCategoryService.findAll());
    }

    /* 查一筆 */
    @GetMapping("/{id}")
    public ResponseEntity<QCategoryBean> findByCategoryId(@PathVariable Integer id) {
        QCategoryBean bean = qCategoryService.findById(id);
        if (bean != null) {
            return ResponseEntity.ok(bean);
        }
        return ResponseEntity.notFound().build();
    }

    /* 名稱搜尋 */
    @GetMapping("/search")
    public ResponseEntity<List<QCategoryBean>> findByCategoryName(
            @RequestParam(name = "categoryName") String name) {

        List<QCategoryBean> list = qCategoryService.findByCategoryName(name);
        return ResponseEntity.ok(list);
    }

    /* 新增分類（含 createdBy） */
    @PostMapping
    public ResponseEntity<?> createCategory(
            @RequestBody QCategoryBean qCategoryBean,
            @RequestHeader("userId") Integer userId) {

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("請先登入");
        }

        User admin = new User();
        admin.setId(userId);
        qCategoryBean.setCreatedBy(admin);

        QCategoryBean newBean = qCategoryService.create(qCategoryBean);
        return ResponseEntity.status(HttpStatus.CREATED).body(newBean);
    }

    /* 修改分類（含 updatedBy） */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateCategory(
            @PathVariable Integer id,
            @RequestBody QCategoryBean qCategoryBean,
            @RequestHeader("userId") Integer userId) {

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("請先登入");
        }

        User admin = new User();
        admin.setId(userId);
        qCategoryBean.setUpdatedBy(admin);

        QCategoryBean updated = qCategoryService.updateById(id, qCategoryBean);
        if (updated != null) {
            return ResponseEntity.ok(updated);
        }
        return ResponseEntity.notFound().build();
    }

    /* 刪除分類（不需 createdBy/updatedBy） */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteByCategoryId(
            @PathVariable Integer id,
            @RequestHeader("userId") Integer userId) {

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("請先登入");
        }

        if (qCategoryService.isCategoryInUse(id)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("此分類正在使用中，無法刪除");
        }

        qCategoryService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    /* 刪除前檢查是否使用中 */
    @GetMapping("/check/{id}")
    public ResponseEntity<?> checkCategoryInUse(@PathVariable Integer id) {
        boolean inUse = qCategoryService.isCategoryInUse(id);
        return ResponseEntity.ok(Map.of("inUse", inUse));
    }

    /* 排序更新 */
    @PutMapping("/sort")
    public ResponseEntity<?> updateSortOrder(
            @RequestBody List<QCategorySortRequest> sortList,
            @RequestHeader("userId") Integer userId) {

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("請先登入");
        }

        qCategoryService.updateSortOrder(sortList);
        return ResponseEntity.ok("Sort updated");
    }
}
