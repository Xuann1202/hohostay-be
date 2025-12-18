package tw.com.ispan.eeit.ho_back.support;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SupportRepository extends JpaRepository<SupportBean, Integer> {

    @Query(value = "SELECT TOP 1 case_code FROM support WHERE case_code LIKE 'SUP-%' ORDER BY support_id DESC", nativeQuery = true)
    Optional<String> findLatestCaseCode();

    // 檢查案件編號是否存在
    boolean existsByCaseCode(String caseCode);

    Optional<SupportBean> findByCaseCode(String caseCode);

    // 用來確認案件分類底下是否已經有問題（預防任意刪除）
    @Query("SELECT COUNT(s) FROM SupportBean s WHERE s.sCategory.categoryId = :categoryId")
    long countByCategoryId(@Param("categoryId") Integer categoryId);

    long countByReason_Id(Integer reasonId);

    List<SupportBean> findByUserId(Integer userId);
}
