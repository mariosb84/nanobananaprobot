package org.example.nanobananaprobot.bot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nanobananaprobot.bot.keyboards.MenuFactory;
import org.example.nanobananaprobot.domain.dto.SignInRequest;
import org.example.nanobananaprobot.domain.dto.SignUpRequest;
import org.example.nanobananaprobot.domain.model.User;
import org.example.nanobananaprobot.errors.InvalidCredentialsException;
import org.example.nanobananaprobot.service.AuthenticationService;
import org.example.nanobananaprobot.service.GenerationBalanceService;
import org.example.nanobananaprobot.service.UserServiceData;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationService authenticationService;
    private final UserServiceData userService;
    private final UserStateManager stateManager;
    private final TelegramService telegramService;
    private final MenuFactory menuFactory;
    private final GenerationBalanceService balanceService; /* ЗАМЕНЯЕМ*/

    public void handleLoginCommand(Long chatId) {
        stateManager.setUserState(chatId, UserStateManager.STATE_WAITING_USERNAME);
        telegramService.sendMessage(chatId, "Введите логин:");
    }

    public void handleRegisterCommand(Long chatId) {
        User existingUser = userService.findByTelegramChatId(chatId);
        if (existingUser != null) {
            telegramService.sendMessage(chatId,
                    "❌ Вы уже зарегистрированы!\n\n" +
                            "📧 Ваш логин: " + existingUser.getUsername() + "\n" +
                            "🆔 Ваш ID: " + existingUser.getId() + "\n\n" +
                            "Используйте /login для входа в существующий аккаунт.");
            return;
        }

        stateManager.setUserState(chatId, UserStateManager.STATE_REGISTER_EMAIL);
        telegramService.sendMessage(chatId, "📧 Введите ваш email:");
    }

    public void handleEmailInput(Long chatId, String email) {
        if (!email.contains("@") || !email.contains(".")) {
            telegramService.sendMessage(chatId, "❌ Неверный формат email. Введите корректный email:");
            return;
        }

        stateManager.setTempEmail(chatId, email);
        stateManager.setUserState(chatId, UserStateManager.STATE_REGISTER_USERNAME);
        telegramService.sendMessage(chatId, "Введите ваш логин:");
    }

    public void handleUsernameInput(Long chatId, String username, boolean isRegistration) {
        if (username.length() < 3) {
            telegramService.sendMessage(chatId, "❌ Логин должен содержать минимум 3 символа:");
            return;
        }

        if (isRegistration && userService.findUserByUsername(username) != null) {
            telegramService.sendMessage(chatId, "❌ Пользователь уже существует. Введите другой логин:");
            return;
        }

        stateManager.setTempUsername(chatId, username);
        String nextState = isRegistration ? UserStateManager.STATE_REGISTER_PASSWORD : UserStateManager.STATE_WAITING_PASSWORD;
        stateManager.setUserState(chatId, nextState);
        telegramService.sendMessage(chatId, "Введите пароль:");
    }

    public void handlePasswordInput(Long chatId, String password, boolean isRegistration) {
        String username = stateManager.getTempUsername(chatId);
        String email = isRegistration ? stateManager.getTempEmail(chatId) : "";

        stateManager.removeTempUsername(chatId);
        if (isRegistration) {
            stateManager.removeTempEmail(chatId);
        }

        if (isRegistration) {
            handleRegistrationAndAutoLogin(chatId, username, password, email);
        } else {
            handleLogin(chatId, username, password);
        }
    }

    private void handleRegistrationAndAutoLogin(Long chatId, String username, String password, String email) {
        SignUpRequest request = new SignUpRequest();
        request.setUsername(username);
        request.setPassword(password);
        request.setEmail(email);

        if (authenticationService.signUp(request).isPresent()) {

            /* Добавляем 3 бесплатные генерации*/

            User user = userService.findUserByUsername(username);
            if (user != null) {

                log.info("Added 0 free generations for new user: {}", username);
            }

            SignInRequest loginRequest = new SignInRequest();
            loginRequest.setUsername(username);
            loginRequest.setPassword(password);

            Optional<User> loggedInUser = authenticationService.signIn(loginRequest);
            if (loggedInUser.isPresent()) {
                userService.updateTelegramChatId(username, chatId);
                stateManager.setUserState(chatId, UserStateManager.STATE_AUTHORIZED_MAIN);
                telegramService.sendMessage(chatId, "✅ Регистрация и авторизация успешны!");
                telegramService.sendMessage(chatId, "💰 Баланс: 0 токенов. Купите пакет в магазине!");

                CompletableFuture.delayedExecutor(500, TimeUnit.MILLISECONDS)
                        .execute(() -> {
                            telegramService.sendMessage(menuFactory.showMainMenuCompact(chatId));
                        });
            } else {
                telegramService.sendMessage(chatId, "❌ Регистрация успешна, но авторизация не удалась. Используйте /login");
                stateManager.removeUserState(chatId);
            }
        } else {
            telegramService.sendMessage(chatId, "❌ Ошибка регистрации.");
            stateManager.removeUserState(chatId);
        }
    }

    private void handleLogin(Long chatId, String username, String password) {
        SignInRequest request = new SignInRequest();
        request.setUsername(username);
        request.setPassword(password);

        try {
            Optional<User> user = authenticationService.signIn(request);
            if (user.isPresent()) {
                userService.updateTelegramChatId(username, chatId);
                stateManager.setUserState(chatId, UserStateManager.STATE_AUTHORIZED_MAIN);
                telegramService.sendMessage(chatId, "✅ Авторизация успешна!");
                telegramService.sendMessage(menuFactory.showMainMenuCompact(chatId));
            }
        } catch (InvalidCredentialsException e) {
            telegramService.sendMessage(chatId, "❌ Неверный логин или пароль. /login");
            stateManager.removeUserState(chatId);
        } catch (Exception e) {
            log.error("Login error for user: {}", username, e);
            telegramService.sendMessage(chatId, "❌ Произошла ошибка. Попробуйте еще раз.");
            stateManager.removeUserState(chatId);
        }
    }

    public void handleLogout(Long chatId) {
        try {
            stateManager.clearUserData(chatId);
            telegramService.sendMessage(chatId, "👋 До свидания! Для возобновления работы нажмите : /start");
        } catch (Exception e) {
            log.error("Error during logout for chatId: {}", chatId, e);
            stateManager.clearUserData(chatId);
            telegramService.sendMessage(chatId, "👋 Сессия завершена");
        }
    }

}
