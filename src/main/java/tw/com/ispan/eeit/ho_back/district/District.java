package tw.com.ispan.eeit.ho_back.district;

//import lombok.NoArgsConstructor; // 建議保留：JPA 規範需要無參建構函數
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import lombok.EqualsAndHashCode;
import tw.com.ispan.eeit.ho_back.city.City;
import tw.com.ispan.eeit.ho_back.hotel.Hotel;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "district", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "city_id", "name" }) // 確保同一城市下不能有重複的區域名稱
})
public class District { // 關鍵修正：移除了 <User>

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Integer id;

    // DB: city_id INT NOT NULL (FK)
    // 關聯到 City 實體 (對應 DB 的 FK_district_city)
    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties("districts")
    @JoinColumn(name = "city_id", nullable = false)
    private City city; // 必須確保 com.hsuan.HotelC.city.City 存在

    // DB: name NVARCHAR(50) NOT NULL
    // 注意：移除了 unique =
    // true，因為不同城市可以有相同的區名（例如「大安區」在台北市和台中市都存在）
    // 但通過 @Table 的 uniqueConstraints 確保同一城市下不能有重複的區域名稱
    @Column(name = "name", length = 50, nullable = false)
    private String name;

    // DB: 對 hotel 表的關聯 (FK_hotel_district)
    // District 是'一' 對 Hotel 是'多'
    // mappedBy 指向 Hotel 實體中的 private District district 欄位
    // 修改：添加 @JsonIgnore 以避免 JSON 序列化時出現循環引用
    @OneToMany(mappedBy = "district")
    @JsonIgnore
    private Set<Hotel> hotels;

    @Override
    public String toString() {
        return "District [id=" + id + ", name=" + name + "]";
    }

}
