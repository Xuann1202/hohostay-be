package tw.com.ispan.eeit.ho_back.role;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleRepository extends JpaRepository<Role, Integer> {
    
    /**
     * 根據角色名稱查找角色
     */
    Optional<Role> findByName(String name);
}
