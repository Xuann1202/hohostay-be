package tw.com.ispan.eeit.ho_back.scategory;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import tw.com.ispan.eeit.ho_back.support.SupportRepository;
import tw.com.ispan.eeit.ho_back.user.User;
import tw.com.ispan.eeit.ho_back.user.UserRepository;

@Service
public class SCategoryService {

    private final SCategoryRepository sCategoryRepository;
    private final SupportRepository supportRepository;
    private final UserRepository userRepository;

    public SCategoryService(SCategoryRepository sCategoryRepository, SupportRepository supportRepository,
            UserRepository userRepository) {
        this.sCategoryRepository = sCategoryRepository;
        this.supportRepository = supportRepository;
        this.userRepository = userRepository;
    }

    public List<SCategoryBean> findAll() {
        return sCategoryRepository.findAll();
    }

    public SCategoryBean findById(@NonNull Integer id) {
        return sCategoryRepository.findById(id).orElse(null);
    }

    public SCategoryBean create(SCategoryBean category, Integer userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        category.setCreatedBy(user);
        category.setCreatedTime(new Date());

        return sCategoryRepository.save(category);
    }

    public SCategoryBean update(Integer id, SCategoryBean newData, Integer userId) {

        SCategoryBean old = sCategoryRepository.findById(id)
                .orElse(null);

        if (old == null)
            return null;

        old.setName(newData.getName());
        old.setDescription(newData.getDescription());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        old.setUpdatedBy(user);
        old.setUpdatedTime(new Date());

        return sCategoryRepository.save(old);
    }

    public void deleteById(@NonNull Integer id) {
        sCategoryRepository.deleteById(id);
    }

    public boolean isCategoryInUse(@NonNull Integer categoryId) {
        return supportRepository.countByCategoryId(categoryId) > 0;
    }

}
