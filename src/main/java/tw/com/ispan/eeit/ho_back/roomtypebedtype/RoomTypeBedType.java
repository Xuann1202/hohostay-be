package tw.com.ispan.eeit.ho_back.roomtypebedtype;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;
import tw.com.ispan.eeit.ho_back.bedtype.BedType;
import tw.com.ispan.eeit.ho_back.room.Room;
import tw.com.ispan.eeit.ho_back.roomtype.RoomType;

@Entity
@Table(name = "room_type_bed_type")
@Data
public class RoomTypeBedType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JsonIgnoreProperties("roomBedConfigs")
    @JoinColumn(name = "room_type_id", nullable = false)
    private RoomType roomType;

    @ManyToOne
    @JsonIgnoreProperties("roomBedConfigs")
    @JoinColumn(name = "bed_type_id", nullable = false)
    private BedType bedType;

    @Column(name = "bed_number", nullable = false)
    private Integer bedNumber;

    @OneToMany(mappedBy = "roomTypeBedType")
    @JsonIgnoreProperties("roomTypeBedType")
    private List<Room> rooms;

    @Override
    public String toString() {
        return "RoomTypeBedType [id=" + id + ", bedNumber=" + bedNumber + "]";
    }

}
