package org.example.nanobananaprobot.bot;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nanobananaprobot.bot.handlers.CallbackHandler;
import org.example.nanobananaprobot.bot.handlers.MessageHandler;

import org.example.nanobananaprobot.bot.service.TelegramService;
import org.example.nanobananaprobot.bot.service.UserStateManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;


import java.nio.file.Files;
import java.util.ArrayList;
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

    /* ДОБАВИТЬ ЭТИ ПОЛЯ:*/

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

                /* ОБНОВИЛИ УСЛОВИЕ: проверяем И фото, И документ*/

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

    /* Вспомогательный метод для проверки, что документ - это изображение*/

    private boolean isImageDocument(Document doc) {
        if (doc == null || doc.getMimeType() == null) return false;
        String mime = doc.getMimeType();
        return mime.startsWith("image/"); /* image/jpeg, image/png и т.д.*/
    }

    /**
     * Обработка загрузки фото
     */

    private void handlePhotoUpload(Message message) {
        Long chatId = message.getChatId();
        String userState = userStateManager.getUserState(chatId);
        String mediaGroupId = message.getMediaGroupId();

        /* Разрешённые состояния для загрузки фото*/
        boolean isWaitingUserInput = UserStateManager.STATE_WAITING_USER_INPUT.equals(userState);
        boolean isWaitingMultipleUpload = UserStateManager.STATE_WAITING_MULTIPLE_IMAGES_UPLOAD.equals(userState);
        boolean isWaitingSettings = UserStateManager.STATE_WAITING_SETTINGS.equals(userState);

        if (!isWaitingUserInput && !isWaitingMultipleUpload && !isWaitingSettings) {
            telegramService.sendMessage(chatId, "❌ Я сейчас не ожидаю загрузку фото.");
            return;
        }

        try {
            List<PhotoSize> photos = message.getPhoto();
            PhotoSize largestPhoto = photos.stream()
                    .max(Comparator.comparing(PhotoSize::getFileSize))
                    .orElseThrow(() -> new RuntimeException("No photo found"));

            String fileId = largestPhoto.getFileId();
            org.telegram.telegrambots.meta.api.objects.File file = execute(
                    new org.telegram.telegrambots.meta.api.methods.GetFile(fileId)
            );
            java.io.File downloadedFile = downloadFile(file.getFilePath());
            byte[] photoBytes = Files.readAllBytes(downloadedFile.toPath());

            log.info("Получено фото: chatId={}, state={}, mediaGroupId={}, size={}KB",
                    chatId, userState, mediaGroupId, photoBytes.length / 1024);

            /* Режим ожидания ввода пользователя (первое фото)*/
            if (isWaitingUserInput && mediaGroupId == null) {
                userStateManager.saveUploadedImage(chatId, photoBytes);
                sendPhotoActionButtons(chatId);
                return;
            }

            /* Альбом или добавление фото для слияния*/
            if (isWaitingUserInput && mediaGroupId != null) {
                userStateManager.addImageToCollection(chatId, photoBytes, mediaGroupId);
                int count = userStateManager.getMultipleImages(chatId).size();
                if (count >= 2) {
                    sendMergeContinueButton(chatId, count);
                } else {
                    telegramService.sendMessage(chatId, "📸 Загружено " + count + " фото. Отправьте ещё (нужно минимум 2)");
                }
                return;
            }

            /* Добавление ещё фото при слиянии (по одному)*/
            if (isWaitingMultipleUpload) {
                userStateManager.addImageToCollection(chatId, photoBytes, null);
                int count = userStateManager.getMultipleImages(chatId).size();
                if (count >= 2) {
                    sendMergeContinueButton(chatId, count);
                } else {
                    telegramService.sendMessage(chatId, "📸 Отправьте ещё одно фото (нужно минимум 2)");
                }
                return;
            }

        } catch (Exception e) {
            log.error("Error handling photo upload", e);
            telegramService.sendMessage(chatId, "❌ Ошибка загрузки фото");
        }
    }

    @PreDestroy
    public void shutdown() {
        messageHandler.shutdown();
    }

    private void sendPhotoActionButtons(Long chatId) {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();

        InlineKeyboardButton editBtn = new InlineKeyboardButton();
        editBtn.setText("📝 Редактировать");
        editBtn.setCallbackData("edit_photo");

        InlineKeyboardButton addBtn = new InlineKeyboardButton();
        addBtn.setText("➕ Добавить ещё фото");
        addBtn.setCallbackData("add_more_photo");

        InlineKeyboardButton cancelBtn = new InlineKeyboardButton();
        cancelBtn.setText("❌ Отмена");
        cancelBtn.setCallbackData("cancel_photo");

        row.add(editBtn);
        row.add(addBtn);
        row.add(cancelBtn);
        rows.add(row);

        inlineKeyboard.setKeyboard(rows);

        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("📸 Фото получено. Что делаем дальше?");
        message.setReplyMarkup(inlineKeyboard);

        telegramService.sendMessage(message);
    }

    private void sendMergeContinueButton(Long chatId, int count) {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();

        InlineKeyboardButton continueBtn = new InlineKeyboardButton();
        continueBtn.setText("✅ Продолжить (" + count + " фото)");
        continueBtn.setCallbackData("merge_continue");

        InlineKeyboardButton cancelBtn = new InlineKeyboardButton();
        cancelBtn.setText("❌ Отмена");
        cancelBtn.setCallbackData("cancel_photo");

        row.add(continueBtn);
        row.add(cancelBtn);
        rows.add(row);

        inlineKeyboard.setKeyboard(rows);

        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("📸 Загружено " + count + " фото. Нажмите 'Продолжить', чтобы ввести описание слияния");
        message.setReplyMarkup(inlineKeyboard);

        telegramService.sendMessage(message);
    }

}
