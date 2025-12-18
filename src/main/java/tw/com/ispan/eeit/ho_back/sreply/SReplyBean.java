package tw.com.ispan.eeit.ho_back.sreply;

import java.util.Date;
import java.util.List;

import tw.com.ispan.eeit.ho_back.rphoto.RphotoBean;
import tw.com.ispan.eeit.ho_back.support.SupportBean;
import tw.com.ispan.eeit.ho_back.user.User;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.*;

@Entity
@Table(name = "s_reply")
@JsonInclude(JsonInclude.Include.ALWAYS)
public class SReplyBean {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reply_id")
    private Integer replyId;

    @ManyToOne
    @JoinColumn(name = "support_id", nullable = false)
    @JsonProperty("support")
    private SupportBean support;

    @ManyToOne(optional = true)
    @JoinColumn(name = "parent_id", nullable = true)
    @JsonProperty("parent")
    private SReplyBean parent;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonProperty("user")
    private User user;

    @OneToMany(mappedBy = "sReply", cascade = CascadeType.ALL)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private List<RphotoBean> photos;

    @Column(name = "content")
    private String content;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_time", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Taipei")
    private Date createdTime;

    @PrePersist
    protected void onCreate() {
        createdTime = new Date();
    }

    public Integer getReplyId() {
        return replyId;
    }

    public void setReplyId(Integer replyId) {
        this.replyId = replyId;
    }

    public SupportBean getSupport() {
        return support;
    }

    public void setSupport(SupportBean support) {
        this.support = support;
    }

    public SReplyBean getParent() {
        return parent;
    }

    public void setParent(SReplyBean parent) {
        this.parent = parent;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Date getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(Date createdTime) {
        this.createdTime = createdTime;
    }

    public List<RphotoBean> getPhotos() {
        return photos;
    }

    public void setPhotos(List<RphotoBean> photos) {
        this.photos = photos;
    }

}
