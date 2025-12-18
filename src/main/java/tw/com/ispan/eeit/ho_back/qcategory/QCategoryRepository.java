package tw.com.ispan.eeit.ho_back.qcategory;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface QCategoryRepository extends JpaRepository<QCategoryBean, Integer>, QCategoryRepositoryCustom {
    List<QCategoryBean> findByNameContainingIgnoreCase(String name);

    List<QCategoryBean> findAllByOrderBySortOrderAsc();

}
