package tw.com.ispan.eeit.ho_back.hotelfacility;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import tw.com.ispan.eeit.ho_back.facility.Facility;
import tw.com.ispan.eeit.ho_back.hotel.Hotel;

@Entity
@Table(name = "hotel_facility", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "hotel_id", "facility_id" })
})
@Data
public class HotelFacility {

    // 複合主鍵替代方案：使用單一 ID
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JsonIgnoreProperties("hotelFacilities")
    @JoinColumn(name = "hotel_id", nullable = false)
    private Hotel hotel;

    @ManyToOne
    @JsonIgnoreProperties("hotelFacilities")
    @JoinColumn(name = "facility_id", nullable = false)
    private Facility facility;

    @Override
    public String toString() {
        return "HotelFacility [id=" + id + "]";
    }
    // 如果需要使用 (hotel_id, facility_id) 作為複合主鍵，需要使用 @IdClass 或 @EmbeddedId
    // 實現
    // 但使用單一 ID 是 Spring Boot 常用且較簡單的模式
}