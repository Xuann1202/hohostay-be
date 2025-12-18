package tw.com.ispan.eeit.ho_back.photo;

import jakarta.persistence.*;
import lombok.Data;

import lombok.EqualsAndHashCode;
import tw.com.ispan.eeit.ho_back.hotel.Hotel;

import org.hibernate.annotations.CreationTimestamp;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;

@Entity
@Table(name = "photo")
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Photo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Integer id;

    // 外鍵：關聯到 Hotel
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id", nullable = false)
    private Hotel hotel;

    @Column(name = "url", nullable = false, length = 255)
    private String url;

    @Column(name = "is_cover")
    private Boolean isCover = false; // 是否為封面圖片

    @Column(name = "display_order")
    private Integer displayOrder = 0; // 顯示順序

    @Column(name = "uploaded_at")
    @CreationTimestamp
    private LocalDateTime uploadedAt; // 上傳時間

}
