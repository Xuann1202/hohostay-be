package tw.com.ispan.eeit.ho_back.inventory;

import java.util.List;

import org.json.JSONObject;

public interface InventoryDAO {

    List<Inventory> find(JSONObject obj);

    Inventory insert(Inventory bean);

    List<Inventory> findByUserId(Integer userId);
}
