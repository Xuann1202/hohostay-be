package tw.com.ispan.eeit.ho_back.inventory;

import java.time.LocalDate;
import java.util.List;

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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;

import tw.com.ispan.eeit.ho_back.bookingInventory.BookingInventory;
import tw.com.ispan.eeit.ho_back.room.Room;

@Entity
@Table(name = "inventory", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "room_id", "date" })
})
@Data
public class Inventory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "stock")
    private Integer stock;

    @Column(name = "price")
    private Integer price;

    @Column(name = "date")
    private LocalDate date;

    @Column(name = "start_date")
    private LocalDate startDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    @JsonIgnoreProperties("inventories")
    private Room room;

    @OneToMany(mappedBy = "inventory")
    @JsonIgnoreProperties("inventory")
    private List<BookingInventory> bookingInventories;

    @PrePersist
    public void onCreate() {
        if (date == null) {
            date = LocalDate.now();
        }
        if (startDate == null) {

            startDate = LocalDate.now();
        }
    }

    @Override
    public String toString() {
        return "Inventory [id=" + id + ", stock=" + stock + ", price=" + price + ", date=" + date + ", startDate="
                + startDate + "]";
    }

}
