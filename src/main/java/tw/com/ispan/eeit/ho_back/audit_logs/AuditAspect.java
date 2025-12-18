package tw.com.ispan.eeit.ho_back.audit_logs;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.aop.framework.Advised;
import org.springframework.core.annotation.Order;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.StreamSupport;

import static tw.com.ispan.eeit.ho_back.audit_logs.AuditSupport.*;

@Aspect
@Component
@Order(50)
public class AuditAspect {

    @PersistenceContext
    private EntityManager em;

    private final AuditLogService audit;

    public AuditAspect(AuditLogService audit) {
        this.audit = audit;
    }

    @Pointcut("execution(* org.springframework.data.repository.CrudRepository.save(..)) || " +
            "execution(* org.springframework.data.repository.CrudRepository.saveAll(..))")
    void anySave() {
    }

    @Pointcut("execution(* org.springframework.data.repository.CrudRepository.delete(..)) || " +
            "execution(* org.springframework.data.repository.CrudRepository.deleteById(..)) || " +
            "execution(* org.springframework.data.repository.CrudRepository.deleteAll(..)) || " +
            "execution(* org.springframework.data.repository.CrudRepository.deleteAllById(..))")
    void anyDelete() {
    }

    private boolean isAuditLogEntity(Object entity) {
        return entity != null
                && entity.getClass().getPackageName().contains(".audit_logs")
                && entity.getClass().getSimpleName().equals("AuditLog");
    }

    // 解析 repository 代理，取得 domainClass（讓 deleteById 能抓 old_value）
    private Class<?> resolveDomainClassFromRepository(ProceedingJoinPoint pjp) {
        try {
            Object current = pjp.getThis();

            // 多層代理解包：繼續解包直到找到 SimpleJpaRepository
            while (current instanceof Advised advised) {
                current = advised.getTargetSource().getTarget();

                if (current instanceof SimpleJpaRepository<?, ?> simple) {
                    Field f = SimpleJpaRepository.class.getDeclaredField("entityInformation");
                    f.setAccessible(true);
                    Object entityInfo = f.get(simple);
                    if (entityInfo instanceof JpaEntityInformation<?, ?> info) {
                        return info.getJavaType();
                    }
                }
            }
        } catch (Exception ignored) {
            // 忽略錯誤，返回 null
        }
        return null;
    }

