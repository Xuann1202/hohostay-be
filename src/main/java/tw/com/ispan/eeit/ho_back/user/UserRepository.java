package tw.com.ispan.eeit.ho_back.user;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    public Boolean existsByEmail(String email);

    public User findByEmail(String email);

    public User findByToken(String token);

    @Query("SELECT u FROM User u WHERE CONCAT(u.lastName, u.firstName) LIKE %:keyword% OR u.email LIKE %:keyword% OR u.phoneNumber LIKE %:keyword%")
    public Page<User> findByNameOrEmail(String keyword, Pageable pageable);
}
