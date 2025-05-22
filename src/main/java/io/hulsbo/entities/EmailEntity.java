package io.hulsbo.entities;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "emails")
public class EmailEntity extends PanacheEntityBase {

    @Id
    public UUID id;

    @Column(nullable = false, unique = true, length = 255)
    public String email;

    @UpdateTimestamp
    @Column(name = "date_modified")
    public LocalDateTime dateModified;

    @CreationTimestamp
    @Column(name = "date_created", nullable = false, updatable = false)
    public LocalDateTime dateCreated;

    @Column(name = "opt_out", nullable = false)
    public boolean optOut = false;

    @Column(name = "opt_out_date")
    public LocalDateTime optOutDate;

    @Column(name = "opt_out_reason", length = 255)
    public String optOutReason;

    // Default constructor required by JPA
    public EmailEntity() {
        this.id = UUID.randomUUID(); // Initialize UUID, as gen_random_uuid() is on DB side
    }

    // Convenience constructor
    public EmailEntity(String email) {
        this(); // Call default constructor to initialize id
        this.email = email;
    }

    // Add getters and setters if needed, or use Panache's public fields
} 