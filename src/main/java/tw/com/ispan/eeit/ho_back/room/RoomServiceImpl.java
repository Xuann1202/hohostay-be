package tw.com.ispan.eeit.ho_back.room;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tw.com.ispan.eeit.ho_back.bedtype.BedType;
import tw.com.ispan.eeit.ho_back.bedtype.BedTypeRepository;
import tw.com.ispan.eeit.ho_back.hotel.Hotel;
import tw.com.ispan.eeit.ho_back.hotel.HotelRepository;
import tw.com.ispan.eeit.ho_back.inventory.InventoryRepository;
import tw.com.ispan.eeit.ho_back.inventory.dto.HotelQueryDto;
import tw.com.ispan.eeit.ho_back.photo.PhotoRepository;
import tw.com.ispan.eeit.ho_back.roomtype.RoomType;
import tw.com.ispan.eeit.ho_back.roomtype.RoomTypeRepository;
import tw.com.ispan.eeit.ho_back.roomtypebedtype.RoomTypeBedType;
import tw.com.ispan.eeit.ho_back.roomtypebedtype.RoomTypeBedTypeRepository;

/**
 * 房間服務實現
 */
@Service
public class RoomServiceImpl implements RoomService {

    @Autowired
    InventoryRepository inventoryRepository;
    @Autowired
    PhotoRepository photoRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private HotelRepository hotelRepository;

    @Autowired
    private RoomTypeBedTypeRepository roomTypeBedTypeRepository;

    @Autowired
    private RoomTypeRepository roomTypeRepository;

    @Autowired
    private BedTypeRepository bedTypeRepository;

    /**
     * Entity 轉 DTO
     * 修改：直接從 room_type_bed_type 獲取
     */
    private RoomDTO toDto(Room room) {
        if (room == null)
            return null;

        RoomDTO dto = new RoomDTO();
        dto.setId(room.getId());
        dto.setHotelId(room.getHotel().getId());
        dto.setName(room.getName());
        dto.setQuantity(room.getQuantity());
        dto.setMaxOccupancy(room.getMaxOccupancy());
        dto.setSize(room.getSize() != null ? room.getSize().doubleValue() : null);
        dto.setBasePrice(room.getBasePrice());
        dto.setDescription(room.getDescription());
        dto.setStatus(room.getStatus());

        // 修改：設置 roomTypeBedTypeId 及相關資訊
        if (room.getRoomTypeBedType() != null) {
            dto.setRoomTypeBedTypeId(room.getRoomTypeBedType().getId());
            // 同時設置 roomTypeId、bedTypeId 和 bedNumber，方便前端編輯時使用
            dto.setRoomTypeId(room.getRoomTypeBedType().getRoomType().getId());
            dto.setBedTypeId(room.getRoomTypeBedType().getBedType().getId());
            dto.setBedNumber(room.getRoomTypeBedType().getBedNumber());
        }

        return dto;
    }

    /**
     * 尋找或創建 RoomTypeBedType 配置
     * 修改：支援自動創建配置，考慮 room_type_id、bed_type_id 和 bed_number
     * 確保標準房 + 單人床x1 和標準房 + 單人床x2 是不同的配置
     */
    private RoomTypeBedType findOrCreateRoomTypeBedType(Integer roomTypeId, Integer bedTypeId, Integer bedNumber) {
        // 1. 驗證房型和床型是否存在
        RoomType roomType = roomTypeRepository.findById(roomTypeId)
                .orElseThrow(() -> new RuntimeException("RoomType not found: " + roomTypeId));
        BedType bedType = bedTypeRepository.findById(bedTypeId)
                .orElseThrow(() -> new RuntimeException("BedType not found: " + bedTypeId));

        // 2. 修改：需要考慮 bed_number 來查詢配置
        // 確保標準房 + 單人床x1 和標準房 + 單人床x2 是不同的配置
        Integer finalBedNumber = bedNumber != null ? bedNumber : 1;
        var existingConfig = roomTypeBedTypeRepository.findByRoomTypeIdAndBedTypeIdAndBedNumber(
                roomTypeId, bedTypeId, finalBedNumber);

        if (existingConfig.isPresent()) {
            // 如果已經存在匹配的配置（room_type + bed_type + bed_number），直接返回
            return existingConfig.get();
        } else {
            // 如果不存在，創建新配置
            RoomTypeBedType newConfig = new RoomTypeBedType();
            newConfig.setRoomType(roomType);
            newConfig.setBedType(bedType);
            newConfig.setBedNumber(finalBedNumber);
            return roomTypeBedTypeRepository.save(newConfig);
        }
    }

    @Override
    @Transactional
    public RoomDTO createRoom(Integer hotelId, RoomDTO dto) {
        // 1. 驗證飯店是否存在
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new RuntimeException("Hotel not found: " + hotelId));

        // 2. 修改：支援自動創建配置
        RoomTypeBedType roomTypeBedType;
        if (dto.getRoomTypeBedTypeId() != null) {
            // 如果提供了配置 ID，直接使用
            roomTypeBedType = roomTypeBedTypeRepository.findById(dto.getRoomTypeBedTypeId())
                    .orElseThrow(
                            () -> new RuntimeException("RoomTypeBedType not found: " + dto.getRoomTypeBedTypeId()));
        } else if (dto.getRoomTypeId() != null && dto.getBedTypeId() != null) {
            // 如果提供了房型 ID 和床型 ID，查詢或創建配置
            roomTypeBedType = findOrCreateRoomTypeBedType(
                    dto.getRoomTypeId(),
                    dto.getBedTypeId(),
                    dto.getBedNumber() != null ? dto.getBedNumber() : 1);
        } else {
            throw new RuntimeException("必須提供 RoomTypeBedTypeId 或 RoomTypeId + BedTypeId");
        }

