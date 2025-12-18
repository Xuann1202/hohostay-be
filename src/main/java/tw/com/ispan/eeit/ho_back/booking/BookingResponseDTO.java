package tw.com.ispan.eeit.ho_back.booking;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import lombok.Data;

@Data
public class BookingResponseDTO {
    private Integer id;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer night;

    private String paymentType;
    private LocalDateTime paymentDate;
    private LocalDateTime bookingDate;

    private LocalTime checkInTime;
    private String request;
    private String leadGuest;

    private LocalDateTime updatedTime;
    private Integer status;
    private Integer totalPrice;

    private Integer userId;
    private String userName;

    private List<String> roomNames;

}
