package tw.com.ispan.eeit.ho_back.reason;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import tw.com.ispan.eeit.ho_back.user.User;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@RestController
@RequestMapping("/api/reasons")
public class ReasonController {

    @Autowired
    private ReasonService reasonService;

    /* 查全部reason */
    @GetMapping
    public List<ReasonBean> getAllReasons() {
        return reasonService.findAll();
    }

    /* 查一筆reason */
    @GetMapping("/{id}")
    public ResponseEntity<ReasonBean> getReasonById(@PathVariable Integer id) {
        ReasonBean reason = reasonService.findById(id);
        if (reason != null) {
            return ResponseEntity.status(HttpStatus.OK).body(reason);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    /* 新增reason */
    @PostMapping
    public ResponseEntity<?> createReason(
            @NonNull @RequestBody ReasonBean reason,
            HttpServletRequest request, @RequestHeader("userId") Integer userId) {

        if (userId <= 0) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("請先登入");
        }

        User admin = new User();
        admin.setId(userId);
        reason.setCreatedBy(admin);

        ReasonBean created = reasonService.create(reason);
        return ResponseEntity.status(HttpStatus.OK).body(created);
    }

    /* 更新reason */
    @PutMapping("/{id}")
    public ResponseEntity<?> updatedReason(
            @NonNull @PathVariable Integer id,
            @RequestBody ReasonBean updateReason, @RequestHeader("userId") Integer userId) {

        if (userId <= 0) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("請先登入");
        }

        User admin = new User();
        admin.setId(userId);
        updateReason.setUpdatedBy(admin);

        ReasonBean updated = reasonService.update(id, updateReason);
        if (updated != null) {
            return ResponseEntity.status(HttpStatus.OK).body(updated);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    /* 刪除reason */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteReason(
            @NonNull @PathVariable Integer id,
            HttpServletRequest request, @RequestHeader("userId") Integer userId) {

        if (userId <= 0) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("請先登入");
        }
        reasonService.deleteById(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping("/check/{reasonId}")
    public Map<String, Boolean> checkReasonUse(@PathVariable Integer reasonId) {
        boolean inUse = reasonService.isReasonUsed(reasonId);
        return Map.of("inUse", inUse);
    }

}
