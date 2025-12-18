package tw.com.ispan.eeit.ho_back.roomtypebedtype;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tw.com.ispan.eeit.ho_back.bedtype.BedType;
import tw.com.ispan.eeit.ho_back.bedtype.BedTypeRepository;
import tw.com.ispan.eeit.ho_back.common.exception.RoomTypeBedTypeNotFoundException;
import tw.com.ispan.eeit.ho_back.roomtype.RoomType;
import tw.com.ispan.eeit.ho_back.roomtype.RoomTypeRepository;

/**
 * 房型床型配置服務實現
 */
@Service
public class RoomTypeBedTypeServiceImpl implements RoomTypeBedTypeService {

    @Autowired
    private RoomTypeBedTypeRepository roomTypeBedTypeRepository;

    @Autowired
    private RoomTypeRepository roomTypeRepository;

    @Autowired
    private BedTypeRepository bedTypeRepository;

    /**
     * Entity 轉 DTO
     */
    private RoomTypeBedTypeDTO toDto(RoomTypeBedType entity) {
        if (entity == null)
            return null;

        RoomTypeBedTypeDTO dto = new RoomTypeBedTypeDTO();
        dto.setId(entity.getId());
        dto.setRoomTypeId(entity.getRoomType().getId());
        dto.setBedTypeId(entity.getBedType().getId());
        dto.setBedNumber(entity.getBedNumber());

        return dto;
    }

    @Override
    @Transactional
    public RoomTypeBedTypeDTO createRoomTypeBedType(RoomTypeBedTypeDTO dto) {
        // 1. 驗證房型和床型是否存在
        RoomType roomType = roomTypeRepository.findById(dto.getRoomTypeId())
                .orElseThrow(() -> new RuntimeException("RoomType not found: " + dto.getRoomTypeId()));

        BedType bedType = bedTypeRepository.findById(dto.getBedTypeId())
                .orElseThrow(() -> new RuntimeException("BedType not found: " + dto.getBedTypeId()));

        // 2. 檢查是否已有相同配置
        roomTypeBedTypeRepository.findByRoomTypeIdAndBedTypeId(
                dto.getRoomTypeId(), dto.getBedTypeId())
                .ifPresent(existing -> {
                    throw new RuntimeException(
                            "配置已存在：房型 ID " + dto.getRoomTypeId() +
                                    " + 床型 ID " + dto.getBedTypeId());
                });

        // 3. 創建新配置
        RoomTypeBedType entity = new RoomTypeBedType();
        entity.setRoomType(roomType);
        entity.setBedType(bedType);
        entity.setBedNumber(dto.getBedNumber());

        RoomTypeBedType saved = roomTypeBedTypeRepository.save(entity);
        return toDto(saved);
    }

    @Override
    @Transactional
    public RoomTypeBedTypeDTO updateRoomTypeBedType(Integer id, RoomTypeBedTypeDTO dto) {
        // 1. 查找現有配置
        RoomTypeBedType entity = roomTypeBedTypeRepository.findById(id)
                .orElseThrow(() -> new RoomTypeBedTypeNotFoundException(id));

        // 2. 更新房型（如果提供）
        if (dto.getRoomTypeId() != null) {
            RoomType roomType = roomTypeRepository.findById(dto.getRoomTypeId())
                    .orElseThrow(() -> new RuntimeException("RoomType not found: " + dto.getRoomTypeId()));
            entity.setRoomType(roomType);
        }

        // 3. 更新床型（如果提供）
        if (dto.getBedTypeId() != null) {
            BedType bedType = bedTypeRepository.findById(dto.getBedTypeId())
                    .orElseThrow(() -> new RuntimeException("BedType not found: " + dto.getBedTypeId()));
            entity.setBedType(bedType);
        }

        // 4. 更新床的數量（如果提供）
        if (dto.getBedNumber() != null) {
            entity.setBedNumber(dto.getBedNumber());
        }

        RoomTypeBedType saved = roomTypeBedTypeRepository.save(entity);
        return toDto(saved);
    }

    @Override
    @Transactional
    public void deleteRoomTypeBedType(Integer id) {
        RoomTypeBedType entity = roomTypeBedTypeRepository.findById(id)
                .orElseThrow(() -> new RoomTypeBedTypeNotFoundException(id));

        roomTypeBedTypeRepository.delete(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public RoomTypeBedTypeDTO getRoomTypeBedTypeById(Integer id) {
        RoomTypeBedType entity = roomTypeBedTypeRepository.findById(id)
                .orElseThrow(() -> new RoomTypeBedTypeNotFoundException(id));

        return toDto(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomTypeBedTypeDTO> getAllRoomTypeBedTypes() {
        return roomTypeBedTypeRepository.findAllWithAssociations().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomTypeBedTypeDTO> getRoomTypeBedTypesByRoomTypeId(Integer roomTypeId) {
        return roomTypeBedTypeRepository.findByRoomTypeId(roomTypeId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomTypeBedTypeDTO> getRoomTypeBedTypesByBedTypeId(Integer bedTypeId) {
        return roomTypeBedTypeRepository.findByBedTypeId(bedTypeId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
}
