package tw.com.ispan.eeit.ho_back.inventory;

import tw.com.ispan.eeit.ho_back.hotel.HotelDetailDto;
import tw.com.ispan.eeit.ho_back.hotel.HotelRepository;
import tw.com.ispan.eeit.ho_back.inventory.dto.HotelQueryDto;
import tw.com.ispan.eeit.ho_back.inventory.dto.RoomPriceDto;
import tw.com.ispan.eeit.ho_back.inventory.dto.SearchResultDto;
import tw.com.ispan.eeit.ho_back.photo.Photo;
import tw.com.ispan.eeit.ho_back.photo.PhotoRepository;
import tw.com.ispan.eeit.ho_back.review.HotelReviewDto;
import tw.com.ispan.eeit.ho_back.review.ReviewRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;

import java.util.Map;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import tw.com.ispan.eeit.ho_back.room.Room;
import tw.com.ispan.eeit.ho_back.room.RoomRepository;

@Transactional
@Service
public class InventoryService {
    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    ReviewRepository reviewRepository;

    @Autowired
    HotelRepository hotelRepository;
    @Autowired
    ModelMapper modelMapper;
    @Autowired
    PhotoRepository photoRepository;

    public List<Inventory> findByQuery(Integer userId, InventoryFindDTO query) {
        // 1. 建立排序
        Sort sort = query.getDir()
                ? Sort.by("date").ascending()
                : Sort.by("date").descending();

        // 2. 計算頁碼
        int pageNumber = query.getStart() / query.getRows();

        // 3. 建立分頁請求
        Pageable pageable = PageRequest.of(pageNumber, query.getRows(), sort);

        // 4. 查詢並返回
        Page<Inventory> page = inventoryRepository.findByUserId(userId, pageable);

        // 可選：記錄 log
        System.out.println("Sorting by: date, direction: " + (query.getDir() ? "ASC" : "DESC"));
        System.out.println("Page number: " + pageNumber + ", size: " + query.getRows());

        return page.getContent();
    }

    public Long countByQuery(Integer userId, InventoryFindDTO query) {
        return inventoryRepository.countByUserId(userId);
    }

    // id存在
    public boolean exists(Integer id) {
        if (id != null) {
            return inventoryRepository.existsById(id);
        }
        return false;
    }

    // 日期+房間id
    public boolean exists(String dateStr, Integer roomId) {
        if (dateStr != null && roomId != null) {
            return true;
        }
        return false;
    }

