package tw.com.ispan.eeit.ho_back.review;

import java.time.LocalDate;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import tw.com.ispan.eeit.ho_back.booking.Booking;
import tw.com.ispan.eeit.ho_back.user.User;

@Data
@Entity
@Table(name = "review", uniqueConstraints = { @UniqueConstraint(columnNames = { "user_id", "booking_id" }) })
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // 使用者關聯
    @JsonIgnoreProperties("reviews")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 訂單關聯
    // @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "booking_id", nullable = false)
    // private Booking booking;

    @JsonIgnoreProperties({ "bookingInventories", "user", "review" })
    @OneToOne
    @JoinColumn(name = "booking_id")
    private Booking booking;

    @Column(name = "rating", nullable = false)
    private Integer rating;

    @Column(name = "comment", length = 1000)
    private String comment;

    @Column(name = "is_edited", nullable = false)
    private Boolean isEdited = false;

    @Column(name = "is_visible", nullable = false)
    private Boolean isVisible = true;

    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDate createdDate;

    @Column(name = "updated_date", nullable = false)
    private LocalDate updatedDate;

    // 飯店業者回復
    @Column(name = "reply", length = 1000)
    private String reply;

    @Column(name = "reply_created_date")
    private LocalDate replyCreatedDate;

    @Column(name = "reply_updated_date")
    private LocalDate replyUpdatedDate;

    @Override
    public String toString() {
        return "Review [id=" + id + ", rating=" + rating + ", comment=" + comment + ", isEdited=" + isEdited
                + ", isVisible=" + isVisible + ", createdDate=" + createdDate + ", updatedDate=" + updatedDate
                + ", reply=" + reply + ", replyCreatedDate=" + replyCreatedDate + ", replyUpdatedDate="
                + replyUpdatedDate + "]";
    }
}