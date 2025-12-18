package tw.com.ispan.eeit.ho_back.bookingInventory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import tw.com.ispan.eeit.ho_back.booking.Booking;
import tw.com.ispan.eeit.ho_back.compositionKey.BookingInventoryId;
import tw.com.ispan.eeit.ho_back.inventory.Inventory;

@Data
@Entity
@Table(name = "booking_inventory")
@IdClass(BookingInventoryId.class)
public class BookingInventory {

    @Id
    @Column(name = "booking_id")
    private Integer bookingId;

    @Id
    @Column(name = "inventory_id")
    private Integer inventoryId;

    @Column(name = "number", nullable = false)
    private Integer number;

    @Column(name = "part_price", nullable = false)
    private Integer partPrice;

    @JsonIgnore
    @JsonIgnoreProperties("bookingInventories")
    @ManyToOne
    @JoinColumn(name = "booking_id", insertable = false, updatable = false)
    private Booking booking;

    @JsonIgnore
    @JsonIgnoreProperties("bookingInventories")
    @JoinColumn(name = "inventory_id", insertable = false, updatable = false)
    @ManyToOne
    private Inventory inventory;

    @Override
    public String toString() {
        return "bookingInventory [bookingId=" + bookingId + ", inventoryId=" + inventoryId + ", number=" + number
                + ", partPrice=" + partPrice
                + "]";
    }

}
