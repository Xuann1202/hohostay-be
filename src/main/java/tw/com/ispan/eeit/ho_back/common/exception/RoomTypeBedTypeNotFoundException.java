package tw.com.ispan.eeit.ho_back.common.exception;

public class RoomTypeBedTypeNotFoundException extends RuntimeException {
    public RoomTypeBedTypeNotFoundException(Integer id) {
        super("RoomTypeBedType not found with ID: " + id);
    }
}

