package org.example.nanobananaprobot.bot;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nanobananaprobot.bot.handlers.CallbackHandler;
import org.example.nanobananaprobot.bot.handlers.MessageHandler;
/*import org.example.nanobananaprobot.bot.handlers.MessageHandlerImpl;*/
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
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
/*import org.telegram.telegrambots.meta.exceptions.TelegramApiException;*/

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
    /*private void handlePhotoUpload(Message message) {
        Long chatId = message.getChatId();

        if (!messageHandler.isUserAuthorized(chatId)) {
            telegramService.sendMessage(chatId, "❌ Пожалуйста, авторизуйтесь: /login");
            return;
        }

        String userState = userStateManager.getUserState(chatId);
        String mediaGroupId = message.getMediaGroupId();

        boolean isWaitingSingleUpload = UserStateManager.STATE_WAITING_IMAGE_UPLOAD.equals(userState);
        boolean isWaitingMultipleUpload = UserStateManager.STATE_WAITING_MULTIPLE_IMAGES_UPLOAD.equals(userState);
        boolean isWaitingUserInput = UserStateManager.STATE_WAITING_USER_INPUT.equals(userState); // ← ДОЛЖНО БЫТЬ ДО if

        log.info("handlePhotoUpload: userState={}, isWaitingUserInput={}", userState, isWaitingUserInput);
        log.info("userState={}", userState);

        if (!isWaitingSingleUpload && !isWaitingMultipleUpload && !isWaitingUserInput
                && !UserStateManager.STATE_WAITING_SETTINGS.equals(userState)) {
            telegramService.sendMessage(chatId,
                    "❌ Я сейчас не ожидаю загрузку фото.\n" +
                            "Используйте /edit для редактирования или /merge для объединения."
            );
            return;
        }

        try {

            *//* Получаем самое большое фото из массива*//*
            List<PhotoSize> photos = message.getPhoto();
            PhotoSize largestPhoto = photos.stream()
                    .max(Comparator.comparing(PhotoSize::getFileSize))
                    .orElseThrow(() -> new RuntimeException("No photo found"));

            *//* Скачиваем фото*//*
            String fileId = largestPhoto.getFileId();
            org.telegram.telegrambots.meta.api.objects.File file = execute(
                    new org.telegram.telegrambots.meta.api.methods.GetFile(fileId)
            );
            java.io.File downloadedFile = downloadFile(file.getFilePath());
            byte[] photoBytes = Files.readAllBytes(downloadedFile.toPath());

            log.info("Получено фото: chatId={}, state={}, mediaGroupId={}, size={}KB",
                    chatId, userState, mediaGroupId, photoBytes.length / 1024);

            if (isWaitingSingleUpload) {

                *//* Для редактирования одного фото*//*
                if (mediaGroupId != null) {
                    telegramService.sendMessage(chatId,
                            "📸 Получено несколько фото в альбоме.\n" +
                                    "⚠️ Для редактирования используется только первое фото.\n" +
                                    "✏️ Для объединения используйте команду /merge"
                    );
                }

                userStateManager.saveUploadedImage(chatId, photoBytes);
                userStateManager.setUserState(chatId, UserStateManager.STATE_WAITING_EDIT_PROMPT);

                telegramService.sendMessage(chatId,
                        "✅ Фото загружено!\n\n" +
                                "📷 Размер: " + (photoBytes.length / 1024) + " KB\n" +
                                "✏️ Теперь введите текстовое описание изменений:\n\n" +
                                "Пример: 'Сделай фон космическим'"
                );

            } else if (isWaitingMultipleUpload) {

                *//* Для объединения нескольких фото*//*
                userStateManager.addImageToCollection(chatId, photoBytes, mediaGroupId);
                int count = userStateManager.getMultipleImages(chatId).size();

                *//* СОЗДАЕМ СООБЩЕНИЕ С КНОПКОЙ*//*
                SendMessage responseMessage = new SendMessage();
                responseMessage.setChatId(chatId.toString());

                if (mediaGroupId != null) {
                    responseMessage.setText("✅ Загружено фото " + count + " из альбома\n" +
                            "📸 Можно отправить еще фото или нажать кнопку чтобы продолжить:");
                } else {
                    responseMessage.setText("✅ Загружено фото: " + count + "\n\n" +
                            "📸 Можно отправить еще фото или нажать кнопку чтобы продолжить:");
                }

                *//* СОЗДАЕМ КЛАВИАТУРУ С КНОПКОЙ*//*
                ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
                keyboard.setResizeKeyboard(true);
                keyboard.setOneTimeKeyboard(true); *//* Клавиатура скроется после нажатия*//*

                List<KeyboardRow> rows = new ArrayList<>();
                KeyboardRow buttonRow = new KeyboardRow();

                *//* ЕСЛИ ЕСТЬ МИНИМУМ 2 ФОТО - ПОКАЗЫВАЕМ КНОПКУ "ВВЕСТИ ПРОМПТ"*//*
                if (count >= 2) {
                    buttonRow.add(new KeyboardButton("✅ Все фото загружены, ввести промпт"));
                }

                *//* КНОПКА "ОТМЕНА" ДЛЯ ВЫХОДА ИЗ РЕЖИМА*//*
                KeyboardRow cancelRow = new KeyboardRow();
                cancelRow.add(new KeyboardButton("❌ Отмена слияния"));

                *//* КНОПКА "ГЛАВНОЕ МЕНЮ"*//*
                KeyboardRow mainMenuRow = new KeyboardRow();
                mainMenuRow.add(new KeyboardButton("🏠 Главное меню"));

                if (!buttonRow.isEmpty()) {
                    rows.add(buttonRow);
                }
                rows.add(cancelRow);
                rows.add(mainMenuRow);

                keyboard.setKeyboard(rows);
                responseMessage.setReplyMarkup(keyboard);

                telegramService.sendMessage(responseMessage);

            } else if (isWaitingUserInput) {
                if (mediaGroupId == null) {
                    // Одно фото — показываем inline-кнопки выбора
                    userStateManager.saveUploadedImage(chatId, photoBytes);
                    sendPhotoActionButtons(chatId);
                } else {
                    // Альбом (2+ фото) — сразу в коллекцию для слияния
                    userStateManager.addImageToCollection(chatId, photoBytes, mediaGroupId);
                    int count = userStateManager.getMultipleImages(chatId).size();
                    if (count >= 2) {
                        // Показать кнопку "Продолжить" для слияния
                        sendMergeContinueButton(chatId, count);
                    } else {
                        telegramService.sendMessage(chatId, "📸 Загружено " + count + " фото. Отправьте ещё фото (нужно минимум 2)");
                    }
                }
            }

        } catch (Exception e) {
            log.error("Error handling photo upload for chatId: {}", chatId, e);
            telegramService.sendMessage(chatId, "❌ Ошибка при загрузке фото. Попробуйте снова.");
        }
    }*/

    private void handlePhotoUpload(Message message) {
        Long chatId = message.getChatId();
        String userState = userStateManager.getUserState(chatId);
        String mediaGroupId = message.getMediaGroupId();

        // Разрешённые состояния для загрузки фото
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

            // Режим ожидания ввода пользователя (первое фото)
            if (isWaitingUserInput && mediaGroupId == null) {
                userStateManager.saveUploadedImage(chatId, photoBytes);
                sendPhotoActionButtons(chatId);
                return;
            }

            // Альбом или добавление фото для слияния
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

            // Добавление ещё фото при слиянии (по одному)
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
