package demo.quarkus.entity;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

import java.time.LocalDateTime;

@Entity
@Cacheable
public class ComplaintEntity extends PanacheEntity {

    @Column(name = "order_id", length = 50)
    public String orderId;


    @Column(length = 2000)
    public String description;

    @Column(name = "product_id", length = 50)
    public String productId;

    @Column(name = "product_name", length = 200)
    public String productName;


    @Column(name = "user_name", length = 200)
    public String userName;

    @Column(name = "category", length = 50)
    public String category;
    
    @Column(name = "rating", length = 50)
    public Integer rating;

    @Column(name = "complaint_date")
    public LocalDateTime complaintDate;

    public ComplaintEntity() {
    }

    public ComplaintEntity( String orderId, String productId, String productName, String userName, String category, Integer rating, String description, LocalDateTime complaintDate) {
        this.orderId = orderId ;
        this.description = description ;
        this.productId = productId ;
        this.productName = productName ;
        this.userName = userName ;
        this.category = category ;
        this.rating = rating ;
        this.complaintDate = complaintDate ;
    }
}
