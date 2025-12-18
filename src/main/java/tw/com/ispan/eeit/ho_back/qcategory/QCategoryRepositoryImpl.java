package tw.com.ispan.eeit.ho_back.qcategory;

import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import tw.com.ispan.eeit.ho_back.questions.QuestionBean;

@Repository
public class QCategoryRepositoryImpl implements QCategoryRepositoryCustom {
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public boolean isCategoryInUse(Integer categoryId) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);

        // FROM questions q
        Root<QuestionBean> root = cq.from(QuestionBean.class);

        // SELECT count(q)
        cq.select(cb.count(root));

        // WHERE q.category.categoryId = :categoryId
        cq.where(
                cb.equal(root.get("category").get("categoryId"), categoryId));

        Long count = entityManager.createQuery(cq).getSingleResult();

        return count != null && count > 0;
    }
}