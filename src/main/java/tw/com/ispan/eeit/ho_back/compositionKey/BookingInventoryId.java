package tw.com.ispan.eeit.ho_back.compositionKey;

import java.io.Serializable;

import lombok.Data;

@Data
public class BookingInventoryId implements Serializable {
    private Integer bookingId;
    private Integer inventoryId;
}
