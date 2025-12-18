package tw.com.ispan.eeit.ho_back.sreply;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import tw.com.ispan.eeit.ho_back.rphoto.RPhotoDTO;
import tw.com.ispan.eeit.ho_back.rphoto.RphotoBean;
import tw.com.ispan.eeit.ho_back.rphoto.RphotoRepository;
import tw.com.ispan.eeit.ho_back.support.SupportBean;
import tw.com.ispan.eeit.ho_back.support.SupportRepository;

import jakarta.transaction.Transactional;

@Service
@Transactional
public class SReplyService {

    private final SReplyRepository sReplyRepository;
    private final RphotoRepository rphotoRepository;
    private final SupportRepository supportRepository;

    @Value("${upload.reply.path}")
    private String replyUploadPath;

    public SReplyService(SReplyRepository sReplyRepository, RphotoRepository rphotoRepository,
            SupportRepository supportRepository) {
        this.sReplyRepository = sReplyRepository;
        this.rphotoRepository = rphotoRepository;
        this.supportRepository = supportRepository;
    }

    /* 查全部 */
    public List<SReplyBean> findAll() {
        return sReplyRepository.findAll();
    }

    /* 針對 SupportId 查所有回覆 */
    public List<SReplyDTO> findBySupportId(@NonNull Integer id) {
        SupportBean support = new SupportBean();
        support.setSupportId(id);

        List<SReplyBean> replies = sReplyRepository.findBySupport(support);
        List<SReplyDTO> dtoList = new ArrayList<>();

        for (SReplyBean s : replies) {
            dtoList.add(toDTO(s));
        }
        return dtoList;
    }

    /* 查一筆回覆 */
    public SReplyBean findById(Integer id) {
        if (id == null) {
            return null;
        }
        return sReplyRepository.findById(id).orElse(null);
    }

    // 暫定用來判斷管理員ID
    private boolean isAdmin(Integer userId) {
        return userId == 1; // 你現在的 admin userId
    }

    /* 新增（同時支援圖片） */
    public SReplyBean create(SReplyBean sReplyBean, List<MultipartFile> photos) {

        // 1️⃣ 先查 support ———— 避免 status = null
        Integer supportId = sReplyBean.getSupport().getSupportId();

        SupportBean realSupport = supportRepository.findById(supportId)
                .orElseThrow(() -> new RuntimeException("Support not found"));

        sReplyBean.setSupport(realSupport); // 替換掉空的 SupportBean

        // 判斷是否是管理員（userId=1）
        Integer userId = sReplyBean.getUser().getId();
        if (isAdmin(userId) && realSupport.getStatus() != null && realSupport.getStatus() == 0) {
            realSupport.setStatus(1); // 改成處理中
        }

        // 查上一筆 reply → 設 parent
        SReplyBean lastReply = sReplyRepository.findTopBySupportOrderByReplyIdDesc(realSupport);
        if (lastReply != null) {
            sReplyBean.setParent(lastReply);
        }

        SReplyBean saved = sReplyRepository.save(sReplyBean);

        if (photos != null) {
            for (MultipartFile file : photos) {
                if (!file.isEmpty()) {
                    saveReplyPhoto(saved, file);
                }
            }
        }

        return saved;
    }

    /* 修改（僅更新 content，不動照片） */
    public SReplyBean update(@NonNull Integer id, SReplyBean sReplyBean) {
        Optional<SReplyBean> optional = sReplyRepository.findById(id);

        if (optional.isPresent()) {
            SReplyBean existing = optional.get();
            existing.setContent(sReplyBean.getContent());
            return sReplyRepository.save(existing);
        }
        return null;
    }

    /* 刪除 */

    public void deleteById(@NonNull Integer id) {
        sReplyRepository.deleteById(id);
    }

    /* 上傳並儲存照片 */
    public void saveReplyPhoto(SReplyBean reply, MultipartFile file) {
        try {
            String uploadDir = replyUploadPath;

            File directory = new File(uploadDir);
            if (!directory.exists()) {
                directory.mkdirs(); // 自動建立資料夾
            }

            // 檔案名稱
            String filename = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            File destination = new File(directory, filename);
            file.transferTo(destination);

            // 儲存 DB 資料
            RphotoBean photo = new RphotoBean();
            photo.setsReply(reply);
            photo.setName(filename);
            photo.setUrl("/uploads/reply/" + filename);

            rphotoRepository.save(photo);

        } catch (Exception e) {
            throw new RuntimeException("Failed to save reply photo", e);
        }
        System.out.println("Saving photo to: " + replyUploadPath);
    }

    /* 轉 DTO */
    public SReplyDTO toDTO(SReplyBean s) {

        Hibernate.initialize(s.getPhotos());
        Hibernate.initialize(s.getUser());
        Hibernate.initialize(s.getUser().getRoles());

        SReplyDTO dto = new SReplyDTO();

        dto.setReplyId(s.getReplyId());
        dto.setContent(s.getContent());
        dto.setCreatedTime(s.getCreatedTime());
        dto.setSupportId(s.getSupport().getSupportId());
        dto.setUserName(s.getUser().getFirstName() + "," + s.getUser().getLastName());

        // ---- 角色判斷 ----
        Integer roleId = null;
        if (s.getUser() != null && s.getUser().getRoles() != null && !s.getUser().getRoles().isEmpty()) {
            roleId = s.getUser().getRoles().get(0).getId();
        }

        dto.setRoleId(roleId);

        dto.setParentId(s.getParent() != null ? s.getParent().getReplyId() : null);

        // 照片 DTO
        List<RPhotoDTO> photos = new ArrayList<>();
        if (s.getPhotos() != null) {
            for (RphotoBean p : s.getPhotos()) {
                RPhotoDTO pd = new RPhotoDTO();
                pd.setPhotoId(p.getPhotoId());
                pd.setName(p.getName());
                pd.setUrl(p.getUrl());
                pd.setUploadedTime(p.getUploadedTime());
                photos.add(pd);
            }
        }

        dto.setPhotos(photos);
        return dto;
    }
}
