package tw.com.ispan.eeit.ho_back.reason;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ReasonRepository extends JpaRepository<ReasonBean, Integer> {
    List<ReasonBean> findAllByOrderByCodeAsc();

    long countById(Integer id);
}
