package tw.com.ispan.eeit.ho_back.support;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import tw.com.ispan.eeit.ho_back.reason.ReasonBean;
import tw.com.ispan.eeit.ho_back.reason.ReasonRepository;
import tw.com.ispan.eeit.ho_back.scategory.SCategoryBean;
import tw.com.ispan.eeit.ho_back.scategory.SCategoryRepository;
import tw.com.ispan.eeit.ho_back.sphoto.SphotoBean;
import tw.com.ispan.eeit.ho_back.user.User;
import tw.com.ispan.eeit.ho_back.user.UserRepository;

import jakarta.transaction.Transactional;

@Service
@Transactional
public class SupportService {

    @Value("${upload.support.path}")
    private String supportUploadPath;

    private final SupportRepository supportRepository;
    private final ReasonRepository reasonRepository;
    private final UserRepository userRepository;
    private final SCategoryRepository sCategoryRepository;

    public SupportService(SupportRepository supportRepository,
            ReasonRepository reasonRepository,
            UserRepository userRepository, SCategoryRepository sCategoryRepository) {
        this.supportRepository = supportRepository;
        this.reasonRepository = reasonRepository;
        this.userRepository = userRepository;
        this.sCategoryRepository = sCategoryRepository;
    }

    private synchronized String generateNextCaseCode() {
        // 取得今年年份
        String currentYear = String.valueOf(java.time.Year.now().getValue());

        // 找出最新案件編號
        Optional<String> latestCaseCode = supportRepository.findLatestCaseCode();
        int nextNumber = 1;

        if (latestCaseCode.isPresent()) {
            String lastCode = latestCaseCode.get(); // e.g. SUP-2025-00012

            // 檢查是否屬於同一年
            if (lastCode.startsWith("SUP-" + currentYear)) {
                // 取出最後五位數
                String numberPart = lastCode.substring(lastCode.lastIndexOf("-") + 1);
                try {
                    nextNumber = Integer.parseInt(numberPart) + 1;
                } catch (NumberFormatException e) {
                    nextNumber = 1;
                }
            } else {
                // 如果跨年，就從 00001 開始
                nextNumber = 1;
            }
        }

        // 格式化成 SUP-2025-00001
        String caseCode = String.format("SUP-%s-%05d", currentYear, nextNumber);

        // 防止重複
        while (supportRepository.existsByCaseCode(caseCode)) {
            nextNumber++;
            caseCode = String.format("SUP-%s-%05d", currentYear, nextNumber);
        }

        return caseCode;
    }

    public List<SupportBean> findAll() {
        return supportRepository.findAll();
    }

    public SupportBean findById(Integer id) {
        if (id == null) {
            return null;
        }
        return supportRepository.findById(id).orElse(null);
    }

    public List<SupportBean> findByUserId(Integer userId) {
        return supportRepository.findByUserId(userId);
    }

    public Optional<SupportBean> findByCaseCode(String caseCode) {
        return supportRepository.findByCaseCode(caseCode);
    }

