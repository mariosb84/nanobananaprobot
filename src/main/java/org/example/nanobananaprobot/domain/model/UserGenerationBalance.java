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

    // ========== ОСНОВНОЕ ПОЛЕ: БАЛАНС ТОКЕНОВ ==========
    @Column(name = "tokens_balance", nullable = false)
    private Integer tokensBalance = 0; // Начинаем с 0 токенов (без бесплатных)

    // ========== ПОЛЯ ДЛЯ ОБРАТНОЙ СОВМЕСТИМОСТИ ==========
    // Можно оставить, но они больше не используются в новой системе
    @Column(name = "image_balance", nullable = false)
    private Integer imageBalance = 0; // Меняем с 3 на 0 (без бесплатных)

    @Column(name = "video_balance", nullable = false)
    private Integer videoBalance = 0;

    @Column(name = "trial_used", nullable = false)
    private Boolean trialUsed = true; // Меняем на true (триал сразу использован)

    // ========== ТАЙМСТАМПЫ ==========
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    // ========== МЕТОД ДЛЯ АВТООБНОВЛЕНИЯ ВРЕМЕНИ ==========
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ========== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ==========
    /**
     * Проверяет, достаточно ли токенов для операции
     */
    public boolean hasEnoughTokens(int requiredTokens) {
        return this.tokensBalance >= requiredTokens;
    }

    /**
     * Списывает токены (возвращает true если успешно)
     */
    public boolean deductTokens(int tokens) {
        if (hasEnoughTokens(tokens)) {
            this.tokensBalance -= tokens;
            return true;
        }
        return false;
    }

    /**
     * Добавляет токены (при покупке пакета)
     */
    public void addTokens(int tokens) {
        this.tokensBalance += tokens;
    }

}