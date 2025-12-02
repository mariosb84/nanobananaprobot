package org.example.nanobananaprobot.repository;

import org.example.nanobananaprobot.domain.model.UserGenerationBalance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserGenerationBalanceRepository extends JpaRepository<UserGenerationBalance, Long> {
    Optional<UserGenerationBalance> findByUserId(Long userId);
}