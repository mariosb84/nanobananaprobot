package org.example.nanobananaprobot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nanobananaprobot.domain.model.UserGenerationBalance;
import org.example.nanobananaprobot.repository.UserGenerationBalanceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class GenerationBalanceService {

    private final UserGenerationBalanceRepository balanceRepository;

    @Transactional
    public UserGenerationBalance getOrCreateBalance(Long userId) {
        return balanceRepository.findByUserId(userId)
                .orElseGet(() -> {
                    UserGenerationBalance newBalance = new UserGenerationBalance();
                    newBalance.setUserId(userId);
                    newBalance.setImageBalance(3); /* 3 бесплатные генерации*/
                    newBalance.setVideoBalance(0);
                    newBalance.setTrialUsed(false);
                    return balanceRepository.save(newBalance);
                });
    }

    @Transactional
    public boolean useImageGeneration(Long userId) {
        UserGenerationBalance balance = getOrCreateBalance(userId);

        /* Если есть бесплатные генерации*/
        if (!balance.getTrialUsed() && balance.getImageBalance() > 0) {
            balance.setImageBalance(balance.getImageBalance() - 1);

            /* Если бесплатные закончились*/
            if (balance.getImageBalance() == 0) {
                balance.setTrialUsed(true);
            }

            balance.setUpdatedAt(LocalDateTime.now());
            balanceRepository.save(balance);
            return true;
        }

        return false;
    }

    @Transactional
    public boolean canGenerateImage(Long userId) {
        UserGenerationBalance balance = getOrCreateBalance(userId);

        /* Если есть бесплатные генерации*/
        if (!balance.getTrialUsed() && balance.getImageBalance() > 0) {
            return true;
        }

        /* TODO: проверять купленные генерации*/
        return balance.getImageBalance() > 0;
    }

    @Transactional
    public void addImageGenerations(Long userId, Integer count) {
        UserGenerationBalance balance = getOrCreateBalance(userId);
        balance.setImageBalance(balance.getImageBalance() + count);
        balance.setUpdatedAt(LocalDateTime.now());
        balanceRepository.save(balance);
    }

    @Transactional
    public void addVideoGenerations(Long userId, Integer count) {
        UserGenerationBalance balance = getOrCreateBalance(userId);
        balance.setVideoBalance(balance.getVideoBalance() + count);
        balance.setUpdatedAt(LocalDateTime.now());
        balanceRepository.save(balance);
    }

    public Integer getImageBalance(Long userId) {
        return getOrCreateBalance(userId).getImageBalance();
    }

    public Integer getVideoBalance(Long userId) {
        return getOrCreateBalance(userId).getVideoBalance();
    }

}