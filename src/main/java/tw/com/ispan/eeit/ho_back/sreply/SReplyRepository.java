package tw.com.ispan.eeit.ho_back.sreply;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import tw.com.ispan.eeit.ho_back.support.SupportBean;

public interface SReplyRepository extends JpaRepository<SReplyBean, Integer> {

    List<SReplyBean> findBySupport(SupportBean support);

    SReplyBean findTopBySupportOrderByReplyIdDesc(SupportBean support);

}
