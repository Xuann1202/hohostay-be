package tw.com.ispan.eeit.ho_back.inventory;

import java.util.List;

import lombok.Data;

@Data
public class InventoryResponse {
    private String message;
    private boolean success;
    private List<InventoryDTO> data;
    private Long total; // 新增：總筆數
    // 原本可能有的 getter / setter

    // 新增建構子
    public InventoryResponse(String message, boolean success, List<InventoryDTO> data, Long total) {
        this.message = message;
        this.success = success;
        this.data = data;
        this.total = total;
    }

    // 也可以加上無參構造器，方便序列化
    public InventoryResponse() {
    }
}