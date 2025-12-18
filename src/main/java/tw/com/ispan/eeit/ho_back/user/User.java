package tw.com.ispan.eeit.ho_back.user;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;
import tw.com.ispan.eeit.ho_back.booking.Booking;
import tw.com.ispan.eeit.ho_back.hotel.Hotel;
import tw.com.ispan.eeit.ho_back.review.Review;
import tw.com.ispan.eeit.ho_back.role.Role;

@Data
@Table(name = "[user]")
@Entity
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String googleId;
    private String loginProvider = "LOCAL";
    private String email;
    private String password;
    private String firstName;
    private String lastName;
    private String image;
    @Column(columnDefinition = "CHAR(10)")
    private String phoneNumber;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateOfBirth;
    private String address;
    @Column(columnDefinition = "CHAR(1)")
    private String gender;
    @Column(columnDefinition = "CHAR(64)")
    private String token;
    @JsonIgnore
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime tokenExpireTime;
    private Integer status = 1; // 0:停用 1:未驗證 2:已驗證

    @CreationTimestamp
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JsonIgnore
    private LocalDateTime createdTime;

    @UpdateTimestamp
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JsonIgnore
    private LocalDateTime updatedTime;

    @JsonIgnore
    @JsonIgnoreProperties("users")
    @ManyToMany
    @JoinTable(name = "user_role", joinColumns = {
            @JoinColumn(name = "user_id", referencedColumnName = "id") }, inverseJoinColumns = {
                    @JoinColumn(name = "role_id", referencedColumnName = "id") })
    private List<Role> roles = new ArrayList<>();

    @JsonIgnore
    @JsonIgnoreProperties("user")
    @OneToMany(mappedBy = "user")
    private List<Hotel> hotels;

    @JsonIgnoreProperties("whosWishList")
    @JsonIgnore
    @ManyToMany
    @JoinTable(name = "wishlist", joinColumns = {
            @JoinColumn(name = "user_id", referencedColumnName = "id") }, inverseJoinColumns = {
                    @JoinColumn(name = "hotel_id", referencedColumnName = "id") })
    private List<Hotel> wishList = new ArrayList<>();

    @JsonIgnore
    @JsonIgnoreProperties("user")
    @OneToMany(mappedBy = "user")
    private List<Booking> bookings;

    @JsonIgnore
    @JsonIgnoreProperties("user")
    @OneToMany(mappedBy = "user")
    private List<Review> reviews;

    @Override
    public String toString() {
        return "user [id=" + id + ", email=" + email + ", first_name=" + firstName + ", last_name=" + lastName
                + ", phone_number=" + phoneNumber + ", date_of_birth=" + dateOfBirth + ", address=" + address
                + ", gender=" + gender + "]";
    }

    public void addRole(Role role) {
        if (!roles.contains(role)) {
            roles.add(role);
        } else {
            throw new RuntimeException("使用者已有此角色");
        }
    }
}