package tw.com.ispan.eeit.ho_back.common.exception;

public class HotelNotFoundException extends RuntimeException {
    public HotelNotFoundException(Integer id) {
        super("Hotel not found with ID: " + id);
    }
}

