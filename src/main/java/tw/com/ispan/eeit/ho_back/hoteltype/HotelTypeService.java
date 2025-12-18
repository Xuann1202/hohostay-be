package tw.com.ispan.eeit.ho_back.hoteltype;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;

@Service
@Transactional
public class HotelTypeService {

    @Autowired
    HotelTypeRepository hotelTypeRepository;

    public List<HotelType> findAll() {
        return hotelTypeRepository.findAll();
    }
}