    // find所有庫存，再依照搜尋條件顯示
    public List<Inventory> find(String json) {
        try {
            JSONObject obj = new JSONObject(json);
            return inventoryRepository.find(obj);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Inventory findById(Integer id) {
        if (id != null) {
            Optional<Inventory> optional = inventoryRepository.findById(id);
            if (optional != null && optional.isPresent()) {
                return optional.get();
            }
        }
        return null;
    }

    public Inventory modify(String json) {
        try {
            JSONObject obj = new JSONObject(json);
            Integer id = obj.isNull("id") ? null : obj.getInt("id");
            Integer stock = obj.isNull("stock") ? null : obj.getInt("stock");
            Integer price = obj.isNull("price") ? null : obj.getInt("price");

            Optional<Inventory> optional = inventoryRepository.findById(id);
            if (optional != null && optional.isPresent()) {
                Inventory update = optional.get();
                update.setStock(stock);
                update.setPrice(price);

                return this.inventoryRepository.save(update);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean remove(Integer id) {
        if (id != null) {
            try {
                Optional<Inventory> optional = this.inventoryRepository.findById(id);
                if (optional != null && optional.isPresent()) {
                    this.inventoryRepository.deleteById(id);
                    return true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    // 新增庫存service
    public Inventory create(InventoryRequest request) {
        // 查重 - 改用 LocalDate
        boolean exists = inventoryRepository
                .findByRoom_IdAndDate(request.getRoomId(), request.getDate())
                .isPresent();
        if (exists) {
            throw new RuntimeException("庫存已有這筆資料");
        }

        Inventory inventory = new Inventory();
        inventory.setStock(request.getStock());
        inventory.setPrice(request.getPrice());
        inventory.setDate(request.getDate()); // LocalDate
        inventory.setStartDate(request.getStartDate()); // LocalDate

        // 找 room
        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new RuntimeException(
                        "找不到 room id: " + request.getRoomId()));
        inventory.setRoom(room);

        return inventoryRepository.save(inventory);
    }

    @Transactional
    public void insertRange(List<InventoryRequest> list) {
        for (InventoryRequest request : list) {
            // ✅ 將 DTO 轉換成 Entity
            Inventory bean = new Inventory();
            bean.setStock(request.getStock());
            bean.setPrice(request.getPrice());
            bean.setDate(request.getDate()); // LocalDate

            // ✅ 處理 Room 關聯
            Room room = roomRepository.findById(request.getRoomId())
                    .orElseThrow(() -> new RuntimeException("找不到房型 ID: " + request.getRoomId()));
            bean.setRoom(room);

            inventoryRepository.insert(bean);
        }
    }

    public List<Inventory> findStockBetweenDates(LocalDate start, LocalDate end) {
        return inventoryRepository.findByDateBetween(start, end);
    }

    public Inventory findInventoryById(Integer id) {
        Inventory inventory = inventoryRepository.findById(id).orElseThrow(() -> new RuntimeException("庫存不存在"));
        return inventory;
    }

    public List<Integer> findAvailableHotelIds(HotelQueryDto query) {
        return inventoryRepository.findAvailableHotelIds(query);
    }

    public List<Integer> filterHotelsByFacilities(HotelQueryDto query) {
        List<Integer> hotelIds = inventoryRepository.findAvailableHotelIds(query);
        if (query.getFacilities() != null && !query.getFacilities().isEmpty()) {
            hotelIds = inventoryRepository.filterHotelsByFacilities(hotelIds, query);
        }
        return hotelIds;
    }

    List<RoomPriceDto> findRoomPriceByHotel(HotelQueryDto query) {
        List<Integer> hotelIds = inventoryRepository.findAvailableHotelIds(query);
        if (query.getFacilities() != null && !query.getFacilities().isEmpty()) {
            hotelIds = inventoryRepository.filterHotelsByFacilities(hotelIds, query);
        }
        return inventoryRepository.findRoomPriceByHotels(hotelIds, query);
    }

    // 返回搜尋結果
    public Map<String, Object> searchHotel(HotelQueryDto query) {
        List<SearchResultDto> searchResults = new ArrayList<>();
        System.out.println("query" + query);
        List<Integer> hotelIds = inventoryRepository.findAvailableHotelIds(query);
        System.out.println(hotelIds);
        if (hotelIds.isEmpty()) {
            return null;
        }
        if (query.getFacilities() != null && !query.getFacilities().isEmpty()) {
            hotelIds = inventoryRepository.filterHotelsByFacilities(hotelIds, query);
        }
        List<HotelDetailDto> hotelDetails = hotelRepository.findHotelDetail(hotelIds);
        List<RoomPriceDto> roomPrices = inventoryRepository.findRoomPriceByHotels(hotelIds, query);
        List<HotelReviewDto> reviews = reviewRepository.findReviewsByHotelIds(hotelIds);

        // 取得各個飯店所有房型的價格
        Map<Integer, List<RoomPriceDto>> hotelsRoomsPrices = new HashMap<>();
        for (RoomPriceDto roomPrice : roomPrices) {
            Integer hotelId = roomPrice.getHotelId();
            if (!hotelsRoomsPrices.containsKey(hotelId)) {
                hotelsRoomsPrices.put(hotelId, new ArrayList<>());
            }
            hotelsRoomsPrices.get(hotelId).add(roomPrice);
        }

        // 算平均分數
        // <HotelId,[sum, count]>
        Map<Integer, Integer[]> hotelRatingMap = new HashMap<>();
        if (!reviews.isEmpty()) {
            for (HotelReviewDto review : reviews) {
                Integer hotelId = review.getHotelId();
                Integer rating = review.getRating();
                if (!hotelRatingMap.containsKey(hotelId)) {
                    hotelRatingMap.put(hotelId, new Integer[] { rating, 1 });
                } else {
                    Integer[] arr = hotelRatingMap.get(hotelId);
                    arr[0] += rating;
                    arr[1]++;
                }
            }
        }
        Map<Integer, Double> hotelAvgRating = new HashMap<>();
        for (Map.Entry<Integer, Integer[]> entry : hotelRatingMap.entrySet()) {
            Integer hotelId = entry.getKey();
            Integer sumRating = entry.getValue()[0];
            Integer ratingCount = entry.getValue()[1];
            Double avgRating = (double) sumRating / ratingCount;
            hotelAvgRating.put(hotelId, avgRating);
        }

        // 把飯店資訊、房型、價格、平均分數加到搜尋結果
        for (HotelDetailDto hotelDetail : hotelDetails) {
            // 加照片到搜尋結果
            Photo coverPhoto = photoRepository.findFirstByHotelIdAndIsCoverTrue(hotelDetail.getHotelId());
            if (coverPhoto != null) {
                hotelDetail.setPhotoUrl(coverPhoto.getUrl());
            } else {
                hotelDetail.setPhotoUrl("no-image.jpg");
            }
            // 加入hotelDetail
            SearchResultDto searchResult = modelMapper.map(hotelDetail, SearchResultDto.class);
            List<RoomPriceDto> hotelRoomPrices = hotelsRoomsPrices.get(hotelDetail.getHotelId());
            RoomPriceDto minRoomPrice = hotelRoomPrices.get(0);
            for (RoomPriceDto hotelRoomPrice : hotelRoomPrices) {
                if (hotelRoomPrice.getPartPrice() < minRoomPrice.getPartPrice()) {
                    minRoomPrice = hotelRoomPrice;
                }
            }
            modelMapper.map(minRoomPrice, searchResult);

            Double avgRating = hotelAvgRating.getOrDefault(hotelDetail.getHotelId(),
                    0.0);
            searchResult.setAvgRating(avgRating);
            searchResult.setNight(query.getNight());
            searchResults.add(searchResult);
        }

        Integer totalElement = searchResults.size();
        // 排序
        sortResults(searchResults, query.getSortBy(), query.getSortOrder());
        searchResults = paginateResults(searchResults, query.getPage(), query.getSize());

        Map<String, Object> result = new HashMap<>();
        result.put("totalElement", totalElement);
        result.put("searchResults", searchResults);
        return result;
    }

    // 排序
    private void sortResults(List<SearchResultDto> results, String sortBy, String sortOrder) {
        Comparator<SearchResultDto> comparator = null;
        switch (sortBy.toLowerCase()) {
            case "price":
                comparator = (a, b) -> Long.compare(
                        a.getPartPrice() != null ? a.getPartPrice() : 0L,
                        b.getPartPrice() != null ? b.getPartPrice() : 0L);
                break;
            case "rating":
                comparator = (a, b) -> Double.compare(
                        a.getAvgRating() != null ? a.getAvgRating() : 0.0,
                        b.getAvgRating() != null ? b.getAvgRating() : 0.0);
                break;
            case "starrating":
                comparator = (a, b) -> Integer.compare(
                        a.getStarRating() != null ? a.getStarRating() : 0,
                        b.getStarRating() != null ? b.getStarRating() : 0);
                break;
            case "maxoccupancy":
                comparator = (a, b) -> Integer.compare(
                        a.getMaxOccupancy() != null ? a.getMaxOccupancy() : 0,
                        b.getMaxOccupancy() != null ? b.getMaxOccupancy() : 0);
                break;
            default:
                return;
        }

        // 處理排序方向
        if ("desc".equalsIgnoreCase(sortOrder)) {
            comparator = comparator.reversed();
        }

        results.sort(comparator);
    }

    // 分頁
    private List<SearchResultDto> paginateResults(List<SearchResultDto> allResults, Integer page, Integer size) {
        int totalElements = allResults.size();
        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, totalElements);

        // 防止越界
        if (fromIndex >= totalElements) {
            return new ArrayList<>();
        }

        return allResults.subList(fromIndex, toIndex);
    }
}
