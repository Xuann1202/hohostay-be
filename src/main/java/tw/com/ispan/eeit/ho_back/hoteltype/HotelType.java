package tw.com.ispan.eeit.ho_back.hoteltype;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import tw.com.ispan.eeit.ho_back.hotel.Hotel;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@Entity
@Table(name = "hotel_type")
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class HotelType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Integer id;

    @Column(name = "type", nullable = false, length = 50, unique = true)
    private String type;

    @JsonIgnore
    @OneToMany(mappedBy = "hotelType")
    @JsonIgnoreProperties("hotelType")
    private List<Hotel> hotels;

    @Override
    public String toString() {
        return "HotelType [id=" + id + ", type=" + type + "]";
    }

}