    /* 新增方法修正 */
    public SupportBean create(@NonNull SupportBean supportBean, List<MultipartFile> files) {

        // ✦ 這裡完全保留你原本的驗證 ✦
        String caseCode = generateNextCaseCode();
        supportBean.setCaseCode(caseCode);

        if (supportBean.getUser() == null || supportBean.getUser().getId() == null) {
            throw new RuntimeException("User is required");
        }

        if (supportBean.getSCategory() == null || supportBean.getSCategory().getCategoryId() == null) {
            throw new RuntimeException("Category is required");
        }

        if (!userRepository.existsById(supportBean.getUser().getId())) {
            throw new RuntimeException("User not found with id: " + supportBean.getUser().getId());
        }

        if (!sCategoryRepository.existsById(supportBean.getSCategory().getCategoryId())) {
            throw new RuntimeException("Category not found with id: " + supportBean.getSCategory().getCategoryId());
        }

        // 先存 support 主資料
        SupportBean saved = supportRepository.save(supportBean);

        // 若無圖片就直接回傳
        if (files == null || files.isEmpty()) {
            return saved;
        }

        try {
            for (MultipartFile file : files) {

                if (file.isEmpty())
                    continue;

                // 1. 準備儲存路徑
                String folderPath = supportUploadPath + saved.getSupportId() + "/";
                File folder = new File(folderPath);
                if (!folder.exists()) {
                    folder.mkdirs();
                }

                // 2. 建立檔名
                String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();

                // 3. 完整路徑（本機檔案）
                String fullPath = folderPath + fileName;

                // 4. 儲存檔案
                file.transferTo(new File(fullPath));

                // 5. 建立 Photo 實體
                SphotoBean photo = new SphotoBean();
                photo.setSupport(saved);
                photo.setName(file.getOriginalFilename());
                photo.setUrl("/uploads/support/" + saved.getSupportId() + "/" + fileName);

                // 6. 加到 support.photos
                saved.getPhotos().add(photo);
            }

            // 7. 再存一次（包含 photos）
            saved = supportRepository.save(saved);
        } catch (Exception e) {
            throw new RuntimeException("Failed to save support photo", e);
        }

        return saved;
    }

    public SupportBean update(@NonNull Integer id, SupportBean updateBean) {
        Optional<SupportBean> optional = supportRepository.findById(id);
        if (optional.isPresent()) {
            SupportBean existing = optional.get();
            existing.setStatus(updateBean.getStatus());
            existing.setRemark(updateBean.getRemark());

            if (updateBean.getReason() != null && updateBean.getReason().getId() != null) {
                Integer reasonId = updateBean.getReason().getId();
                if (!reasonRepository.existsById(reasonId)) {
                    throw new RuntimeException("Reason not found with id: " + reasonId);
                }
                ReasonBean reasonRef = new ReasonBean();
                reasonRef.setId(reasonId);
                existing.setReason(reasonRef);
            }

            return supportRepository.save(existing);
        }
        return null;
    }

    /* 新增管理員使用更新客服類別區域 */
    public SupportBean updateCategory(Integer supportId, Integer categoryId) {
        SupportBean support = supportRepository.findById(supportId)
                .orElseThrow(() -> new RuntimeException("Support not found"));
        SCategoryBean category = sCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found"));
        support.setSCategory(category);
        return supportRepository.save(support);
    }

    @Transactional
    public SupportBean updateStatus(
            Integer supportId,
            Integer newStatus,
            Integer reasonId,
            String remark,
            Integer adminId) {

        SupportBean support = supportRepository.findById(supportId)
                .orElseThrow(() -> new RuntimeException("Support not found"));

        // 狀態流檢查：只能結案，不可亂跳
        if (newStatus == 2 && support.getStatus() == 2) {
            throw new RuntimeException("此案件已結案");
        }

        // 設定狀態
        support.setStatus(newStatus);

        // --- 以下只有在 "結案" 時需要 ---
        if (newStatus == 2) {

            // ✔ 更新 reason
            if (reasonId != null) {
                ReasonBean reason = reasonRepository.findById(reasonId)
                        .orElseThrow(() -> new RuntimeException("Reason not found"));
                support.setReason(reason);
            }

            // 更新備註
            support.setRemark(remark);

            // 設定結案人員
            User admin = new User();
            admin.setId(adminId);
            support.setUpdatedBy(admin);

            // 設定結案時間
            support.setUpdatedTime(new Date());
        }
        // --- 結案判斷結束 ---

        return supportRepository.save(support);
    }

    public void deleteById(@NonNull Integer id) {
        supportRepository.deleteById(id);
    }

    // 判斷某個分類是否被 support 使用
    public long countByCategoryId(Integer categoryId) {
        return supportRepository.countByCategoryId(categoryId);
    }
}
