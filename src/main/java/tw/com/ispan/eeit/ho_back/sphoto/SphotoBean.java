package tw.com.ispan.eeit.ho_back.sphoto;

import java.util.Date;

import tw.com.ispan.eeit.ho_back.support.SupportBean;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;

@Entity
@Table(name = "s_photos")
public class SphotoBean {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "photo_id")
    private Integer photoId;

    @ManyToOne
    @JoinColumn(name = "support_id", nullable = false)
    @JsonIgnore
    private SupportBean support;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "url", nullable = false, length = 255)
    private String url;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "uploaded_time", nullable = true)
    private Date uploadedTime;

    @PrePersist
    protected void onCreate() {
        uploadedTime = new Date();
    }

    public Integer getPhotoId() {
        return photoId;
    }

    public void setPhotoId(Integer photoId) {
        this.photoId = photoId;
    }

    public SupportBean getSupport() {
        return support;
    }

    public void setSupport(SupportBean support) {
        this.support = support;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Date getUploadedTime() {
        return uploadedTime;
    }

    public void setUploadedTime(Date uploadedTime) {
        this.uploadedTime = uploadedTime;
    }

}