    // SAVE / SAVEALL - 自動記錄新增和編輯操作
    @Around("anySave()")
    public Object aroundSave(ProceedingJoinPoint pjp) throws Throwable {
        System.out.println("\n==========================================");
        System.out.println("=== AuditAspect.aroundSave 被觸發 ===");
        System.out.println("=== Repository: " + pjp.getTarget().getClass().getSimpleName() + " ===");

        Object[] args = pjp.getArgs();
        if (args == null || args.length == 0) {
            System.out.println("=== 無參數，直接執行 ===");
            System.out.println("==========================================\n");
            return pjp.proceed();
        }

        List<Object> entities = new ArrayList<>();
        Object first = args[0];
        System.out.println("=== 參數類型: " + (first != null ? first.getClass().getName() : "null") + " ===");

        if (first instanceof Iterable<?> it) {
            StreamSupport.stream(it.spliterator(), false).forEach(entities::add);
        } else {
            entities.add(first);
        }

        // 使用 ID 作為 key 來匹配實體，比 IdentityHashMap 更可靠
        Map<String, EntityAuditInfo> auditInfoMap = new HashMap<>();

        for (Object e : entities) {
            if (e == null) {
                System.out.println("=== 跳過實體: null ===");
                continue;
            }

            if (isAuditLogEntity(e)) {
                System.out.println("=== 跳過實體: " + e.getClass().getSimpleName() + " (AuditLog 避免遞迴) ===");
                continue;
            }

            Class<?> entityClass = e.getClass();
            String table = resolveTableName(entityClass);
            System.out.println("=== 處理實體: " + table + " (類別: " + entityClass.getSimpleName() + ") ===");
            var idOpt = extractId(e);
            System.out.println("=== ID: " + idOpt.orElse("無") + " ===");

            // 建立唯一 key：table + id（如果有的話）
            String key = buildEntityKey(table, idOpt);

            // 如果是已存在的實體（有 ID），先查詢舊值
            // 關鍵：需要從資料庫重新載入原始值，而不是從 Persistence Context 取得已修改的實體
            String oldJson = null;
            Object oldEntity = null;
            if (idOpt.isPresent() && idOpt.get() instanceof Number num) {
                try {
                    // 先清除當前實體的一級緩存狀態，確保查詢時從資料庫讀取
                    if (em.contains(e)) {
                        em.detach(e);
                        System.out.println("=== 已將當前實體從 Persistence Context 中 detach（準備查詢舊值） ===");
                    }

                    // 對於 User 實體，使用 LEFT JOIN FETCH 載入 roles 關聯
                    String queryString;
                    if (entityClass.getSimpleName().equals("User")) {
                        queryString = "SELECT e FROM " + entityClass.getSimpleName() + " e LEFT JOIN FETCH e.roles WHERE e.id = :id";
                    } else {
                        queryString = "SELECT e FROM " + entityClass.getSimpleName() + " e WHERE e.id = :id";
                    }

                    // 查詢舊值，使用 BYPASS 提示確保從資料庫獲取
                    oldEntity = em.createQuery(queryString, entityClass)
                            .setParameter("id", num)
                            .setHint("javax.persistence.cache.retrieveMode", "BYPASS")
                            .getSingleResult();

                    // 序列化為 JSON 後立即 detach，避免被後續操作影響
                    oldJson = toJson(oldEntity);
                    em.detach(oldEntity);
                    System.out.println("=== 查詢到舊值並已序列化，長度: " + (oldJson != null ? oldJson.length() : "null") + " ===");
                } catch (Exception ex) {
                    // 如果查詢失敗，可能是新實體
                    System.out.println("=== 查詢舊值失敗（可能是新實體）: " + ex.getMessage() + " ===");
                }
            }

            auditInfoMap.put(key, new EntityAuditInfo(table, oldJson, entityClass, oldEntity));
        }

        // 執行實際的保存操作
        System.out.println("\n=== 準備執行實際的保存操作 ===");
        System.out.println("=== 待處理的實體數量: " + auditInfoMap.size() + " ===");
        Object result = pjp.proceed();
        System.out.println("=== 保存操作完成 ===\n");

        // 處理保存後的實體
        List<Object> savedEntities = new ArrayList<>();
        if (result instanceof Iterable<?> it) {
            StreamSupport.stream(it.spliterator(), false).forEach(savedEntities::add);
        } else {
            savedEntities.add(result);
        }

        for (Object saved : savedEntities) {
            if (saved == null || isAuditLogEntity(saved))
                continue;

            Class<?> entityClass = saved.getClass();
            String table = resolveTableName(entityClass);
            var idOpt = extractId(saved);
            String key = buildEntityKey(table, idOpt);

            EntityAuditInfo info = auditInfoMap.get(key);
            if (info == null) {
                // 如果找不到對應的資訊，使用實體類別名稱作為 key
                info = auditInfoMap.values().stream()
                        .filter(i -> i.entityClass.equals(entityClass))
                        .findFirst()
                        .orElse(new EntityAuditInfo(table, null, entityClass));
            }

            String newJson = toJson(saved);
            String oldJson = info.oldJson;
            String action = (oldJson == null || "null".equals(oldJson)) ? "insert" : "update";
            Long targetId = idOpt
                    .map(v -> (v instanceof Number) ? ((Number) v).longValue() : null)
                    .orElse(null);

            try {
                System.out.println(
                        "=== 建立審計日誌: action=" + action + ", table=" + table + ", targetId=" + targetId + " ===");
                System.out.println("=== oldJson 長度: " + (oldJson != null ? oldJson.length() : "null") + " ===");
                System.out.println("=== newJson 長度: " + (newJson != null ? newJson.length() : "null") + " ===");

                // 根據操作類型簡化 JSON
                String simplifiedOldJson = oldJson;
                String simplifiedNewJson = newJson;

                if ("update".equals(action)) {
                    // UPDATE：只保留有變更的欄位
                    String[] simplified = AuditDiffHelper.extractChangedFields(oldJson, newJson);
                    simplifiedOldJson = simplified[0];
                    simplifiedNewJson = simplified[1];
                    System.out.println("=== UPDATE 簡化後 oldJson 長度: " +
                            (simplifiedOldJson != null ? simplifiedOldJson.length() : "null") + " ===");
                    System.out.println("=== UPDATE 簡化後 newJson 長度: " +
                            (simplifiedNewJson != null ? simplifiedNewJson.length() : "null") + " ===");
                    
                    // 如果簡化後都是空 JSON（只有關聯關係變更，沒有實體欄位變更），記錄為中間表的操作
                    if ("{}".equals(simplifiedOldJson) && "{}".equals(simplifiedNewJson)) {
                        System.out.println("=== 檢測到只有關聯關係變更，記錄為中間表操作 ===");
                        
                        // 直接從實體對象中讀取關聯關係（因為 @JsonIgnore 會導致 JSON 中沒有這些欄位）
                        try {
                            // 使用保存前查詢的舊實體
                            Object oldEntity = info.oldEntity;
                            
                            // 重新查詢保存後的實體，確保載入 roles（因為懶加載可能還沒載入）
                            Object newEntity = saved;
                            if (entityClass.getSimpleName().equals("User") && idOpt.isPresent() && idOpt.get() instanceof Number num) {
                                try {
                                    newEntity = em.createQuery(
                                            "SELECT e FROM " + entityClass.getSimpleName() + " e LEFT JOIN FETCH e.roles WHERE e.id = :id",
                                            entityClass)
                                            .setParameter("id", num)
                                            .getSingleResult();
                                    System.out.println("=== 重新查詢新實體並載入 roles ===");
                                } catch (Exception ex) {
                                    System.out.println("=== 重新查詢新實體失敗，使用保存後的實體: " + ex.getMessage() + " ===");
                                }
                            }
                            
                            // 檢測 User 實體的 roles 變更
                            if (entityClass.getSimpleName().equals("User")) {
                                Set<Object> oldRoleIds = extractRoleIds(oldEntity);
                                Set<Object> newRoleIds = extractRoleIds(newEntity);
                                
                                System.out.println("=== 舊 roles IDs: " + oldRoleIds + " ===");
                                System.out.println("=== 新 roles IDs: " + newRoleIds + " ===");
                                
                                Set<Object> added = new HashSet<>(newRoleIds);
                                added.removeAll(oldRoleIds);
                                
                                Set<Object> removed = new HashSet<>(oldRoleIds);
                                removed.removeAll(newRoleIds);
                                
                                System.out.println("=== 新增的 roles: " + added + " ===");
                                System.out.println("=== 刪除的 roles: " + removed + " ===");
                                
                                // 記錄新增的 role
                                for (Object roleId : added) {
                                    String joinTableOldJson = null; // 新增操作，舊值為 null
                                    String joinTableNewJson = String.format("{\"user_id\":%d,\"role_id\":%s}", targetId, roleId);
                                    
                                    AuditLog joinTableLog = audit.buildLog("insert", "user_role", null, joinTableOldJson, joinTableNewJson);
                                    System.out.println("=== 記錄 user_role 新增: user_id=" + targetId + ", role_id=" + roleId + " ===");
                                    audit.logAfterCommit(joinTableLog);
                                }
                                
                                // 記錄刪除的 role
                                for (Object roleId : removed) {
                                    String joinTableOldJson = String.format("{\"user_id\":%d,\"role_id\":%s}", targetId, roleId);
                                    String joinTableNewJson = null; // 刪除操作，新值為 null
                                    
                                    AuditLog joinTableLog = audit.buildLog("delete", "user_role", null, joinTableOldJson, joinTableNewJson);
                                    System.out.println("=== 記錄 user_role 刪除: user_id=" + targetId + ", role_id=" + roleId + " ===");
                                    audit.logAfterCommit(joinTableLog);
                                }
                            }
                        } catch (Exception e) {
                            System.err.println("=== 處理關聯關係變更時發生錯誤: " + e.getMessage() + " ===");
                            e.printStackTrace();
                        }
                        
                        // 跳過主表的 update 記錄
                        continue;
                    }
                } else if ("insert".equals(action)) {
                    // INSERT：移除 null 值和時間戳欄位
                    simplifiedNewJson = AuditDiffHelper.simplifyInsertJson(newJson);
                    System.out.println("=== INSERT 簡化後 newJson 長度: " +
                            (simplifiedNewJson != null ? simplifiedNewJson.length() : "null") + " ===");
                }

                AuditLog log = audit.buildLog(action, table, targetId, simplifiedOldJson, simplifiedNewJson);
                System.out.println("=== 審計日誌已建立，準備在事務提交後保存 ===");
                audit.logAfterCommit(log);
            } catch (Exception e) {
                // 記錄錯誤但不中斷主流程
                // 可以考慮使用日誌框架記錄錯誤
                System.err.println(
                        "=== Failed to create audit log for " + table + " " + action + ": " + e.getMessage() + " ===");
                e.printStackTrace();
            }
        }
        System.out.println("=== AuditAspect.aroundSave 完成 ===");
        System.out.println("==========================================\n");
        return result;
    }

