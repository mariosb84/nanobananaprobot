package org.example.nanobananaprobot.repository;

import org.example.nanobananaprobot.domain.model.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @EntityGraph(type = EntityGraph.EntityGraphType.FETCH, attributePaths = "roles")
    List<User> findAll();

    @EntityGraph(type = EntityGraph.EntityGraphType.FETCH, attributePaths = "roles")
    List<User> findAllUsersByUsername(String username);

    @EntityGraph(type = EntityGraph.EntityGraphType.FETCH, attributePaths = "roles")
    List<User> findAllUsersByUsernameContaining(String usernamePart);

    @EntityGraph(type = EntityGraph.EntityGraphType.FETCH, attributePaths = "roles")
    Optional<User> findUserByUsername(String username);

    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);

    /* ✅ Оставляем только для обновления telegram chat id*/
    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE User u SET u.telegramChatId = :telegramChatId WHERE u.username = :username")
    void updateTelegramChatId(@Param("username") String username,
                              @Param("telegramChatId") Long telegramChatId);

    /* ✅ Оставляем для обновления email (если нужно)*/
    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE User u SET u.email = :email WHERE u.id = :id")
    void updateEmail(@Param("id") Long id, @Param("email") String email);

    User findByTelegramChatId(Long chatId);
}