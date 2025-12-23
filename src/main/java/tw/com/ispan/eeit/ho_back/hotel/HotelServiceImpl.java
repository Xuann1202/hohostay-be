package tw.com.ispan.eeit.ho_back.hotel;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tw.com.ispan.eeit.ho_back.city.CityDTO;
import tw.com.ispan.eeit.ho_back.common.exception.HotelNotFoundException;
import tw.com.ispan.eeit.ho_back.district.DistrictDTO;
import tw.com.ispan.eeit.ho_back.district.DistrictRepository;
import tw.com.ispan.eeit.ho_back.facility.Facility;
import tw.com.ispan.eeit.ho_back.facility.FacilityRepository;
import tw.com.ispan.eeit.ho_back.hotelfacility.HotelFacility;
import tw.com.ispan.eeit.ho_back.hotelfacility.HotelFacilityRepository;
import tw.com.ispan.eeit.ho_back.hoteltype.HotelTypeDTO;
import tw.com.ispan.eeit.ho_back.hoteltype.HotelTypeRepository;
import tw.com.ispan.eeit.ho_back.photo.Photo;
import tw.com.ispan.eeit.ho_back.photo.PhotoDTO;
import tw.com.ispan.eeit.ho_back.photo.PhotoRepository;

import tw.com.ispan.eeit.ho_back.room.Room;
import tw.com.ispan.eeit.ho_back.room.RoomIdsDTO;
import tw.com.ispan.eeit.ho_back.room.RoomRepository;

@Service
public class HotelServiceImpl implements HotelService {

    // 依賴注入

    @Autowired
    private HotelRepository hotelRepository;
    @Autowired
    private HotelTypeRepository hotelTypeRepository;
    @Autowired
    private DistrictRepository districtRepository;
    @Autowired
    private PhotoRepository photoRepository;
    @Autowired
    private FacilityRepository facilityRepository;
    @Autowired
    private HotelFacilityRepository hotelFacilityRepository;
    @Autowired
    private RoomRepository roomRepository;

    private static final LocalTime DEFAULT_CHECK_IN_TIME = LocalTime.of(15, 0);
    private static final LocalTime DEFAULT_CHECK_OUT_TIME = LocalTime.of(11, 0);

    private LocalTime resolveCheckInTime(LocalTime time) {
        return time != null ? time : DEFAULT_CHECK_IN_TIME;
    }

    private LocalTime resolveCheckOutTime(LocalTime time) {
        return time != null ? time : DEFAULT_CHECK_OUT_TIME;
    }

    @Override
    @Transactional(readOnly = true)
    public List<HotelRoomIdsDTO> getHotelAndRoomIdsByOwner(Integer userId) {
        System.out.println("getHotelAndRoomIdsByOwner userId=" + userId);
        List<Hotel> hotels = hotelRepository.findByUserId(userId);
        if (hotels == null || hotels.isEmpty()) {
            return new ArrayList<>();
        }

        return hotels.stream().map(hotel -> {
            List<Room> rooms = roomRepository.findByHotelId(hotel.getId());
            List<RoomIdsDTO> roomDtos = rooms.stream()
                    .map(r -> new RoomIdsDTO(r.getId(), r.getName()))
                    .toList();

            HotelRoomIdsDTO dto = new HotelRoomIdsDTO();
            dto.setHotelId(hotel.getId());
            dto.setHotelName(hotel.getName());
            dto.setRooms(roomDtos);

            return dto;
        }).toList();
    }

    // =========================================================================
    // I. 輔助方法：將 Entity -> DTO 轉換
    // =========================================================================
    @Override
    @Transactional(readOnly = true)
    public List<Integer> getHotelIdsByOwner(Integer userId) {
        // 查詢該房東的所有飯店 ID
        List<Hotel> hotels = hotelRepository.findByUserId(userId);
        if (hotels == null || hotels.isEmpty()) {
            return new ArrayList<>();
        }
        return hotels.stream()
                .map(Hotel::getId)
                .collect(Collectors.toList());
    }

    /**
     * 批量載入的照片和設施資料（優化 N+1 問題）
     */
    private static class BatchData {
        final Map<Integer, List<PhotoDTO>> photosByHotelId;
        final Map<Integer, List<Integer>> facilityIdsByHotelId;

        BatchData(Map<Integer, List<PhotoDTO>> photosByHotelId, Map<Integer, List<Integer>> facilityIdsByHotelId) {
            this.photosByHotelId = photosByHotelId;
            this.facilityIdsByHotelId = facilityIdsByHotelId;
        }
    }

