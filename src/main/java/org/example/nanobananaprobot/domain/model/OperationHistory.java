package org.example.nanobananaprobot.domain.model;

import jakarta.persistence.*;
import lombok.Data;
import org.example.nanobananaprobot.domain.JsonbConverter;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "operation_history")
@Data
public class OperationHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "operation_type")
    private String operationType;

    @Column(name = "tokens_change")
    private Integer tokensChange;

    @Column(name = "tokens_balance_after")
    private Integer tokensBalanceAfter;

    /*@Convert(converter = JsonbConverter.class)*/
    @Column(columnDefinition = "jsonb")
    /*private Map<String, Object> details;*/
    private String details;

    private String status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

}