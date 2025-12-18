package tw.com.ispan.eeit.ho_back.sreply;

import java.util.Date;
import java.util.List;

import tw.com.ispan.eeit.ho_back.rphoto.RPhotoDTO;
import com.fasterxml.jackson.annotation.JsonFormat;

public class SReplyDTO {

    private Integer replyId;
    private String content;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Taipei")
    private Date createdTime;

    private String userName;
    private Integer roleId;

    private Integer supportId;
    private Integer parentId;

    private List<RPhotoDTO> photos;

    public Integer getReplyId() {
        return replyId;
    }

    public void setReplyId(Integer replyId) {
        this.replyId = replyId;
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

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Integer getRoleId() {
        return roleId;
    }

    public void setRoleId(Integer roleId) {
        this.roleId = roleId;
    }

    public Integer getSupportId() {
        return supportId;
    }

    public void setSupportId(Integer supportId) {
        this.supportId = supportId;
    }

    public Integer getParentId() {
        return parentId;
    }

    public void setParentId(Integer parentId) {
        this.parentId = parentId;
    }

    public List<RPhotoDTO> getPhotos() {
        return photos;
    }

    public void setPhotos(List<RPhotoDTO> photos) {
        this.photos = photos;
    }

}
