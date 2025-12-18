package tw.com.ispan.eeit.ho_back.roomtypebedtype;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomTypeBedTypeRepository extends JpaRepository<RoomTypeBedType, Integer> {
       /**
        * 根據房型 ID 和 床型 ID 查找特定的床型組合
        * 
        * @param roomTypeId 房型分類 ID
        * @param bedTypeId  床型 ID
        * @return 查找到的 RoomTypeBedType 實體 (Optional)
        */
       Optional<RoomTypeBedType> findByRoomTypeIdAndBedTypeId(Integer roomTypeId, Integer bedTypeId);

       /**
        * 根據房型 ID、床型 ID 和床的數量查找完整的配置
        * 修改：同時考慮 bed_number，確保配置的唯一性
        * 
        * @param roomTypeId 房型分類 ID
        * @param bedTypeId  床型 ID
        * @param bedNumber  床的數量
        * @return 查找到的 RoomTypeBedType 實體 (Optional)
        */
       @Query("SELECT rtbt FROM RoomTypeBedType rtbt " +
                     "WHERE rtbt.roomType.id = :roomTypeId " +
                     "AND rtbt.bedType.id = :bedTypeId " +
                     "AND rtbt.bedNumber = :bedNumber")
       Optional<RoomTypeBedType> findByRoomTypeIdAndBedTypeIdAndBedNumber(
                     @Param("roomTypeId") Integer roomTypeId,
                     @Param("bedTypeId") Integer bedTypeId,
                     @Param("bedNumber") Integer bedNumber);

       /**
        * 根據房型 ID 查詢所有配置
        * 
        * @param roomTypeId 房型 ID
        * @return 配置列表
        */
       @Query("SELECT rtbt FROM RoomTypeBedType rtbt " +
                     "LEFT JOIN FETCH rtbt.roomType " +
                     "LEFT JOIN FETCH rtbt.bedType " +
                     "WHERE rtbt.roomType.id = :roomTypeId")
       List<RoomTypeBedType> findByRoomTypeId(@Param("roomTypeId") Integer roomTypeId);

       /**
        * 根據床型 ID 查詢所有配置
        * 
        * @param bedTypeId 床型 ID
        * @return 配置列表
        */
       @Query("SELECT rtbt FROM RoomTypeBedType rtbt " +
                     "LEFT JOIN FETCH rtbt.roomType " +
                     "LEFT JOIN FETCH rtbt.bedType " +
                     "WHERE rtbt.bedType.id = :bedTypeId")
       List<RoomTypeBedType> findByBedTypeId(@Param("bedTypeId") Integer bedTypeId);

       /**
        * 查詢所有配置（預先加載關聯）
        */
       @Query("SELECT DISTINCT rtbt FROM RoomTypeBedType rtbt " +
                     "LEFT JOIN FETCH rtbt.roomType " +
                     "LEFT JOIN FETCH rtbt.bedType")
       List<RoomTypeBedType> findAllWithAssociations();
}
