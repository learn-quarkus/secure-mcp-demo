package demo.quarkus.repository;

import java.time.LocalDateTime;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
@Cacheable
public class Complaint {

    @Id
    @GeneratedValue
    public Long id;

    @Column(length = 2000)
    public String description;


    @Column(length = 50, name = "order_id")
    public String orderId;

    @Column(length = 50, name = "product_id")
    public String productId;

    @Column(length = 200, name = "product_name")
    public String productName;
    
    @Column(length = 200, name = "user_name")
    public String userName;

    @Column(length = 50, name = "category")
    public String category;
    
    @Column(length = 50)
    public Integer rating;
    
    @Column(name = "complaint_date")
    public LocalDateTime complaintDate;


    public Complaint() {
    }

    public Complaint(String description) {
        this.description = description;
    }
}
