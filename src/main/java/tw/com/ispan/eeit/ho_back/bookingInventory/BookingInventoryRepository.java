package tw.com.ispan.eeit.ho_back.bookingInventory;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import tw.com.ispan.eeit.ho_back.compositionKey.BookingInventoryId;

@Repository
public interface BookingInventoryRepository extends JpaRepository<BookingInventory, BookingInventoryId> {
    @Query("""
                SELECT new tw.com.ispan.eeit.ho_back.bookingInventory.BookingResponseDTO(
                    b.startDate,
                    CONCAT(u.lastName, u.firstName),
                    h.name,
                    r.name,
                    SUM(bi.number),
                    SUM(bi.partPrice),
                    CASE
                        WHEN CURRENT_DATE < b.startDate THEN '已預訂'
                        WHEN CURRENT_DATE BETWEEN b.startDate AND b.endDate THEN '入住中'
                        ELSE '完成訂單'
                    END
                )
                FROM Booking b
                JOIN b.user u
                JOIN b.bookingInventories bi
                JOIN bi.inventory inv
                JOIN inv.room r
                JOIN r.hotel h
                WHERE h.user.id = :hotelOwnerId
                GROUP BY b.id, b.startDate, b.endDate, CONCAT(u.lastName, u.firstName), h.name, r.name
                ORDER BY b.startDate DESC
            """)
    List<BookingResponseDTO> findBookingDetailsByHotelOwner(@Param("hotelOwnerId") Integer hotelOwnerId);
}