package tw.com.ispan.eeit.ho_back.inventory;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import tw.com.ispan.eeit.ho_back.hotel.Hotel;
import tw.com.ispan.eeit.ho_back.hotel.HotelDTO;
import tw.com.ispan.eeit.ho_back.room.Room;
import tw.com.ispan.eeit.ho_back.room.RoomDTO;

@RestController
@RequestMapping("/api/inventory")
public class InventoryAjaxController {
    @Autowired
    private InventoryService inventoryService;

    @PostMapping("/query")
    public InventoryResponse queryInventory(
            @RequestHeader(value = "userid", required = false) Integer userId, // 從 header 拿房東 id
            @RequestBody InventoryFindDTO query // 分頁、排序、其他查詢條件
    ) {
        if (userId == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "缺少 userId");
        }

        // 設定預設值
        if (query.getStart() == null)
            query.setStart(0);
        if (query.getRows() == null)
            query.setRows(20);
        if (query.getSort() == null)
            query.setSort("date");
        if (query.getDir() == null)
            query.setDir(true);

        // 查詢資料（帶分頁、排序）
        List<Inventory> inventorys = inventoryService.findByQuery(userId, query);

        // 查詢總筆數
        Long total = inventoryService.countByQuery(userId, query);

        // DTO 轉換
        List<InventoryDTO> list = inventorys.stream().map(bean -> {
            Room room = bean.getRoom();
            Hotel hotel = room.getHotel();

            HotelDTO hotelDTO = new HotelDTO();
            hotelDTO.setId(hotel.getId());
            hotelDTO.setName(hotel.getName());

            RoomDTO roomDTO = new RoomDTO();
            roomDTO.setId(room.getId());
            roomDTO.setName(room.getName());
            roomDTO.setHotel(hotelDTO);

            InventoryDTO dto = new InventoryDTO();
            dto.setId(bean.getId());
            dto.setStock(bean.getStock());
            dto.setPrice(bean.getPrice());
            dto.setDate(bean.getDate());
            dto.setRoom(roomDTO);

            return dto;
        }).toList();

        return new InventoryResponse("查詢成功", true, list, total);
    }

    @GetMapping("/{id}")
    public String findById(@PathVariable Integer id) {
        JSONObject response = new JSONObject();
        JSONArray array = new JSONArray();
        if (id != null) {
            Inventory inventory = inventoryService.findById(id);
            if (inventory != null) {
                // ✅ LocalDate.toString() 預設輸出 "yyyy-MM-dd" 格式
                String date = inventory.getDate().toString();
                String startDate = inventory.getStartDate().toString();

                JSONObject item = new JSONObject()
                        .put("id", inventory.getId())
                        .put("stock", inventory.getStock())
                        .put("price", inventory.getPrice())
                        .put("date", date)
                        .put("startDate", startDate);

                array = array.put(item);
            }
        }
        response.put("list", array);
        return response.toString();
    }

    @PostMapping("/insert")
    public ResponseEntity<InventoryResponse> create(
            @RequestHeader(value = "userid", required = false) Integer userId,
            @RequestBody InventoryRequest inventoryDTO) {
        try {
            inventoryService.create(inventoryDTO);

            // ✅ 簡化版:不回傳 data
            InventoryResponse response = new InventoryResponse(
                    "新增成功",
                    true,
                    null, // 或 Collections.emptyList()
                    null);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (RuntimeException e) {
            InventoryResponse response = new InventoryResponse(
                    e.getMessage(),
                    false,
                    null,
                    null);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (Exception e) {
            e.printStackTrace();
            InventoryResponse response = new InventoryResponse(
                    "新增失敗: " + e.getMessage(),
                    false,
                    null,
                    null);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @PostMapping("/insertRange")
    public ResponseEntity<InventoryResponse> insertRange(
            @RequestHeader(value = "userid", required = false) Integer userId,
            @RequestBody List<InventoryRequest> list // ✅ 改用 DTO 接收
    ) {
        try {
            inventoryService.insertRange(list);

            // ✅ 使用統一的 InventoryResponse
            InventoryResponse response = new InventoryResponse(
                    "批次新增成功",
                    true,
                    null, // 或 Collections.emptyList()
                    (long) list.size() // 回傳新增的筆數
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (RuntimeException e) {
            InventoryResponse response = new InventoryResponse(
                    e.getMessage(),
                    false,
                    null,
                    0L);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (Exception e) {
            e.printStackTrace();
            InventoryResponse response = new InventoryResponse(
                    "批次新增失敗: " + e.getMessage(),
                    false,
                    null,
                    0L);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @DeleteMapping("/{id}")
    public InventoryResponse remove(@PathVariable Integer id) {
        if (id == null) {
            return new InventoryResponse("id是必要欄位", false, null, null);
        } else if (!inventoryService.exists(id)) {
            return new InventoryResponse("id不存在", false, null, null);
        } else {
            if (!inventoryService.remove(id)) {
                return new InventoryResponse("刪除失敗", false, null, null);
            } else {
                return new InventoryResponse("刪除成功", true, null, null);
            }
        }
    }

    @PutMapping("/{id}")
    public InventoryResponse modify(@PathVariable Integer id, @RequestBody String json) {
        if (id == null) {
            return new InventoryResponse("id是必要欄位", false, null, null);
        } else if (!inventoryService.exists(id)) {
            return new InventoryResponse("id不存在", false, null, null);
        } else {
            Inventory update = inventoryService.modify(json);
            if (update == null) {
                return new InventoryResponse("修改失敗", false, null, null);
            } else {
                return new InventoryResponse("修改成功", true, null, null);
            }
        }
    }

}