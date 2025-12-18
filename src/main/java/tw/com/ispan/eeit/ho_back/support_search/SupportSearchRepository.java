package tw.com.ispan.eeit.ho_back.support_search;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.*;

import tw.com.ispan.eeit.ho_back.user.User;
import tw.com.ispan.eeit.ho_back.reason.ReasonBean;
import tw.com.ispan.eeit.ho_back.scategory.SCategoryBean;
import tw.com.ispan.eeit.ho_back.support.SupportBean;

@Repository
public class SupportSearchRepository {
    @PersistenceContext
    private EntityManager em;

    public List<SupportBean> search(SupportSearchRequest req) {

        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<SupportBean> cq = cb.createQuery(SupportBean.class);
        Root<SupportBean> root = cq.from(SupportBean.class);

        // JOIN
        Join<SupportBean, User> userJoin = root.join("user", JoinType.LEFT);
        Join<SupportBean, SCategoryBean> categoryJoin = root.join("sCategory", JoinType.LEFT);
        Join<SupportBean, ReasonBean> reasonJoin = root.join("reason", JoinType.LEFT);
        Join<SupportBean, User> updaterJoin = root.join("updatedBy", JoinType.LEFT);

        List<Predicate> predicates = new ArrayList<>();

        // ---- 動態條件開始 ----

        if (req.getUserId() != null && !req.getUserId().isBlank()) {
            predicates.add(cb.equal(userJoin.get("id"), Integer.valueOf(req.getUserId())));
        }

        if (req.getUsername() != null && !req.getUsername().isBlank()) {
            predicates.add(cb.like(userJoin.get("firstName"), "%" + req.getUsername() + "%"));
        }

        if (req.getCaseCode() != null && !req.getCaseCode().isBlank()) {
            predicates.add(cb.like(root.get("caseCode"), "%" + req.getCaseCode() + "%"));
        }

        if (req.getStatus() != null && !req.getStatus().isBlank()) {
            predicates.add(cb.equal(root.get("status"), Integer.valueOf(req.getStatus())));
        }

        if (req.getCategoryId() != null && !req.getCategoryId().isBlank()) {
            predicates.add(cb.equal(categoryJoin.get("categoryId"), Integer.valueOf(req.getCategoryId())));
        }
        // 結案代碼（Reason）篩選：支援 R / C / A 群組
        if (req.getReasonId() != null && !req.getReasonId().isBlank()) {

            String rid = req.getReasonId();

            // ⭐ 字首類別：R / C / A
            if ("R".equalsIgnoreCase(rid) ||
                    "C".equalsIgnoreCase(rid) ||
                    "A".equalsIgnoreCase(rid)) {

                predicates.add(
                        cb.like(
                                cb.upper(reasonJoin.get("code")), // ← 修正為 code
                                rid.toUpperCase() + "%"));
            }

            // ⭐ 一般 ID（安全處理）
            else if (rid.matches("\\d+")) {
                predicates.add(
                        cb.equal(reasonJoin.get("id"), Integer.valueOf(rid)));
            }
        }

        if (req.getCategoryId() != null && !req.getCategoryId().isBlank()) {
            predicates.add(cb.equal(categoryJoin.get("categoryId"), Integer.valueOf(req.getCategoryId())));
        }

        if (req.getClosedBy() != null && !req.getClosedBy().isBlank()) {
            predicates.add(cb.like(updaterJoin.get("firstName"), "%" + req.getClosedBy() + "%"));
        }

        if (req.getStartDate() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("createdTime"), req.getStartDate()));
        }

        if (req.getEndDate() != null) {
            // +1 天
            Date endPlus = new Date(req.getEndDate().getTime() + 24 * 60 * 60 * 1000);
            predicates.add(cb.lessThan(root.get("createdTime"), endPlus));
        }

        // ◆ 結案時間（起）
        if (req.getCloseStart() != null) {

            // 若輸入結案起始時間 → 排除未結案
            predicates.add(cb.isNotNull(root.get("updatedTime")));

            predicates.add(cb.greaterThanOrEqualTo(
                    root.get("updatedTime"), req.getCloseStart()));
        }

        // ◆ 結案時間（迄）
        if (req.getCloseEnd() != null) {

            // 若輸入結案結束時間 → 排除未結案
            predicates.add(cb.isNotNull(root.get("updatedTime")));

            // 迄日 +1 （包含整天）
            Date endPlus = new Date(req.getCloseEnd().getTime() + 24 * 60 * 60 * 1000);

            predicates.add(cb.lessThan(
                    root.get("updatedTime"), endPlus));
        }

        // ---- 動態條件結束 ----

        cq.where(predicates.toArray(new Predicate[0]));
        cq.orderBy(cb.desc(root.get("createdTime")));

        return em.createQuery(cq).getResultList();
    }
}