package tw.com.ispan.eeit.ho_back.audit_logs;

import org.springframework.data.jpa.domain.Specification;

public class AuditLogSpecs {

    public static Specification<AuditLog> byQuery(AuditLogQuery q) {
        return (root, cq, cb) -> {
            if (q == null) {
                System.out.println("AuditLogSpecs: Query is null");
                return cb.conjunction();
            }

            System.out.println("AuditLogSpecs: Building query with ActionType=" + q.getActionType());
            
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            
            if (q.getActionType() != null && !q.getActionType().isBlank()) {
                System.out.println("AuditLogSpecs: Adding actionType condition: " + q.getActionType());
                String actionTypeValue = q.getActionType().toLowerCase();
                
                // 處理映射：insert 應該匹配 insert 和 create
                if ("insert".equals(actionTypeValue)) {
                    // 使用 OR 條件匹配 insert 或 create（不區分大小寫）
                    predicates.add(cb.or(
                        cb.equal(cb.lower(root.get("actionType")), "insert"),
                        cb.equal(cb.lower(root.get("actionType")), "create")
                    ));
                    System.out.println("AuditLogSpecs: ActionType condition added for insert/create");
                } else {
                    // 其他類型直接匹配（不區分大小寫）
                    predicates.add(cb.equal(cb.lower(root.get("actionType")), actionTypeValue));
                    System.out.println("AuditLogSpecs: ActionType condition added with value: " + actionTypeValue);
                }
            } else {
                System.out.println("AuditLogSpecs: ActionType is null or blank, skipping");
            }
            if (q.getTargetTable() != null && !q.getTargetTable().isBlank()) {
                predicates.add(cb.equal(root.get("targetTable"), q.getTargetTable()));
            }
            if (q.getActorUserId() != null) {
                predicates.add(cb.equal(root.get("actorUserId"), q.getActorUserId()));
            }
            if (q.getTargetId() != null) {
                predicates.add(cb.equal(root.get("targetId"), q.getTargetId()));
            }
            if (q.getFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), q.getFrom()));
            }
            if (q.getTo() != null) {
                predicates.add(cb.lessThan(root.get("createdAt"), q.getTo()));
            }
            if (q.getKeyword() != null && !q.getKeyword().isBlank()) {
                String like = "%" + q.getKeyword() + "%";
                predicates.add(
                        cb.or(
                                cb.like(cb.coalesce(root.get("oldValue"), ""), like),
                                cb.like(cb.coalesce(root.get("newValue"), ""), like)));
            }
            
            System.out.println("AuditLogSpecs: Total predicates count: " + predicates.size());
            
            if (predicates.isEmpty()) {
                return cb.conjunction();
            }
            
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }
}