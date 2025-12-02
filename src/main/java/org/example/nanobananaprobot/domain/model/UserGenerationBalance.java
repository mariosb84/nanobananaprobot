package org.example.nanobananaprobot.domain.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_generation_balance")
@Data
public class UserGenerationBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "image_balance", nullable = false)
    private Integer imageBalance = 0;

    @Column(name = "video_balance", nullable = false)
    private Integer videoBalance = 0;

    @Column(name = "trial_used", nullable = false)
    private Boolean trialUsed = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

}