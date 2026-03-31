package org.example.nanobananaprobot.repository;

import org.example.nanobananaprobot.domain.model.OperationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OperationHistoryRepository extends JpaRepository<OperationHistory, Long> {
    int countByUserIdAndOperationTypeInAndStatus(Long userId, List<String> operationTypes, String status);
}