    /**
     * 批量載入照片和設施資料（優化 N+1 問題）
     * 
     * @param hotelIds 飯店 ID 列表
     * @return 包含照片和設施 Map 的 BatchData 對象
     */
    private BatchData loadPhotosAndFacilitiesBatch(List<Integer> hotelIds) {
        // 批量查詢所有照片（1 次查詢）
        List<Photo> allPhotos = photoRepository.findByHotelIdIn(hotelIds);
        
        // 批量查詢所有設施（1 次查詢）
        List<HotelFacility> allFacilities = hotelFacilityRepository.findByHotelIdIn(hotelIds);
        
        // 將照片按 hotelId 分組並轉換為 DTO
        Map<Integer, List<PhotoDTO>> photosByHotelId = allPhotos.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getHotel().getId(),
                        Collectors.mapping(p -> {
                            PhotoDTO photoDto = new PhotoDTO();
                            photoDto.setId(p.getId());
                            photoDto.setUrl(p.getUrl());
                            photoDto.setIsCover(p.getIsCover());
                            photoDto.setDisplayOrder(p.getDisplayOrder());
                            return photoDto;
                        }, Collectors.toList())
                ));
        
        // 將設施按 hotelId 分組並提取 facilityId
        Map<Integer, List<Integer>> facilityIdsByHotelId = allFacilities.stream()
                .collect(Collectors.groupingBy(
                        hf -> hf.getHotel().getId(),
                        Collectors.mapping(hf -> hf.getFacility().getId(), Collectors.toList())
                ));
        
        return new BatchData(photosByHotelId, facilityIdsByHotelId);
    }

    /**
     * 將 Hotel Entity 轉換為 DTO（使用預載入的 Map，優化 N+1 問題）
     * 
     * @param h 飯店 Entity
     * @param photosByHotelId 按 hotelId 分組的照片 Map
     * @param facilityIdsByHotelId 按 hotelId 分組的設施 ID Map
     * @return HotelDTO
     */
    private HotelDTO toDto(Hotel h, Map<Integer, List<PhotoDTO>> photosByHotelId,
            Map<Integer, List<Integer>> facilityIdsByHotelId) {
        if (h == null)
            return null;
        HotelDTO dto = new HotelDTO();

        dto.setId(h.getId());
        dto.setName(h.getName());
        dto.setLicense(h.getLicense());
        dto.setPhone(h.getPhone());
        dto.setLocalCall(h.getLocalCall());
        dto.setDescription(h.getDescription());

        dto.setStarRating(h.getStarRating() == null ? null : h.getStarRating());

        dto.setCheckInTime(h.getCheckInTime());
        dto.setCheckOutTime(h.getCheckOutTime());
        dto.setAddress(h.getAddress());

        dto.setLatitude(h.getLatitude() == null ? null : h.getLatitude().doubleValue());
        dto.setLongitude(h.getLongitude() == null ? null : h.getLongitude().doubleValue());

        dto.setBusinessStatus(h.getBusinessStatus());

        // 設置外鍵 ID
        if (h.getDistrict() != null) {
            dto.setDistrictId(h.getDistrict().getId());

            // 設置完整的 District 對象（包含 City 信息）
            DistrictDTO districtDto = new DistrictDTO();
            districtDto.setId(h.getDistrict().getId());
            districtDto.setName(h.getDistrict().getName());

            // 設置 City 信息
            if (h.getDistrict().getCity() != null) {
                CityDTO cityDto = new CityDTO();
                cityDto.setId(h.getDistrict().getCity().getId());
                cityDto.setName(h.getDistrict().getCity().getName());
                districtDto.setCity(cityDto);
            }

            dto.setDistrict(districtDto);
        }

        if (h.getHotelType() != null) {
            dto.setHotelTypeId(h.getHotelType().getId());

            // 設置完整的 HotelType 對象
            HotelTypeDTO hotelTypeDto = new HotelTypeDTO();
            hotelTypeDto.setId(h.getHotelType().getId());
            hotelTypeDto.setType(h.getHotelType().getType());
            dto.setHotelType(hotelTypeDto);
        }

        // 從 Map 中獲取照片（不查詢資料庫）
        List<PhotoDTO> photoList = photosByHotelId.getOrDefault(h.getId(), new ArrayList<>());
        dto.setPhotos(photoList);

        // 從 Map 中獲取設施 ID 列表（不查詢資料庫）
        List<Integer> facilityIds = facilityIdsByHotelId.getOrDefault(h.getId(), new ArrayList<>());
        dto.setFacilityIds(facilityIds);

        return dto;
    }

    /**
     * 將 Hotel Entity 轉換為 DTO（單一飯店查詢時使用，會單獨查詢照片和設施）
     * 
     * @param h 飯店 Entity
     * @return HotelDTO
     */
    private HotelDTO toDto(Hotel h) {
        if (h == null)
            return null;
        HotelDTO dto = new HotelDTO();

        dto.setId(h.getId());
        dto.setName(h.getName());
        dto.setLicense(h.getLicense());
        dto.setPhone(h.getPhone());
        dto.setLocalCall(h.getLocalCall());
        dto.setDescription(h.getDescription());

        dto.setStarRating(h.getStarRating() == null ? null : h.getStarRating());

        dto.setCheckInTime(h.getCheckInTime());
        dto.setCheckOutTime(h.getCheckOutTime());
        dto.setAddress(h.getAddress());

        dto.setLatitude(h.getLatitude() == null ? null : h.getLatitude().doubleValue());
        dto.setLongitude(h.getLongitude() == null ? null : h.getLongitude().doubleValue());

        dto.setBusinessStatus(h.getBusinessStatus());

        // 設置外鍵 ID
        if (h.getDistrict() != null) {
            dto.setDistrictId(h.getDistrict().getId());

            // 設置完整的 District 對象（包含 City 信息）
            DistrictDTO districtDto = new DistrictDTO();
            districtDto.setId(h.getDistrict().getId());
            districtDto.setName(h.getDistrict().getName());

            // 設置 City 信息
            if (h.getDistrict().getCity() != null) {
                CityDTO cityDto = new CityDTO();
                cityDto.setId(h.getDistrict().getCity().getId());
                cityDto.setName(h.getDistrict().getCity().getName());
                districtDto.setCity(cityDto);
            }

            dto.setDistrict(districtDto);
        }

        if (h.getHotelType() != null) {
            dto.setHotelTypeId(h.getHotelType().getId());

            // 設置完整的 HotelType 對象
            HotelTypeDTO hotelTypeDto = new HotelTypeDTO();
            hotelTypeDto.setId(h.getHotelType().getId());
            hotelTypeDto.setType(h.getHotelType().getType());
            dto.setHotelType(hotelTypeDto);
        }

        // 查詢照片 - 直接使用 Repository 查詢，避免 LAZY 載入問題
        try {
            List<Photo> photoList = photoRepository.findByHotelId(h.getId());
            if (photoList != null && !photoList.isEmpty()) {
                List<PhotoDTO> photoDtos = new ArrayList<>();
                for (Photo p : photoList) {
                    if (p != null) {
                        PhotoDTO photoDto = new PhotoDTO();
                        photoDto.setId(p.getId());
                        photoDto.setUrl(p.getUrl());
                        photoDto.setIsCover(p.getIsCover());
                        photoDto.setDisplayOrder(p.getDisplayOrder());
                        photoDtos.add(photoDto);
                    }
                }
                dto.setPhotos(photoDtos);
            } else {
                dto.setPhotos(new ArrayList<>());
            }
        } catch (Exception e) {
            System.err.println("查詢飯店 " + h.getId() + " 的照片失敗: " + e.getMessage());
            dto.setPhotos(new ArrayList<>());
        }

        // 查詢設施 ID 列表 - 使用 Repository 查詢，避免 LAZY 載入問題
        try {
            List<Integer> facilityIds = hotelFacilityRepository.findFacilityIdsByHotelId(h.getId());
            dto.setFacilityIds(facilityIds != null ? facilityIds : new ArrayList<>());
        } catch (Exception e) {
            System.err.println("查詢飯店 " + h.getId() + " 的設施列表失敗: " + e.getMessage());
            dto.setFacilityIds(new ArrayList<>());
        }

        return dto;
    }

    // =========================================================================
    // II. 飯店 CRUD 實現
    // =========================================================================

    @Override
    @Transactional
    public HotelDTO createHotel(Integer userId, HotelDTO dto) {
        // 1. 驗證用戶 ID
        if (userId == null) {
            throw new SecurityException("需要身份驗證。用戶 ID 不能為空。");
        }

        // 2. 尋找 FK 依賴
        var hotelType = hotelTypeRepository.findById(dto.getHotelTypeId())
                .orElseThrow(() -> new RuntimeException("找不到飯店類型: " + dto.getHotelTypeId()));
        var district = districtRepository.findById(dto.getDistrictId())
                .orElseThrow(() -> new RuntimeException("找不到行政區: " + dto.getDistrictId()));

        // 3. DTO -> Entity 轉換並設定預設值
        Hotel hotel = new Hotel();
        hotel.setUserId(userId);
        hotel.setHotelType(hotelType);
        hotel.setDistrict(district);

        hotel.setName(dto.getName());
        hotel.setLicense(dto.getLicense());
        hotel.setPhone(dto.getPhone());
        hotel.setLocalCall(dto.getLocalCall());
        hotel.setAddress(dto.getAddress());
        hotel.setDescription(dto.getDescription());
        hotel.setStarRating(dto.getStarRating() == null ? null : dto.getStarRating());
        hotel.setCheckInTime(resolveCheckInTime(dto.getCheckInTime()));
        hotel.setCheckOutTime(resolveCheckOutTime(dto.getCheckOutTime()));

        // 修改：處理 businessStatus（預設為 false，需要房東主動開啟營業狀態）
        hotel.setBusinessStatus(dto.getBusinessStatus() != null ? dto.getBusinessStatus() : false);

        // 處理經緯度（前端必須提供）
        if (dto.getLatitude() != null && dto.getLongitude() != null) {
            hotel.setLatitude(BigDecimal.valueOf(dto.getLatitude()));
            hotel.setLongitude(BigDecimal.valueOf(dto.getLongitude()));
        } else {
            // 前端必須提供經緯度，不再使用後端 geocoding
            throw new RuntimeException("經緯度為必填項目。請使用前端的位置選擇器選擇地址，系統會自動取得座標。");
        }

        Hotel savedHotel = hotelRepository.save(hotel);

        // 處理照片（如果提供）
        if (dto.getPhotos() != null && !dto.getPhotos().isEmpty()) {
            Set<Photo> photos = new HashSet<>();
            int order = 0;
            for (PhotoDTO p : dto.getPhotos()) {
                Photo photo = new Photo();
                photo.setUrl(p.getUrl());
                photo.setIsCover(p.getIsCover() != null ? p.getIsCover() : false);
                photo.setDisplayOrder(p.getDisplayOrder() != null ? p.getDisplayOrder() : order++);
                photo.setHotel(savedHotel);
                photos.add(photo);
            }
            // 修改：使用 photoRepository 保存照片，確保正確持久化
            photoRepository.saveAll(photos);
        }

        // 處理設施關聯
        if (dto.getFacilityIds() != null && !dto.getFacilityIds().isEmpty()) {
            List<HotelFacility> hotelFacilities = new ArrayList<>();
            for (Integer facilityId : dto.getFacilityIds()) {
                Facility facility = facilityRepository.findById(facilityId)
                        .orElseThrow(() -> new RuntimeException("找不到設施: " + facilityId));

                HotelFacility hf = new HotelFacility();
                hf.setHotel(savedHotel);
                hf.setFacility(facility);
                hotelFacilities.add(hf);
            }
            hotelFacilityRepository.saveAll(hotelFacilities);
        }

        // 5. 返回 DTO
        return toDto(savedHotel);
    }

    @Override
    @Transactional
    public HotelDTO updateHotel(Integer userId, Integer hotelId, HotelDTO dto) {
        // 1. 驗證用戶 ID
        if (userId == null) {
            throw new SecurityException("需要身份驗證。用戶 ID 不能為空。");
        }

        // 2. 尋找飯店
        Hotel exist = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new HotelNotFoundException(hotelId));

        // 3. 驗證所有權限，確保用戶只能修改自己的飯店
        if (exist.getUserId() == null || !exist.getUserId().equals(userId)) {
            throw new SecurityException("未授權：您只能修改自己的飯店。");
        }

        // 2. 部分更新邏輯：部分更新 (if (null) 檢查)
        if (dto.getName() != null)
            exist.setName(dto.getName());
        if (dto.getLicense() != null)
            exist.setLicense(dto.getLicense());
        if (dto.getPhone() != null)
            exist.setPhone(dto.getPhone());
        if (dto.getLocalCall() != null)
            exist.setLocalCall(dto.getLocalCall());
        if (dto.getAddress() != null)
            exist.setAddress(dto.getAddress());
        if (dto.getDescription() != null)
            exist.setDescription(dto.getDescription());

        if (dto.getStarRating() != null)
            exist.setStarRating(dto.getStarRating());

        // 驗證營業狀態：只有在必填資料完成時才能開啟營業狀態
        if (dto.getBusinessStatus() != null && dto.getBusinessStatus()) {
            // 檢查必填資料是否完成
            boolean hasRequiredFields = exist.getName() != null && !exist.getName().trim().isEmpty() &&
                    exist.getLicense() != null && !exist.getLicense().trim().isEmpty() &&
                    exist.getDistrict() != null &&
                    exist.getAddress() != null && !exist.getAddress().trim().isEmpty() &&
                    exist.getLatitude() != null && exist.getLongitude() != null &&
                    (exist.getPhone() != null && !exist.getPhone().trim().isEmpty() ||
                            exist.getLocalCall() != null && !exist.getLocalCall().trim().isEmpty());

            if (!hasRequiredFields) {
                throw new IllegalStateException("無法開啟營業狀態：請先完成所有必填項目（飯店名稱、執照號碼、地址、座標、聯絡電話）");
            }
        }

        if (dto.getBusinessStatus() != null)
            exist.setBusinessStatus(dto.getBusinessStatus());
        if (dto.getCheckInTime() != null)
            exist.setCheckInTime(resolveCheckInTime(dto.getCheckInTime()));
        if (dto.getCheckOutTime() != null)
            exist.setCheckOutTime(resolveCheckOutTime(dto.getCheckOutTime()));

        // 更新外鍵
        if (dto.getHotelTypeId() != null) {
            var hotelType = hotelTypeRepository.findById(dto.getHotelTypeId())
                    .orElseThrow(() -> new RuntimeException("找不到飯店類型: " + dto.getHotelTypeId()));
            exist.setHotelType(hotelType);
        }
        if (dto.getDistrictId() != null) {
            var district = districtRepository.findById(dto.getDistrictId())
                    .orElseThrow(() -> new RuntimeException("找不到行政區: " + dto.getDistrictId()));
            exist.setDistrict(district);
        }

        // 更新經緯度
        // 處理經緯度（前端必須提供，或保留原值）
        if (dto.getLatitude() != null && dto.getLongitude() != null) {
            exist.setLatitude(BigDecimal.valueOf(dto.getLatitude()));
            exist.setLongitude(BigDecimal.valueOf(dto.getLongitude()));
        }

        // 更新設施關聯 (完全替換邏輯)
        if (dto.getFacilityIds() != null) {
            hotelFacilityRepository.deleteByHotelId(hotelId); // 刪除舊關聯
            if (!dto.getFacilityIds().isEmpty()) {
                List<HotelFacility> newFacilities = new ArrayList<>();
                for (Integer facilityId : dto.getFacilityIds()) {
                    Facility facility = facilityRepository.findById(facilityId)
                            .orElseThrow(() -> new RuntimeException("找不到設施: " + facilityId));

                    HotelFacility hf = new HotelFacility();
                    hf.setHotel(exist);
                    hf.setFacility(facility);
                    newFacilities.add(hf);
                }
                hotelFacilityRepository.saveAll(newFacilities);
            }
        }

        // 修改：更新照片（完全替換邏輯）
        // 注意：如果前端沒有發送 photos 欄位（為 null），則不更新照片
        // 如果前端發送了空陣列，則刪除所有照片
        if (dto.getPhotos() != null) {
            // 刪除舊照片：先查詢所有照片，然後刪除
            List<Photo> existingPhotos = photoRepository.findByHotelId(hotelId);
            if (existingPhotos != null && !existingPhotos.isEmpty()) {
                photoRepository.deleteAll(existingPhotos);
            }

            // 如果有新照片，創建並保存
            if (!dto.getPhotos().isEmpty()) {
                List<Photo> newPhotos = new ArrayList<>();
                int order = 0;
                for (PhotoDTO p : dto.getPhotos()) {
                    Photo photo = new Photo();
                    photo.setUrl(p.getUrl());
                    photo.setIsCover(p.getIsCover() != null ? p.getIsCover() : false);
                    photo.setDisplayOrder(p.getDisplayOrder() != null ? p.getDisplayOrder() : order++);
                    photo.setHotel(exist);
                    newPhotos.add(photo);
                }
                // 修改：先保存照片，確保它們有 ID
                photoRepository.saveAll(newPhotos);
            }
        }

        // 3. 保存 Entity
        Hotel savedHotel = hotelRepository.save(exist);

        // 4. 重新載入飯店以確保所有關聯都正確載入（特別是 district 和 city）
        Hotel reloadedHotel = hotelRepository.findByIdWithAssociations(savedHotel.getId())
                .orElseThrow(() -> new HotelNotFoundException(savedHotel.getId()));

        // 5. 返回 DTO
        return toDto(reloadedHotel);
    }

    @Override
    @Transactional
    public void deleteHotel(Integer userId, Integer hotelId) {
        // 1. 驗證用戶 ID
        if (userId == null) {
            throw new SecurityException("需要身份驗證。用戶 ID 不能為空。");
        }

        // 2. 尋找飯店
        Hotel exist = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new HotelNotFoundException(hotelId));

        // 3. 驗證所有權限，確保用戶只能刪除自己的飯店
        if (exist.getUserId() == null || !exist.getUserId().equals(userId)) {
            throw new SecurityException("未授權：您只能刪除自己的飯店。");
        }

        // 邏輯刪除：設定 businessStatus = 0
        exist.setBusinessStatus(false); // 設為停業/停用
        // 更新時間戳
        exist.setUpdatedTime(java.time.LocalDateTime.now());

        hotelRepository.save(exist); // 保存變更
    }

    // =========================================================================
    // III. 查詢實現 (後台 & 前台)
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public List<HotelDTO> getHotelsByOwner(Integer userId) {
        // 使用 JOIN FETCH 載入所有基本關聯資料
        List<Hotel> hotels = hotelRepository.findByUserId(userId);

        if (hotels.isEmpty()) {
            return new ArrayList<>();
        }

        // 批量載入照片和設施（優化 N+1 問題）
        List<Integer> hotelIds = hotels.stream()
                .map(Hotel::getId)
                .collect(Collectors.toList());
        BatchData batchData = loadPhotosAndFacilitiesBatch(hotelIds);

        return hotels.stream()
                .map(h -> toDto(h, batchData.photosByHotelId, batchData.facilityIdsByHotelId))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getHotelsByOwner(Integer userId, int page, int size) {
        // 使用 JOIN FETCH 載入所有基本關聯資料
        List<Hotel> allHotels = hotelRepository.findByUserId(userId);

        if (allHotels.isEmpty()) {
            Map<String, Object> result = new HashMap<>();
            result.put("content", new ArrayList<>());
            result.put("totalElements", 0);
            result.put("totalPages", 0);
            result.put("currentPage", page);
            result.put("size", size);
            return result;
        }

        // 批量載入照片和設施（優化 N+1 問題）
        List<Integer> hotelIds = allHotels.stream()
                .map(Hotel::getId)
                .collect(Collectors.toList());
        BatchData batchData = loadPhotosAndFacilitiesBatch(hotelIds);

        // 手動分頁處理
        int totalElements = allHotels.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        int start = (page - 1) * size;

        List<HotelDTO> hotelDTOs = allHotels.stream()
                .skip(start)
                .limit(size)
                .map(h -> toDto(h, batchData.photosByHotelId, batchData.facilityIdsByHotelId))
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("content", hotelDTOs);
        result.put("totalElements", totalElements);
        result.put("totalPages", totalPages);
        result.put("currentPage", page);
        result.put("size", size);

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getHotelsByOwner(Integer userId, int page, int size, Integer cityId,
            Integer districtId) {
        // 使用地區篩選查詢
        List<Hotel> allHotels;
        if (cityId != null || districtId != null) {
            allHotels = hotelRepository.findByUserIdAndLocation(userId, cityId, districtId);
        } else {
            allHotels = hotelRepository.findByUserId(userId);
        }

        if (allHotels.isEmpty()) {
            Map<String, Object> result = new HashMap<>();
            result.put("content", new ArrayList<>());
            result.put("totalElements", 0);
            result.put("totalPages", 0);
            result.put("currentPage", page);
            result.put("size", size);
            return result;
        }

        // 批量載入照片和設施（優化 N+1 問題）
        List<Integer> hotelIds = allHotels.stream()
                .map(Hotel::getId)
                .collect(Collectors.toList());
        BatchData batchData = loadPhotosAndFacilitiesBatch(hotelIds);

        // 手動分頁處理
        int totalElements = allHotels.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        int start = (page - 1) * size;

        List<HotelDTO> hotelDTOs = allHotels.stream()
                .skip(start)
                .limit(size)
                .map(h -> toDto(h, batchData.photosByHotelId, batchData.facilityIdsByHotelId))
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("content", hotelDTOs);
        result.put("totalElements", totalElements);
        result.put("totalPages", totalPages);
        result.put("currentPage", page);
        result.put("size", size);

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getHotelsByOwner(Integer userId, int page, int size, Integer cityId, Integer districtId,
            String sortBy, String sortOrder) {
        // 使用地區篩選查詢
        List<Hotel> allHotels;
        if (cityId != null || districtId != null) {
            allHotels = hotelRepository.findByUserIdAndLocation(userId, cityId, districtId);
        } else {
            allHotels = hotelRepository.findByUserId(userId);
        }

        if (allHotels.isEmpty()) {
            Map<String, Object> result = new HashMap<>();
            result.put("content", new ArrayList<>());
            result.put("totalElements", 0);
            result.put("totalPages", 0);
            result.put("currentPage", page);
            result.put("size", size);
            return result;
        }

        // 排序處理
        Comparator<Hotel> comparator = null;
        if (sortBy != null && !sortBy.isEmpty()) {
            switch (sortBy.toLowerCase()) {
                case "name":
                    comparator = Comparator.comparing(Hotel::getName,
                            Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
                    break;
                case "created_time":
                    comparator = Comparator.comparing(Hotel::getCreatedTime,
                            Comparator.nullsLast(LocalDateTime::compareTo));
                    break;
                case "updated_time":
                    comparator = Comparator.comparing(Hotel::getUpdatedTime,
                            Comparator.nullsLast(LocalDateTime::compareTo));
                    break;
                case "location":
                    // 按地區排序：城市 -> 行政區 -> 名稱
                    comparator = Comparator
                            .comparing((Hotel h) -> h.getDistrict() != null && h.getDistrict().getCity() != null
                                    ? h.getDistrict().getCity().getName()
                                    : "", Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                            .thenComparing(h -> h.getDistrict() != null ? h.getDistrict().getName() : "",
                                    Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                            .thenComparing(Hotel::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
                    break;
                case "business_status":
                    // 營業狀態排序：營業中優先
                    comparator = Comparator
                            .comparing((Hotel h) -> h.getBusinessStatus() != null && h.getBusinessStatus() ? 0 : 1)
                            .thenComparing(Hotel::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
                    break;
                default:
                    // 默認按建立時間排序
                    comparator = Comparator.comparing(Hotel::getCreatedTime,
                            Comparator.nullsLast(LocalDateTime::compareTo));
                    break;
            }

            // 應用排序方向
            if (comparator != null && "desc".equalsIgnoreCase(sortOrder)) {
                comparator = comparator.reversed();
            }

            if (comparator != null) {
                allHotels = allHotels.stream()
                        .sorted(comparator)
                        .collect(Collectors.toList());
            }
        } else {
            // 默認按建立時間降序（最新的在前）
            allHotels = allHotels.stream()
                    .sorted(Comparator.comparing(Hotel::getCreatedTime, Comparator.nullsLast(LocalDateTime::compareTo))
                            .reversed())
                    .collect(Collectors.toList());
        }

        // 批量載入照片和設施（優化 N+1 問題）
        List<Integer> hotelIds = allHotels.stream()
                .map(Hotel::getId)
                .collect(Collectors.toList());
        BatchData batchData = loadPhotosAndFacilitiesBatch(hotelIds);

        // 手動分頁處理
        int totalElements = allHotels.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        int start = (page - 1) * size;

        List<HotelDTO> hotelDTOs = allHotels.stream()
                .skip(start)
                .limit(size)
                .map(h -> toDto(h, batchData.photosByHotelId, batchData.facilityIdsByHotelId))
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("content", hotelDTOs);
        result.put("totalElements", totalElements);
        result.put("totalPages", totalPages);
        result.put("currentPage", page);
        result.put("size", size);

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getHotelsByOwner(Integer userId, int page, int size, Integer cityId, Integer districtId,
            Boolean businessStatus, Integer hotelTypeId, String sortBy, String sortOrder) {
        // 使用多條件篩選查詢
        List<Hotel> allHotels;

        // 判斷是否需要使用篩選查詢
        boolean needsFilter = (cityId != null || districtId != null || businessStatus != null || hotelTypeId != null);

        if (needsFilter) {
            allHotels = hotelRepository.findByUserIdWithFilters(userId, cityId, districtId, businessStatus,
                    hotelTypeId);
        } else {
            allHotels = hotelRepository.findByUserId(userId);
        }

        if (allHotels.isEmpty()) {
            Map<String, Object> result = new HashMap<>();
            result.put("content", new ArrayList<>());
            result.put("totalElements", 0);
            result.put("totalPages", 0);
            result.put("currentPage", page);
            result.put("size", size);
            return result;
        }

        // 排序處理
        Comparator<Hotel> comparator = null;
        if (sortBy != null && !sortBy.isEmpty()) {
            switch (sortBy.toLowerCase()) {
                case "name":
                    comparator = Comparator.comparing(Hotel::getName,
                            Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
                    break;
                case "created_time":
                    comparator = Comparator.comparing(Hotel::getCreatedTime,
                            Comparator.nullsLast(LocalDateTime::compareTo));
                    break;
                case "updated_time":
                    comparator = Comparator.comparing(Hotel::getUpdatedTime,
                            Comparator.nullsLast(LocalDateTime::compareTo));
                    break;
                case "location":
                    // 按地區排序：城市 -> 行政區 -> 名稱
                    comparator = Comparator
                            .comparing((Hotel h) -> h.getDistrict() != null && h.getDistrict().getCity() != null
                                    ? h.getDistrict().getCity().getName()
                                    : "", Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                            .thenComparing(h -> h.getDistrict() != null ? h.getDistrict().getName() : "",
                                    Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                            .thenComparing(Hotel::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
                    break;
                case "business_status":
                    // 營業狀態排序：營業中優先
                    comparator = Comparator
                            .comparing((Hotel h) -> h.getBusinessStatus() != null && h.getBusinessStatus() ? 0 : 1)
                            .thenComparing(Hotel::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
                    break;
                default:
                    // 默認按建立時間排序
                    comparator = Comparator.comparing(Hotel::getCreatedTime,
                            Comparator.nullsLast(LocalDateTime::compareTo));
                    break;
            }

            // 應用排序方向
            if (comparator != null && "desc".equalsIgnoreCase(sortOrder)) {
                comparator = comparator.reversed();
            }

            if (comparator != null) {
                allHotels = allHotels.stream()
                        .sorted(comparator)
                        .collect(Collectors.toList());
            }
        } else {
            // 默認按建立時間降序（最新的在前）
            allHotels = allHotels.stream()
                    .sorted(Comparator.comparing(Hotel::getCreatedTime, Comparator.nullsLast(LocalDateTime::compareTo))
                            .reversed())
                    .collect(Collectors.toList());
        }

        // 批量載入照片和設施（優化 N+1 問題）
        List<Integer> hotelIds = allHotels.stream()
                .map(Hotel::getId)
                .collect(Collectors.toList());
        BatchData batchData = loadPhotosAndFacilitiesBatch(hotelIds);

        // 手動分頁處理
        int totalElements = allHotels.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        int start = (page - 1) * size;

        List<HotelDTO> hotelDTOs = allHotels.stream()
                .skip(start)
                .limit(size)
                .map(h -> toDto(h, batchData.photosByHotelId, batchData.facilityIdsByHotelId))
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("content", hotelDTOs);
        result.put("totalElements", totalElements);
        result.put("totalPages", totalPages);
        result.put("currentPage", page);
        result.put("size", size);

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public HotelDTO getHotelForOwner(Integer userId, Integer hotelId) {
        // 1. 驗證用戶 ID
        if (userId == null) {
            throw new SecurityException("需要身份驗證。用戶 ID 不能為空。");
        }

        // 2. 使用 JOIN FETCH 載入所有關聯資料
        Hotel hotel = hotelRepository.findByIdWithAssociations(hotelId)
                .orElseThrow(() -> new HotelNotFoundException(hotelId));

        // 3. 驗證所有權限，確保用戶只能查看自己的飯店
        if (hotel.getUserId() == null || !hotel.getUserId().equals(userId)) {
            throw new SecurityException("未授權：您只能查看自己的飯店。");
        }

        return toDto(hotel);
    }

    @Override
    @Transactional(readOnly = true)
    public HotelDTO getHotelById(Integer hotelId) {
        // 1. 尋找指定飯店 (無權限檢查)
        // 使用 JOIN FETCH 載入所有關聯資料
        Hotel hotel = hotelRepository.findByIdWithAssociations(hotelId)
                .orElseThrow(() -> new HotelNotFoundException(hotelId));

        // 2. 檢查飯店是否為停業狀態，如果是則不允許公開查詢
        if (hotel.getBusinessStatus() == null || !hotel.getBusinessStatus()) {
            throw new HotelNotFoundException(hotelId);
        }

        return toDto(hotel);
    }

    @Override
    @Transactional
    public List<HotelDetailDto> findHotelByCity(String cityName) {
        if (cityName != null && cityName.length() != 0) {
            Pageable pageable = PageRequest.of(0, 5);
            List<HotelDetailDto> hotels = hotelRepository.findHotelByCity(cityName, pageable);
            for (HotelDetailDto hotel : hotels) {
                Photo coverPhoto = photoRepository.findFirstByHotelIdAndIsCoverTrue(hotel.getHotelId());
                if (coverPhoto != null) {
                    hotel.setPhotoUrl(coverPhoto.getUrl());
                } else {
                    hotel.setPhotoUrl("no-image.jpg");
                }
            }
            return hotels;
        }
        return null;
    }

    @Override
    @Transactional
    public List<GetNearbyHotelProjection> findByLonLat(BigDecimal latitude, BigDecimal longitude, LocalDate checkInDate,
            LocalDate checkOutDate, Integer guestNumber) {
        return hotelRepository.findByLonLat(latitude, longitude, BigDecimal.valueOf(3), checkInDate, checkOutDate,
                guestNumber);
    }
}
