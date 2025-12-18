package tw.com.ispan.eeit.ho_back.sreply;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import tw.com.ispan.eeit.ho_back.support.SupportBean;
import tw.com.ispan.eeit.ho_back.user.User;

@RestController
@RequestMapping("/api/support/reply")
public class SReplyController {

    private final SReplyService sReplyService;

    public SReplyController(SReplyService sReplyService) {
        this.sReplyService = sReplyService;
    }

    /* 查全部回覆 */
    @GetMapping
    public List<SReplyDTO> findAllSReply() {
        return sReplyService.findAll()
                .stream()
                .map(sReplyService::toDTO)
                .toList();
    }

    /* 透過supportId 找所有回覆 */

    @GetMapping("/support/{supportId}")
    public List<SReplyDTO> findBySupport(@PathVariable Integer supportId) {
        return sReplyService.findBySupportId(supportId);
    }

    /* 查一筆 */
    @GetMapping("/{id}")
    public ResponseEntity<SReplyDTO> findSReplyById(@PathVariable Integer id) {
        SReplyBean reply = sReplyService.findById(id);
        if (reply != null) {
            return ResponseEntity.ok(sReplyService.toDTO(reply));
        }
        return ResponseEntity.notFound().build();
    }

    /* 新增 */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SReplyDTO> createSReply(
            @RequestParam("supportId") Integer supportId,
            @RequestParam("content") String content,
            @RequestParam(value = "parentId", required = false) Integer parentId,
            @RequestParam(value = "photos", required = false) List<MultipartFile> files,
            @RequestHeader("userId") Integer userId) {

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        SReplyBean replyBean = new SReplyBean();

        SupportBean support = new SupportBean();
        support.setSupportId(supportId);
        replyBean.setSupport(support);

        User user = new User();
        user.setId(userId);
        replyBean.setUser(user);
        /* ------------------------------ */

        replyBean.setContent(content);

        if (parentId != null) {
            SReplyBean parent = new SReplyBean();
            parent.setReplyId(parentId);
            replyBean.setParent(parent);
        }

        // 這裡改成把 file 傳給 service，一次處理
        SReplyBean savedReply = sReplyService.create(replyBean, files);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(sReplyService.toDTO(savedReply));
    }

    /* 修改內容（不處理圖片） */
    @PutMapping("/{id}")
    public ResponseEntity<SReplyBean> updateSReply(
            @PathVariable Integer id,
            @RequestBody SReplyBean body) {

        SReplyBean updated = sReplyService.update(id, body);
        if (updated != null) {
            return ResponseEntity.ok(updated);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    /* 刪除 */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSReplyById(@PathVariable Integer id) {
        sReplyService.deleteById(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}