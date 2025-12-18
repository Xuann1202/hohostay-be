package tw.com.ispan.eeit.ho_back.questions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import tw.com.ispan.eeit.ho_back.user.User;
import tw.com.ispan.eeit.ho_back.util.SupportTokenHelper;

@RestController
@RequestMapping("/api/questions")
public class QuestionController {

    private final QuestionService questionService;

    public QuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    /* 查全部 */
    @GetMapping
    public ResponseEntity<List<QuestionBean>> findAllQuestion() {
        return ResponseEntity.ok(questionService.findAll());
    }

    /* 查一筆問題 */
    @GetMapping("/{id}")
    public ResponseEntity<QuestionBean> findById(@PathVariable Integer id) {
        QuestionBean question = questionService.findById(id);
        if (question != null) {
            return ResponseEntity.ok(question);
        }
        return ResponseEntity.notFound().build();
    }

    /* 按分類名稱查詢（模糊搜尋） */
    @GetMapping(value = "/search", params = "categoryName")
    public ResponseEntity<List<QuestionBean>> findByCategoryName(@RequestParam String categoryName) {
        List<QuestionBean> results = questionService.findByCategoryName(categoryName);
        if (results.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(results);
    }

    /* 關鍵字查詢文章（模糊搜尋） */
    @GetMapping(value = "/search", params = "keyword")
    public ResponseEntity<List<QuestionBean>> searchQuestions(@RequestParam String keyword) {
        List<QuestionBean> results = questionService.search(keyword);
        if (results.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(results);
    }

    /* 依分類 ID 查詢文章 */
    @GetMapping(params = "categoryId")
    public ResponseEntity<List<QuestionBean>> findByCategoryId(@RequestParam Integer categoryId) {
        return ResponseEntity.ok(questionService.findByCategoryId(categoryId));
    }

    /* ➤ 新增問題 */
    @PostMapping
    public ResponseEntity<?> createQuestion(
            @RequestBody QuestionCreateRequest req,
            @RequestHeader("userId") Integer userId) {

        req.setCreatedBy(userId);

        try {
            QuestionBean created = questionService.createQuestion(req);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /* ➤ 內容圖片上傳 */
    @Value("${upload.faq.path}")
    private String faqUploadPath;

    @PostMapping("/upload/faq")
    public Map<String, String> uploadFaqImage(
            @RequestParam("file") MultipartFile file,
            @RequestHeader("userId") Integer userId) {

        User admin = new User();
        admin.setId(userId);

        String fileName = UUID.randomUUID() + "-" + file.getOriginalFilename();
        Path path = Paths.get(faqUploadPath + fileName);

        try {
            Files.createDirectories(path.getParent());
            Files.write(path, file.getBytes());
        } catch (IOException e) {
            throw new RuntimeException("圖片儲存失敗", e);
        }

        return Map.of("url", "/uploads/faq/" + fileName);
    }

    /* ➤ 修改問題 */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateQuestion(
            @PathVariable Integer id,
            @RequestBody QuestionEditRequest req,
            @RequestHeader("userId") Integer userId) {

        req.setUpdatedBy(userId);

        QuestionBean updated = questionService.updateQuestion(id, req);

        return updated != null
                ? ResponseEntity.ok(updated)
                : ResponseEntity.notFound().build();
    }

    /* ➤ 修改狀態 */
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable Integer id,
            @RequestParam Integer status,
            @RequestHeader("userId") Integer userId) {

        QuestionBean updated = questionService.updateStatus(id, status, userId);
        if (updated == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updated);
    }

    /* ➤ 刪除問題 */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteById(
            @PathVariable Integer id,
            @RequestHeader("userId") Integer userId) {

        questionService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    /* ➤ 排序功能 */
    @PutMapping("/sort")
    public ResponseEntity<?> updateSortOrder(
            @RequestBody List<QuestionSortRequest> sortList,
            @RequestHeader("userId") Integer userId) {

        questionService.updateSortOrder(sortList);
        return ResponseEntity.ok("Sort updated");
    }
}
