package tw.com.ispan.eeit.ho_back.inventory;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.hibernate.Session;
import org.json.JSONObject;
import org.springframework.stereotype.Repository;

import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import tw.com.ispan.eeit.ho_back.hotel.Hotel;
import tw.com.ispan.eeit.ho_back.room.Room;
import tw.com.ispan.eeit.ho_back.util.DatetimeConverter;

@Repository
public class InventoryDAOImpl implements InventoryDAO {
    @PersistenceContext
    private Session session;

    public Session getSession() {
        return session;
    }

    @Override
    public Inventory insert(Inventory bean) {
        session.persist(bean);
        return bean;
    }

    // 拿掉分頁的功能
    @Override
    public List<Inventory> find(JSONObject obj) {
        Integer stock = obj.isNull("stock") ? null : obj.getInt("stock");
        Integer price = obj.isNull("price") ? null : obj.getInt("price");
        String date = obj.isNull("date") ? null : obj.getString("date");
        String startDate = obj.isNull("startDate") ? null : obj.getString("startDate");

        // 日期範圍變數
        String firstDate = obj.isNull("firstDate") ? null : obj.getString("firstDate");
        String lastDate = obj.isNull("lastDate") ? null : obj.getString("lastDate");

        // ✅ 外鍵關聯欄位 (room_name)
        String roomName = obj.isNull("roomName") ? null : obj.getString("roomName");
        Integer roomId = obj.isNull("roomId") ? null : obj.getInt("roomId");

        int start = obj.isNull("start") ? 0 : obj.getInt("start");
        int rows = obj.isNull("rows") ? 5 : obj.getInt("rows");
        boolean dir = obj.isNull("dir") ? false : obj.getBoolean("dir");
        String order = obj.isNull("order") ? "id" : obj.getString("order");

        CriteriaBuilder criteriaBuilder = this.getSession().getCriteriaBuilder();
        CriteriaQuery<Inventory> criteriaQuery = criteriaBuilder.createQuery(Inventory.class);

        Root<Inventory> table = criteriaQuery.from(Inventory.class);

        // ✅ JOIN Room (只有在需要時才 Join)
        Join<Inventory, Room> roomJoin = null; // 改成 RoomBean
        if (roomName != null || roomId != null) {
            // 這裡的 "room" 必須對應 InventoryBean 中的屬性名稱
            roomJoin = table.join("room", JoinType.INNER);
        }

        List<Predicate> predicates = new ArrayList<>();
        // ✅ 新增：roomName 查詢條件
        if (roomName != null && !roomName.isEmpty()) {
            predicates.add(criteriaBuilder.equal(roomJoin.get("roomName"), roomName));
        }

        // ✅ 新增：roomId 查詢條件
        if (roomId != null) {
            predicates.add(criteriaBuilder.equal(roomJoin.get("id"), roomId));
        }

        if (stock != null) {
            predicates.add(criteriaBuilder.equal(table.get("stock"), stock));
        }

        if (price != null) {
            predicates.add(criteriaBuilder.equal(table.get("price"), price));
        }

        if (date != null && date.length() != 0) {
            Date dateValue = DatetimeConverter.parse(date, "yyyy-MM-dd");
            predicates.add(criteriaBuilder.equal(table.get("date"), dateValue));
        }

        if (startDate != null && startDate.length() != 0) {
            Date startDateValue = DatetimeConverter.parse(startDate, "yyyy-MM-dd");
            predicates.add(criteriaBuilder.equal(table.get("startDate"), startDateValue));
        }

        if (firstDate != null && !firstDate.isEmpty()) {
            Date firstDateValue = DatetimeConverter.parse(firstDate, "yyyy-MM-dd");

            if (lastDate != null && !lastDate.isEmpty()) {
                // 有區間：firstDate ~ lastDate
                Date end = DatetimeConverter.parse(lastDate, "yyyy-MM-dd");
                predicates.add(criteriaBuilder.between(table.get("date"), firstDateValue, end));
            } else {
                // 只輸入 firstDate：查當天
                // SQL: date >= firstDate AND date < firstDate + 1 day
                Calendar cal = Calendar.getInstance();
                cal.setTime(firstDateValue);
                cal.add(Calendar.DATE, 1);
                Date nextDay = cal.getTime();

                predicates.add(criteriaBuilder.greaterThanOrEqualTo(table.get("date"), firstDateValue));
                predicates.add(criteriaBuilder.lessThan(table.get("date"), nextDay));
            }
        }
        if (!predicates.isEmpty()) {
            Predicate[] array = predicates.toArray(new Predicate[0]);
            criteriaQuery = criteriaQuery.where(array);
        }
        if (dir) {
            Order o = criteriaBuilder.desc(table.get(order));
            criteriaQuery = criteriaQuery.orderBy(o);
        } else {
            criteriaQuery = criteriaQuery.orderBy(
                    criteriaBuilder.asc(table.get(order)));
        }
        List<Inventory> result = this.getSession()
                .createQuery(criteriaQuery)
                .setFirstResult(start)
                .setMaxResults(rows)
                .getResultList();
        return result.isEmpty() ? null : result;
    }

    @Override
    public List<Inventory> findByUserId(Integer userId) {
        if (userId == null) {
            return new ArrayList<>();
        }

        CriteriaBuilder cb = session.getCriteriaBuilder();
        CriteriaQuery<Inventory> cq = cb.createQuery(Inventory.class);
        Root<Inventory> root = cq.from(Inventory.class);

        // join Room
        Join<Inventory, Room> roomJoin = root.join("room", JoinType.INNER);
        // join Hotel
        Join<Room, Hotel> hotelJoin = roomJoin.join("hotel", JoinType.INNER);

        // 過濾 user_id
        Predicate userPredicate = cb.equal(hotelJoin.get("userId"), userId);
        cq.where(userPredicate);

        return session.createQuery(cq).getResultList();
    }
}