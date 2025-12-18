package tw.com.ispan.eeit.ho_back.questions;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import tw.com.ispan.eeit.ho_back.qcategory.QCategoryBean;
import tw.com.ispan.eeit.ho_back.qcategory.QCategoryRepository;
import tw.com.ispan.eeit.ho_back.user.User;
import tw.com.ispan.eeit.ho_back.user.UserRepository;

@Service
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final QCategoryRepository qCategoryRepository;
    private final UserRepository userRepository;

    public QuestionService(QuestionRepository questionRepository, QCategoryRepository qCategoryRepository,
            UserRepository userRepository) {
        this.questionRepository = questionRepository;
        this.qCategoryRepository = qCategoryRepository;
        this.userRepository = userRepository;
    }

    /* 查全部 */
    public List<QuestionBean> findAll() {
        return questionRepository.findAll();
    }

    /* 查一筆 */

    public QuestionBean findById(Integer id) {
        return questionRepository.findById(id).orElse(null);
    }

    /* 依分類名稱模糊查詢 */
    public List<QuestionBean> findByCategoryName(String name) {
        return questionRepository.findByCategory_NameContainingIgnoreCase(name);
    }

    /* 依分類名稱模糊查詢 */
    public List<QuestionBean> findByCategoryId(Integer categoryId) {
        return questionRepository.findByCategory_CategoryIdAndStatusOrderBySortOrderAsc(categoryId, 1);
    }

    /* 關鍵字模糊查詢文章 */
    public List<QuestionBean> search(String keyword) {
        return questionRepository.findByTitleContainingOrContentContaining(keyword, keyword);
    }

    /* 新增 */

    public QuestionBean createQuestion(QuestionCreateRequest req) {

        // 1. 找分類
        QCategoryBean category = qCategoryRepository.findById(req.categoryId)
                .orElseThrow(() -> new RuntimeException("分類不存在 categoryId=" + req.categoryId));

        // 2. 找建立者
        User createdBy = userRepository.findById(req.createdBy)
                .orElseThrow(() -> new RuntimeException("使用者不存在 createdBy=" + req.createdBy));

        // 3. 建立 QuestionBean
        QuestionBean q = new QuestionBean();
        q.setCategory(category);
        q.setCreatedBy(createdBy);
        q.setUpdatedBy(createdBy);

        q.setTitle(req.title);
        q.setContent(req.content);
        q.setStatus(req.status != null ? req.status : 0);
        q.setSortOrder(0); // 初始排序

        // createdTime 會在 @PrePersist 自動填入
        return questionRepository.save(q);
    }

    /* 修改 */
    public QuestionBean updateQuestion(Integer id, QuestionEditRequest req) {

        QuestionBean existing = questionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("問題不存在"));

        // 修改標題
        if (req.getTitle() != null) {
            existing.setTitle(req.getTitle());
        }

        // 修改內容
        if (req.getContent() != null) {
            existing.setContent(req.getContent());
        }

        // ⭐ 修改分類
        if (req.getCategoryId() != null) {
            QCategoryBean cat = qCategoryRepository.findById(req.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("分類不存在"));
            existing.setCategory(cat);
        }

        // ⭐ 修改狀態
        if (req.getStatus() != null) {
            existing.setStatus(req.getStatus());
        }

        // ⭐ 修改人
        User admin = new User();
        admin.setId(req.getUpdatedBy());
        existing.setUpdatedBy(admin);

        existing.setUpdatedTime(new Date());

        return questionRepository.save(existing);
    }

    /* 修改狀態 */
    public QuestionBean updateStatus(Integer id, Integer status, Integer userId) {

        Optional<QuestionBean> optional = questionRepository.findById(id);
        if (optional.isPresent()) {
            QuestionBean existing = optional.get();

            existing.setStatus(status);

            // 若你未來要記錄誰更新可加這行
            // User user = userRepository.findById(userId).orElse(null);
            // existing.setUpdatedBy(user);

            existing.setUpdatedTime(new Date());

            return questionRepository.save(existing);
        }
        return null;
    }

    /* 刪除 */

    public void deleteById(Integer id) {
        questionRepository.deleteById(id);
    }

    /* 補上排序方法 */
    public void updateSortOrder(List<QuestionSortRequest> sortList) {

        List<QuestionBean> updateList = new ArrayList<>();

        for (QuestionSortRequest req : sortList) {

            QuestionBean bean = questionRepository.findById(req.getId())
                    .orElseThrow(() -> new RuntimeException("Question not found"));

            bean.setSortOrder(req.getSortOrder());

            updateList.add(bean);
        }

        questionRepository.saveAll(updateList); // ⭐⭐ 一次處理，避免 persist
    }
}
