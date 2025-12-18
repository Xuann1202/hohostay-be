package tw.com.ispan.eeit.ho_back.facility;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;

@Service
@Transactional
public class FacilityService {
    @Autowired
    FacilityRepository facilityRepository;

    public List<Facility> findAllFacility() {
        return facilityRepository.findAll();
    }
}
