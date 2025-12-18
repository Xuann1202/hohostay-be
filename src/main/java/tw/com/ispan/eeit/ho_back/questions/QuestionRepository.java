package tw.com.ispan.eeit.ho_back.questions;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionRepository extends JpaRepository<QuestionBean, Integer> {

    List<QuestionBean> findByCategory_NameContainingIgnoreCase(String name);

    List<QuestionBean> findByCategory_CategoryId(Integer categoryId);

    /* 用來搜尋文章 */
    List<QuestionBean> findByTitleContainingOrContentContaining(String title, String content);

    List<QuestionBean> findByCategory_CategoryIdAndStatusOrderBySortOrderAsc(Integer categoryId, Integer status);
}
