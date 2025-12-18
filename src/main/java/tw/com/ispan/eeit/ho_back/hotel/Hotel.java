package tw.com.ispan.eeit.ho_back.hotel;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import tw.com.ispan.eeit.ho_back.district.District;
import tw.com.ispan.eeit.ho_back.hotelfacility.HotelFacility;
import tw.com.ispan.eeit.ho_back.hoteltype.HotelType;
import tw.com.ispan.eeit.ho_back.photo.Photo;
import tw.com.ispan.eeit.ho_back.room.Room;
import tw.com.ispan.eeit.ho_back.user.User;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "hotel")
public class Hotel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Integer id;

    // 外鍵：業主 ID（簡化為整數，保留 User 關聯以備後用）
    // 注意：userId 和 user 關聯共享同一個資料庫欄位
    // 當設置 userId 時，user 關聯會自動更新；反之亦然
    @Column(name = "user_id", nullable = false, insertable = true, updatable = true)
    private Integer userId; // 業主 ID

    @JsonIgnore
    @JsonIgnoreProperties("hotels")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, insertable = false, updatable = false)
    private User user; // 與 User 的多對一關聯

    // 外鍵：飯店類型
    @JsonIgnore
    @JsonIgnoreProperties("hotels")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_type_id", nullable = false)
    private HotelType hotelType; // 與 HotelType 的多對一關聯

    // 外鍵：行政區
    @JsonIgnore
    @JsonIgnoreProperties("hotels")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "district_id", nullable = false)
    private District district; // 與 District 的多對一關聯

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "star_rating", columnDefinition = "TINYINT")
    private Integer starRating; // 星級 (0-5) - 改為 Short 以匹配 TINYINT
    // private Integer starRating; // 星級 (0-5)

    @Column(name = "license", nullable = false, length = 50)
    private String license;

    @Column(name = "phone", length = 10)
    private String phone;

    @Column(name = "local_call", length = 10)
    private String localCall; // 市話

    @Column(name = "address", nullable = false, length = 255)
    private String address; // 地址

    @Column(name = "latitude", nullable = false, precision = 10, scale = 8)
    private BigDecimal latitude; // 緯度

    @Column(name = "longitude", nullable = false, precision = 11, scale = 8)
    private BigDecimal longitude; // 經度

    @Column(name = "description", columnDefinition = "NVARCHAR(MAX)")
    private String description; // 描述

    @Column(name = "business_status", nullable = false, columnDefinition = "BIT DEFAULT 0")
    private Boolean businessStatus = false; // 商業狀態 (0:停用 1:啟用)

    @Column(name = "check_in_time")
    private LocalTime checkInTime; // 入住時間 (TIME)

    @Column(name = "check_out_time")
    private LocalTime checkOutTime; // 退房時間 (TIME)

    @JsonIgnore
    @CreationTimestamp
    @Column(name = "created_time", nullable = false)
    private LocalDateTime createdTime; // 創建時間

    @JsonIgnore
    @UpdateTimestamp
    @Column(name = "updated_time", nullable = false)
    private LocalDateTime updatedTime; // 更新時間
    @JsonIgnore
    @JsonIgnoreProperties("wishlist")
    @ManyToMany
    @JoinTable(name = "wishlist", joinColumns = {
            @JoinColumn(name = "hotel_id", referencedColumnName = "id") }, inverseJoinColumns = {
                    @JoinColumn(name = "user_id", referencedColumnName = "id") })
    private List<User> whosWishList;

    // 關聯：照片(一對多)
    @OneToMany(mappedBy = "hotel", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private Set<Photo> photos;
    @JsonIgnore
    @OneToMany(mappedBy = "hotel")
    @JsonIgnoreProperties("hotel")
    private List<Room> rooms;

    @JsonIgnore
    @OneToMany(mappedBy = "hotel")
    @JsonIgnoreProperties("hotel")
    private List<HotelFacility> hotelFacilities;

    @Override
    public String toString() {
        return "Hotel [id=" + id + ", name=" + name + ", starRating=" + starRating + ", license=" + license + ", phone="
                + phone + ", localCall=" + localCall + ", address=" + address + ", latitude=" + latitude
                + ", longitude=" + longitude + ", description=" + description
                + ", businessStatus=" + businessStatus + ", checkInTime=" + checkInTime + ", checkOutTime="

                + checkOutTime + "]";
    }

}
