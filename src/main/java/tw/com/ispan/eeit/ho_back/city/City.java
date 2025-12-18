package tw.com.ispan.eeit.ho_back.city;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.EqualsAndHashCode;
import tw.com.ispan.eeit.ho_back.district.District;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "city")
public class City {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Integer id;

    @Column(name = "name", length = 50, nullable = false, unique = true)
    private String name;

    // --- 關聯 ---
    // 一個 City 可以有多個 District (一對多關聯)
    // mappedBy 指向 District 實體中用來定義關聯欄位的名稱 (即 private City city;)
    // 修改：添加 @JsonIgnore 以避免 JSON 序列化時出現循環引用
    @OneToMany(mappedBy = "city", fetch = FetchType.LAZY)
    @JsonIgnore
    private Set<District> districts;

    @Override
    public String toString() {
        return "District [id=" + id + ", name=" + name + "]";
    }
}
