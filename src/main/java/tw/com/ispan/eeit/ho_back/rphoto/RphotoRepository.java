package tw.com.ispan.eeit.ho_back.rphoto;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import tw.com.ispan.eeit.ho_back.sreply.SReplyBean;

public interface RphotoRepository extends JpaRepository<RphotoBean, Integer> {

    List<RphotoBean> findBySReply(SReplyBean sReplyBean);

    // 用來找照片
    List<RphotoBean> findBySReply_ReplyId(Integer replyId);

}