        // 3. 創建 Room Entity
        Room room = new Room();
        room.setHotel(hotel);
        room.setRoomTypeBedType(roomTypeBedType);
        room.setName(dto.getName());
        room.setQuantity(dto.getQuantity() != null ? dto.getQuantity() : 1);
        room.setMaxOccupancy(dto.getMaxOccupancy());
        room.setSize(dto.getSize() != null ? BigDecimal.valueOf(dto.getSize()) : null);
        room.setBasePrice(dto.getBasePrice());
        room.setDescription(dto.getDescription());
        room.setStatus(dto.getStatus() != null ? dto.getStatus() : (short) 1);

        // 4. 保存 Room
        Room savedRoom = roomRepository.save(room);

        return toDto(savedRoom);
    }

    @Override
    @Transactional
    public RoomDTO updateRoom(Integer roomId, RoomDTO dto) {
        // 1. 尋找現有房間
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found: " + roomId));

        // 2. 更新基本資料
        if (dto.getName() != null)
            room.setName(dto.getName());
        if (dto.getQuantity() != null)
            room.setQuantity(dto.getQuantity());
        if (dto.getMaxOccupancy() != null)
            room.setMaxOccupancy(dto.getMaxOccupancy());
        if (dto.getSize() != null)
            room.setSize(BigDecimal.valueOf(dto.getSize()));
        if (dto.getBasePrice() != null)
            room.setBasePrice(dto.getBasePrice());
        if (dto.getDescription() != null)
            room.setDescription(dto.getDescription());
        if (dto.getStatus() != null)
            room.setStatus(dto.getStatus());

        // 3. 修改：更新 roomTypeBedType（如果提供）
        if (dto.getRoomTypeBedTypeId() != null) {
            // 如果提供了配置 ID，直接使用
            RoomTypeBedType roomTypeBedType = roomTypeBedTypeRepository.findById(dto.getRoomTypeBedTypeId())
                    .orElseThrow(
                            () -> new RuntimeException("RoomTypeBedType not found: " + dto.getRoomTypeBedTypeId()));
            room.setRoomTypeBedType(roomTypeBedType);
        } else if (dto.getRoomTypeId() != null && dto.getBedTypeId() != null) {
            // 如果提供了房型 ID 和床型 ID，查詢或創建配置
            RoomTypeBedType roomTypeBedType = findOrCreateRoomTypeBedType(
                    dto.getRoomTypeId(),
                    dto.getBedTypeId(),
                    dto.getBedNumber() != null ? dto.getBedNumber() : 1);
            room.setRoomTypeBedType(roomTypeBedType);
        }

        Room savedRoom = roomRepository.save(room);
        return toDto(savedRoom);
    }

    @Override
    @Transactional
    public void deleteRoom(Integer roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found: " + roomId));
        roomRepository.delete(room);
    }

    @Override
    @Transactional(readOnly = true)
    public RoomDTO getRoomById(Integer roomId) {
        Room room = roomRepository.findByIdWithAssociations(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found: " + roomId));

        // 檢查飯店是否為停業狀態，如果是則不允許公開查詢
        Hotel hotel = room.getHotel();
        if (hotel == null || hotel.getBusinessStatus() == null || !hotel.getBusinessStatus()) {
            throw new RuntimeException("Room not found: " + roomId);
        }

        return toDto(room);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomDTO> getRoomsByHotelId(Integer hotelId) {
        // 先檢查飯店是否存在且為營業狀態
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new RuntimeException("Hotel not found: " + hotelId));

        // 如果飯店停業，返回空列表（不允許公開查詢停業飯店的房間）
        if (hotel.getBusinessStatus() == null || !hotel.getBusinessStatus()) {
            return List.of();
        }

        List<Room> rooms = roomRepository.findByHotelId(hotelId);
        return rooms.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * 根據飯店 ID 查詢所有房間（後台管理用，不過濾停業狀態）
     * 用於房東查看自己的飯店房型，包括停業飯店
     */
    @Override
    @Transactional(readOnly = true)
    public List<RoomDTO> getRoomsByHotelIdForOwner(Integer hotelId) {
        // 檢查飯店是否存在（不過濾停業狀態）
        if (!hotelRepository.existsById(hotelId)) {
            throw new RuntimeException("Hotel not found: " + hotelId);
        }

        // 直接返回所有房間，不過濾停業狀態
        List<Room> rooms = roomRepository.findByHotelId(hotelId);
        return rooms.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<RoomDetailDto> findRoomInventoryByRoomIds(List<Integer> roomIds, HotelQueryDto query) {
        if (query.getCheckInDate() == null || query.getCheckOutDate() == null) {
            throw new RuntimeException("日期不能為空");
        }
        if (roomIds.isEmpty() || roomIds == null) {
            throw new RuntimeException("roomId不能為空");
        }
        for (Integer roomId : roomIds) {
            roomRepository.findById(roomId).orElseThrow(() -> new RuntimeException("無此房間"));
        }
        List<RoomDetailDto> roomDetailDtos = roomRepository.findRoomInventoryByRoomIds(roomIds, query);
        for (RoomDetailDto roomDetailDto : roomDetailDtos) {
            List<Integer> inventoryIds = inventoryRepository.findByroomIdAndDate(roomDetailDto.getRoomId(), query);
            roomDetailDto.setInventoryIds(inventoryIds);

        }
        return roomDetailDtos;
    }
}
