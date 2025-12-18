package tw.com.ispan.eeit.ho_back.room;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;
import tw.com.ispan.eeit.ho_back.hotel.Hotel;
import tw.com.ispan.eeit.ho_back.inventory.Inventory;
import tw.com.ispan.eeit.ho_back.roomtypebedtype.RoomTypeBedType;

@Entity
@Table(name = "room")
@Data
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // 外鍵：關聯到 Hotel
    @JsonIgnoreProperties("rooms")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id", nullable = false)
    private Hotel hotel;

    @Column(name = "name", nullable = false, length = 100)
    private String name; // 房間名稱 (例如：海景101套房、CITY ROOM，房間自定義)

    // 修改：直接關聯到 room_type_bed_type 配置表
    // room_type_bed_type 是配置表（房型類型 + 床型 + 床數量的組合）
    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties("rooms")
    @JoinColumn(name = "room_type_id", nullable = false)
    private RoomTypeBedType roomTypeBedType;

    @Column(name = "quantity", nullable = false)
    private Integer quantity; // 基本庫存量

    @Column(name = "max_occupancy")
    private Integer maxOccupancy; // 最多入住人數

    @Column(name = "size", precision = 5, scale = 2)
    private BigDecimal size; // 房間大小

    @Column(name = "base_price", precision = 10, scale = 2, nullable = false)
    private BigDecimal basePrice; // 價格

    @Column(name = "description", length = 300)
    private String description;

    @Column(name = "status", nullable = false)
    private Short status = 1; // 狀態：0:停用 1:啟用

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "room")
    @JsonIgnoreProperties("room")
    private List<Inventory> inventories;

    @Override
    public String toString() {
        return "Room [id=" + id + ", name=" + name + ", quantity=" + quantity + ", maxOccupancy=" + maxOccupancy
                + ", size=" + size + ", basePrice=" + basePrice + ", description=" + description + ", status=" + status
                + ", createdAt=" + createdAt + ", updatedAt=" + updatedAt + "]";
    }

}
