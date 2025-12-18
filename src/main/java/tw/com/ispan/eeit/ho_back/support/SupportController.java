package tw.com.ispan.eeit.ho_back.support;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import tw.com.ispan.eeit.ho_back.scategory.SCategoryBean;
import tw.com.ispan.eeit.ho_back.user.User;

@RestController
@RequestMapping("/api/support")
public class SupportController {

    private final SupportService supportService;

    public SupportController(SupportService supportService) {
        this.supportService = supportService;
    }

    /* 查全部 */
    @GetMapping
    public List<SupportBean> getAllSupports() {
        return supportService.findAll();
    }

    /* 查一筆 */

    @GetMapping("/{id}")
    public ResponseEntity<SupportBean> getSupportById(@PathVariable Integer id) {
        SupportBean supportBean = supportService.findById(id);
        if (supportBean != null) {
            return ResponseEntity.status(HttpStatus.OK).body(supportBean);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    /* 查一筆 - 依案件編號 */
    @GetMapping("/code/{caseCode}")
    public ResponseEntity<SupportBean> getSupportByCaseCode(@PathVariable String caseCode) {
        return supportService.findByCaseCode(caseCode)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /* 使用者查自己的案件 */
    @GetMapping("/mine")
    public ResponseEntity<?> getMySupports(@RequestHeader("userId") Integer userId) {

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("請先登入");
        }

        return ResponseEntity.ok(supportService.findByUserId(userId));
    }

    /* 新增 */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createSupport(
            @RequestParam("categoryId") Integer categoryId,
            @RequestParam(value = "bookingId", required = false) Integer bookingId,
            @RequestParam("title") String title,
            @RequestParam("content") String content,
            @RequestParam(value = "photos", required = false) List<MultipartFile> photos,
            @RequestHeader("userId") Integer userId) {

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("請先登入");
        }

        User user = new User();
        user.setId(userId);

        SupportBean bean = new SupportBean();
        bean.setUser(user);

        SCategoryBean cat = new SCategoryBean();
        cat.setCategoryId(categoryId);
        bean.setSCategory(cat);

        bean.setBookingId(bookingId);
        bean.setTitle(title);
        bean.setContent(content);
        bean.setStatus(0);

        SupportBean saved = supportService.create(bean, photos);

        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /* 修改 */

    @PutMapping("/{id}")
    public ResponseEntity<SupportBean> updateSupport(@NonNull @PathVariable Integer id,
            @RequestBody SupportBean supportBean) {

        SupportBean updated = supportService.update(id, supportBean);
        if (updated != null) {
            return ResponseEntity.status(HttpStatus.OK).body(updated);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

    }

    /* 客服修改類別方法 */
    @PutMapping("/{id}/category")
    public ResponseEntity<?> updateCategory(
            @PathVariable Integer id,
            @RequestBody Map<String, Integer> body) {

        Integer categoryId = body.get("categoryId");

        SupportBean updated = supportService.updateCategory(id, categoryId);

        return ResponseEntity.ok(updated);
    }

    /* 新版 結案方式 */
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable Integer id,
            @RequestBody Map<String, Object> body,
            @RequestHeader("userId") Integer adminId) {

        Integer status = (Integer) body.get("status");
        Integer reasonId = (Integer) body.get("reasonId");
        String remark = (String) body.get("remark");

        SupportBean updated = supportService.updateStatus(id, status, reasonId, remark, adminId);

        return ResponseEntity.ok(updated);
    }

    /* 刪除前判斷是否有分類底下有客服案件 */
    @GetMapping("/check/category/{id}")
    public Map<String, Boolean> checkCategoryUsage(@PathVariable Integer id) {
        boolean inUse = supportService.countByCategoryId(id) > 0;
        return Map.of("inUse", inUse);
    }

    /* 刪除 */

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSupport(@NonNull @PathVariable Integer id) {
        supportService.deleteById(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

}
