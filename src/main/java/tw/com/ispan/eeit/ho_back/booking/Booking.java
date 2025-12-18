package tw.com.ispan.eeit.ho_back.booking;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;
import tw.com.ispan.eeit.ho_back.bookingInventory.BookingInventory;
import tw.com.ispan.eeit.ho_back.review.Review;
import tw.com.ispan.eeit.ho_back.user.User;

@Data
@Entity
@Table(name = "booking")
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "night", nullable = false)
    private Integer night;

    @Column(name = "payment_type")
    private String paymentType;

    @Column(name = "payment_date")
    private LocalDateTime paymentDate;

    @CreationTimestamp
    @Column(name = "booking_date")
    private LocalDateTime bookingDate;

    @Column(name = "check_in_time")
    private LocalTime checkInTime;

    @Column(name = "request")
    private String request;

    @Column(name = "coupon_id")
    private Integer couponId;

    @Column(name = "lead_guest")
    private String leadGuest;

    @UpdateTimestamp
    @Column(name = "updated_time")
    private LocalDateTime updatedTime;

    @Column(name = "status")
    private Integer status;

    @Column(name = "total_price")
    private Integer totalPrice;

    private String ecpayTradeNo;
    private String ecpayMerchantTradeNo;
    private String ecpayPaymentType;
    private String ecpayAuthCode;
    @Column(columnDefinition = "CHAR(10)")
    private String phone;

    @JsonIgnore
    @OneToMany(mappedBy = "booking")
    @JsonIgnoreProperties("booking")
    private List<BookingInventory> bookingInventories = new ArrayList<>();

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "user_id")
    @JsonIgnoreProperties("bookings")
    private User user;

    @JsonIgnore
    @OneToOne(mappedBy = "booking")
    private Review review;

    @Override
    public String toString() {
        return "Booking [id=" + id + ", startDate=" + startDate + ", endDate=" + endDate + ", night=" + night
                + ", paymentType=" + paymentType + ", paymentDate=" + paymentDate + ", bookingDate=" + bookingDate
                + ", checkInTime=" + checkInTime + ", request=" + request + ", couponId=" + couponId + ", leadGuest="
                + leadGuest + ", updatedTime=" + updatedTime + ", status=" + status + ", totalPrice=" + totalPrice
                + "]";
    }

}