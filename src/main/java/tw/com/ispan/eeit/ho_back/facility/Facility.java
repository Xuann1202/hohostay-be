package tw.com.ispan.eeit.ho_back.facility;

import jakarta.persistence.*;
import lombok.Data;
import tw.com.ispan.eeit.ho_back.hotelfacility.HotelFacility;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "facility")

@Data
public class Facility {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "name", nullable = false, length = 50)

    private String name; // 設施名稱 (如：游泳池、健身房)

    // 關聯：一個設施可以被多個飯店使用
    // 修改：添加 @JsonIgnore 避免 JSON 序列化時的循環引用問題
    @OneToMany(mappedBy = "facility")
    @JsonIgnore
    @JsonIgnoreProperties("facility")
    private Set<HotelFacility> hotelFacilities;

    @Override
    public String toString() {
        return "Facility [id=" + id + ", name=" + name + "]";
    }
}