    // 建立實體的唯一 key
    private String buildEntityKey(String table, Optional<Object> idOpt) {
        if (idOpt.isPresent() && idOpt.get() instanceof Number num) {
            return table + ":" + num;
        }
        return table + ":new";
    }

    // 內部類別：儲存實體審計資訊
    private static class EntityAuditInfo {
        @SuppressWarnings("unused")
        final String table; // 保留用於未來可能的用途
        final String oldJson;
        final Class<?> entityClass;
        final Object oldEntity; // 保存舊實體對象，用於提取關聯關係

        EntityAuditInfo(String table, String oldJson, Class<?> entityClass) {
            this.table = table;
            this.oldJson = oldJson;
            this.entityClass = entityClass;
            this.oldEntity = null;
        }

        EntityAuditInfo(String table, String oldJson, Class<?> entityClass, Object oldEntity) {
            this.table = table;
            this.oldJson = oldJson;
            this.entityClass = entityClass;
            this.oldEntity = oldEntity;
        }
    }

    // DELETE / DELETE(IDS) - 自動記錄刪除操作
    @Around("anyDelete()")
    public Object aroundDelete(ProceedingJoinPoint pjp) throws Throwable {
        Object[] args = pjp.getArgs();
        if (args == null || args.length == 0)
            return pjp.proceed();

        Class<?> domainClass = resolveDomainClassFromRepository(pjp);

        List<AuditLog> pending = new ArrayList<>();
        for (Object arg : args) {
            if (arg == null)
                continue;

            if (arg instanceof Iterable<?> it) {
                for (Object each : it) {
                    collectDelete(each, pending, domainClass);
                }
            } else {
                collectDelete(arg, pending, domainClass);
            }
        }

        // 執行實際的刪除操作
        Object ret = pjp.proceed();

        // 在刪除後記錄審計日誌
        for (AuditLog log : pending) {
            try {
                audit.logAfterCommit(log);
            } catch (Exception e) {
                // 記錄錯誤但不中斷主流程
                System.err.println("Failed to create audit log for delete: " + e.getMessage());
                e.printStackTrace();
            }
        }
        return ret;
    }

