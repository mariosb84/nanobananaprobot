package org.example.nanobananaprobot.repository;

import jakarta.persistence.LockModeType;
import org.example.nanobananaprobot.domain.model.UserGenerationBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserGenerationBalanceRepository extends JpaRepository<UserGenerationBalance, Long> {
    Optional<UserGenerationBalance> findByUserId(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM UserGenerationBalance b WHERE b.userId = :userId")
    Optional<UserGenerationBalance> findByUserIdForUpdate(@Param("userId") Long userId);
}