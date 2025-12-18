package tw.com.ispan.eeit.ho_back.bedtype;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;
import tw.com.ispan.eeit.ho_back.roomtypebedtype.RoomTypeBedType;

@Entity
@Table(name = "bed_type")
@Data
public class BedType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "name", nullable = false, length = 50, unique = true)
    private String name; // (例如：單人床、雙人床、大床)

    // 修改：添加 @JsonIgnore 以避免 JSON 序列化時出現循環引用
    @OneToMany(mappedBy = "bedType")
    @JsonIgnore
    @JsonIgnoreProperties("bedType")
    private List<RoomTypeBedType> roomBedConfigs;
}
