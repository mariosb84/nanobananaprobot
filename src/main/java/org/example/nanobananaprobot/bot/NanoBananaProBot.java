package org.example.nanobananaprobot.bot;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nanobananaprobot.bot.handlers.CallbackHandler;
import org.example.nanobananaprobot.bot.handlers.MessageHandler;
import org.example.nanobananaprobot.bot.handlers.MessageHandlerImpl;
import org.example.nanobananaprobot.bot.service.TelegramService;
import org.example.nanobananaprobot.bot.service.UserStateManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.nio.file.Files;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class NanoBananaProBot extends TelegramLongPollingBot {

    @Value("${tg.username}")
    private String username;

    @Value("${tg.token}")
    private String token;

    private final MessageHandler messageHandler;
    private final CallbackHandler callbackHandler;

    // ДОБАВИТЬ ЭТИ ПОЛЯ:
    private final TelegramService telegramService;
    private final UserStateManager userStateManager;

    @Override
    public String getBotUsername() {
        return username;
    }

    @Override
    public String getBotToken() {
        return token;
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (update.hasCallbackQuery()) {
                callbackHandler.handleCallback(update.getCallbackQuery());
            } else if (update.hasMessage()) {
                Message message = update.getMessage();

                // ОБНОВИЛИ УСЛОВИЕ: проверяем И фото, И документ
                if (message.hasPhoto() || (message.hasDocument() && isImageDocument(message.getDocument()))) {
                    handlePhotoUpload(message);
                }
                else if (message.hasText() && message.getText() != null) {
                    messageHandler.handleTextMessage(message);
                }
                else {
                    log.debug("Received unsupported message type, chatId: {}", message.getChatId());
                }
            }
        } catch (Exception e) {
            log.error("Error processing update: {}", e.getMessage());
            messageHandler.handleError(update, e);
        }
    }

    // Вспомогательный метод для проверки, что документ - это изображение
    private boolean isImageDocument(Document doc) {
        if (doc == null || doc.getMimeType() == null) return false;
        String mime = doc.getMimeType();
        return mime.startsWith("image/"); // image/jpeg, image/png и т.д.
    }

    /**
     * Обработка загрузки фото
     */
    private void handlePhotoUpload(Message message) {
        Long chatId = message.getChatId();

        // ДОБАВЬТЕ ПРОВЕРКУ АВТОРИЗАЦИИ:
        if (!messageHandler.isUserAuthorized(chatId)) {
            telegramService.sendMessage(chatId, "❌ Пожалуйста, авторизуйтесь: /login");
            return;
        }

        // Используем напрямую внедренный userStateManager
        String userState = userStateManager.getUserState(chatId);

        // Проверяем, ожидаем ли мы загрузку фото
        if (!UserStateManager.STATE_WAITING_IMAGE_UPLOAD.equals(userState)) {
            telegramService.sendMessage(chatId,
                    "❌ Я сейчас не ожидаю загрузку фото.\n" +
                            "Используйте команду /edit для начала редактирования."
            );
            return;
        }

        try {
            // Получаем самое большое фото из массива
            List<PhotoSize> photos = message.getPhoto();
            PhotoSize largestPhoto = photos.stream()
                    .max(Comparator.comparing(PhotoSize::getFileSize))
                    .orElseThrow(() -> new RuntimeException("No photo found"));

            // Получаем fileId и скачиваем файл ПРАВИЛЬНЫМ способом
            String fileId = largestPhoto.getFileId();

            // Получаем объект файла от Telegram API
            org.telegram.telegrambots.meta.api.objects.File file = execute(
                    new org.telegram.telegrambots.meta.api.methods.GetFile(fileId)
            );

            // Получаем путь к файлу
            String filePath = file.getFilePath();

            // Скачиваем файл по пути
            java.io.File downloadedFile = downloadFile(filePath);

            // Читаем фото в byte[]
            byte[] photoBytes = Files.readAllBytes(downloadedFile.toPath());

            // Сохраняем в stateManager
            userStateManager.saveUploadedImage(chatId, photoBytes);
            userStateManager.setUserState(chatId, UserStateManager.STATE_WAITING_EDIT_PROMPT);

            // Уведомляем пользователя
            telegramService.sendMessage(chatId,
                    "✅ Фото загружено!\n\n" +
                            "📷 Размер: " + (photoBytes.length / 1024) + " KB\n" +
                            "✏️ Теперь введите текстовое описание изменений:\n\n" +
                            "Пример: 'Сделай фон космическим, добавь летающие планеты'"
            );

        } catch (TelegramApiException e) {
            log.error("Telegram API error handling photo upload for chatId: {}", chatId, e);
            telegramService.sendMessage(chatId,
                    "❌ Ошибка Telegram API при загрузке фото.\n" +
                            "Попробуйте отправить фото еще раз."
            );
        } catch (Exception e) {
            log.error("Error handling photo upload for chatId: {}", chatId, e);
            telegramService.sendMessage(chatId,
                    "❌ Ошибка при загрузке фото. Попробуйте снова.\n" +
                            "Убедитесь, что фото не слишком большое."
            );
        }
    }

    @PreDestroy
    public void shutdown() {
        messageHandler.shutdown();
    }

}