    private void collectDelete(Object target, List<AuditLog> out, Class<?> domainClass) {
        // delete(entity) - 直接傳入實體物件
        if (!(target instanceof Number)) {
            if (target == null || isAuditLogEntity(target))
                return;

            Class<?> entityClass = target.getClass();
            String table = resolveTableName(entityClass);
            Long id = extractId(target)
                    .map(v -> (v instanceof Number) ? ((Number) v).longValue() : null)
                    .orElse(null);

            // 嘗試從資料庫查詢舊值（如果實體已經被刪除，這裡可能查不到）
            String oldJson = null;
            if (id != null) {
                try {
                    Object old = em.find(entityClass, id);
                    oldJson = toJson(old);
                } catch (Exception ignored) {
                    // 如果查詢失敗，使用傳入的實體本身
                    oldJson = toJson(target);
                }
            } else {
                oldJson = toJson(target);
            }

            // DELETE：簡化 JSON（保留所有欄位，DELETE 通常需要完整記錄）
            String simplifiedOldJson = AuditDiffHelper.simplifyDeleteJson(oldJson);
            out.add(audit.buildLog("delete", table, id, simplifiedOldJson, null));
            return;
        }

        // deleteById(id) / deleteAllById(ids) - 只傳入 ID
        Long id = ((Number) target).longValue();
        if (domainClass != null && !isAuditLogEntityClass(domainClass)) {
            try {
                Object old = em.find(domainClass, id);
                String table = resolveTableName(domainClass);
                String oldJson = toJson(old);
                String simplifiedOldJson = AuditDiffHelper.simplifyDeleteJson(oldJson);
                out.add(audit.buildLog("delete", table, id, simplifiedOldJson, null));
            } catch (Exception ignored) {
                // 如果查詢失敗，仍然記錄刪除操作（但沒有舊值）
                String table = resolveTableName(domainClass);
                out.add(audit.buildLog("delete", table, id, null, null));
            }
        } else {
            // 無法確定實體類型，只記錄 ID
            out.add(audit.buildLog("delete", null, id, null, null));
        }
    }

