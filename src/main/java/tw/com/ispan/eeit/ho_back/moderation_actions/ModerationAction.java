package tw.com.ispan.eeit.ho_back.moderation_actions;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "moderation_actions")
public class ModerationAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /* 舉報人ID */
    @Column(name = "reporter_id", nullable = false)
    private Long reporterId;

    /* 被舉報評論ID */
    @Column(name = "review_id", nullable = false)
    private Long reviewId;

    /* 被舉報評論的作者ID */
    @Column(name = "review_author_id", nullable = false)
    private Long reviewAuthorId;

    /* 舉報原因 */
    @Convert(converter = ReasonConverter.class)
    @Column(name = "reason", length = 50)
    private Reason reason;

    /* 採取的行動 */
    @Convert(converter = ActionTakenConverter.class)
    @Column(name = "action_taken", length = 50)
    private ActionTaken actionTaken;

    /* 審核人員ID */
    @Column(name = "moderator_id", nullable = false)
    private Long moderatorId;

    /* 狀態 */
    @Convert(converter = StatusConverter.class)
    @Column(name = "status", length = 50, nullable = true)
    private Status status = Status.PENDING;

    /* 建立時間 */
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    /* 審核處理時間 */
    @Column(name = "reviewed_at", insertable = false, updatable = false)
    private LocalDateTime reviewedAt;

    /* 其他細節(JSON字串可) */
    @Column(name = "metadata", columnDefinition = "nvarchar(max)")
    private String metadata;
}