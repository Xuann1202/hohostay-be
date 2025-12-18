package tw.com.ispan.eeit.ho_back.room;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import tw.com.ispan.eeit.ho_back.inventory.dto.HotelQueryDto;

@Repository
public interface RoomRepository extends JpaRepository<Room, Integer> {
        /**
         * 根據飯店 ID 查詢所有房間
         * 
         * @param hotelId 飯店 ID
         * @return 房間列表
         */
        // 修改：直接 JOIN roomTypeBedType
        @Query("SELECT DISTINCT r FROM Room r " +
                        "LEFT JOIN FETCH r.hotel " +
                        "LEFT JOIN FETCH r.roomTypeBedType rtbt " +
                        "LEFT JOIN FETCH rtbt.roomType " +
                        "LEFT JOIN FETCH rtbt.bedType " +
                        "WHERE r.hotel.id = :hotelId")
        List<Room> findByHotelId(@Param("hotelId") Integer hotelId);

        /**
         * 根據 ID 查詢房間（預先加載所有關聯）
         * 
         * @param roomId 房間 ID
         * @return 房間（如果存在）
         */
        // 修改：直接 JOIN roomTypeBedType
        @Query("SELECT DISTINCT r FROM Room r " +
                        "LEFT JOIN FETCH r.hotel " +
                        "LEFT JOIN FETCH r.roomTypeBedType rtbt " +
                        "LEFT JOIN FETCH rtbt.roomType " +
                        "LEFT JOIN FETCH rtbt.bedType " +
                        "WHERE r.id = :roomId")
        java.util.Optional<Room> findByIdWithAssociations(@Param("roomId") Integer roomId);

        @Query("SELECT new tw.com.ispan.eeit.ho_back.room.RoomDetailDto(r.id, r.name, r.maxOccupancy, r.size, r.description, SUM(i.price)) FROM Room r "
                        +
                        "JOIN r.inventories i " +
                        "WHERE r.id IN :roomIds " +
                        "AND i.date >= :#{#query.checkInDate} " +
                        "AND i.date < :#{#query.checkOutDate} " +
                        "GROUP BY r")
        public List<RoomDetailDto> findRoomInventoryByRoomIds(List<Integer> roomIds, HotelQueryDto query);

}