    // 檢查類別是否為 AuditLog
    private boolean isAuditLogEntityClass(Class<?> clazz) {
        return clazz != null
                && clazz.getPackageName().contains(".audit_logs")
                && clazz.getSimpleName().equals("AuditLog");
    }

    /**
     * 從 User 實體中提取 role IDs
     */
    private Set<Object> extractRoleIds(Object userEntity) {
        Set<Object> roleIds = new HashSet<>();
        if (userEntity == null) {
            System.out.println("=== extractRoleIds: userEntity 為 null ===");
            return roleIds;
        }
        try {
            Field rolesField = userEntity.getClass().getDeclaredField("roles");
            rolesField.setAccessible(true);
            Object rolesObj = rolesField.get(userEntity);
            System.out.println("=== extractRoleIds: rolesObj 類型: " + (rolesObj != null ? rolesObj.getClass().getName() : "null") + " ===");
            if (rolesObj instanceof List<?> roles) {
                System.out.println("=== extractRoleIds: roles 數量: " + roles.size() + " ===");
                for (Object role : roles) {
                    Optional<Object> roleId = extractId(role);
                    if (roleId.isPresent()) {
                        roleIds.add(roleId.get());
                        System.out.println("=== extractRoleIds: 找到 role ID: " + roleId.get() + " ===");
                    } else {
                        System.out.println("=== extractRoleIds: role 沒有 ID: " + role + " ===");
                    }
                }
            } else {
                System.out.println("=== extractRoleIds: rolesObj 不是 List 類型 ===");
            }
        } catch (Exception e) {
            System.err.println("=== 提取 role IDs 失敗: " + e.getMessage() + " ===");
            e.printStackTrace();
        }
        return roleIds;
    }
}
