package org.example.nanobananaprobot.bot.service;

import org.example.nanobananaprobot.domain.dto.ImageConfig;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserStateManager {

    private final Map<Long, String> userStates = new ConcurrentHashMap<>();
    private final Map<Long, String> tempUsernames = new ConcurrentHashMap<>();
    private final Map<Long, String> tempEmails = new ConcurrentHashMap<>();
    private final Map<Long, String> tempPrompts = new ConcurrentHashMap<>(); /* ДЛЯ ХРАНЕНИЯ ПРОМПТОВ*/

    /* Хранилище для временных данных*/

    private final Map<Long, ImageConfig> userConfigs = new ConcurrentHashMap<>();
    private final Map<Long, byte[]> userUploadedImages = new ConcurrentHashMap<>();

    /* Хранилище для нескольких изображений*/

    private final Map<Long, List<byte[]>> userMultipleImages = new ConcurrentHashMap<>();
    private final Map<Long, String> mediaGroupIds = new ConcurrentHashMap<>();

    /* УПРОЩЕННЫЕ СОСТОЯНИЯ*/

    public static final String STATE_NONE = "NONE";
    public static final String STATE_WAITING_USERNAME = "WAITING_FOR_USERNAME";
    public static final String STATE_WAITING_PASSWORD = "WAITING_FOR_PASSWORD";
    public static final String STATE_REGISTER_USERNAME = "REGISTER_USERNAME";
    public static final String STATE_REGISTER_PASSWORD = "REGISTER_PASSWORD";
    public static final String STATE_REGISTER_EMAIL = "REGISTER_EMAIL";
    public static final String STATE_WAITING_PAYMENT_ID = "WAITING_FOR_PAYMENT_ID";
    public static final String STATE_AUTHORIZED_MAIN = "AUTHORIZED_MAIN_MENU";

    /* НОВЫЕ СОСТОЯНИЯ ДЛЯ ГЕНЕРАЦИИ*/

    public static final String STATE_WAITING_IMAGE_PROMPT = "WAITING_IMAGE_PROMPT";
    public static final String STATE_WAITING_VIDEO_PROMPT = "WAITING_VIDEO_PROMPT";
    public static final String STATE_GENERATION_IN_PROGRESS = "GENERATION_IN_PROGRESS";

    /* СОСТОЯНИЯ ДЛЯ ПОКУПКИ ПАКЕТОВ*/

    public static final String STATE_WAITING_PACKAGE_TYPE = "WAITING_PACKAGE_TYPE";
    public static final String STATE_WAITING_IMAGE_PACKAGE = "WAITING_IMAGE_PACKAGE";
    public static final String STATE_WAITING_VIDEO_PACKAGE = "WAITING_VIDEO_PACKAGE";

    public static final String STATE_WAITING_TEST_PROMPT = "WAITING_TEST_PROMPT";

    /* Новые состояния для загрузки изображений и настроек*/

    public static final String STATE_WAITING_IMAGE_UPLOAD = "WAITING_IMAGE_UPLOAD";
    public static final String STATE_WAITING_EDIT_PROMPT = "WAITING_EDIT_PROMPT";
    public static final String STATE_WAITING_QUALITY_SETTINGS = "WAITING_QUALITY_SETTINGS";

    /* НОВЫЕ СОСТОЯНИЯ ДЛЯ СЛИЯНИЯ ИЗОБРАЖЕНИЙ*/

    public static final String STATE_WAITING_MULTIPLE_IMAGES_UPLOAD = "WAITING_MULTIPLE_IMAGES_UPLOAD";
    public static final String STATE_WAITING_MERGE_PROMPT = "WAITING_MERGE_PROMPT";

    public static final String STATE_WAITING_TOKEN_PACKAGE = "WAITING_TOKEN_PACKAGE"; // Новое состояние

    public String getUserState(Long chatId) {
        return userStates.getOrDefault(chatId, STATE_NONE);
    }

    public void setUserState(Long chatId, String state) {
        userStates.put(chatId, state);
    }

    public void removeUserState(Long chatId) {
        userStates.remove(chatId);
    }

    public String getTempUsername(Long chatId) {
        return tempUsernames.get(chatId);
    }

    public void setTempUsername(Long chatId, String username) {
        tempUsernames.put(chatId, username);
    }

    public void removeTempUsername(Long chatId) {
        tempUsernames.remove(chatId);
    }

    public String getTempEmail(Long chatId) {
        return tempEmails.get(chatId);
    }

    public void setTempEmail(Long chatId, String email) {
        tempEmails.put(chatId, email);
    }

    public void removeTempEmail(Long chatId) {
        tempEmails.remove(chatId);
    }

    /* МЕТОДЫ ДЛЯ ПРОМПТОВ*/

    public String getTempPrompt(Long chatId) {
        return tempPrompts.get(chatId);
    }

    public void setTempPrompt(Long chatId, String prompt) {
        tempPrompts.put(chatId, prompt);
    }

    public void removeTempPrompt(Long chatId) {
        tempPrompts.remove(chatId);
    }

   /* public void clearUserData(Long chatId) {
        userStates.remove(chatId);
        tempUsernames.remove(chatId);
        tempEmails.remove(chatId);
        tempPrompts.remove(chatId);
    }*/

    /**
     * Получить или создать конфигурацию для пользователя
     */
    public ImageConfig getOrCreateConfig(Long chatId) {
        return userConfigs.computeIfAbsent(chatId, k -> new ImageConfig());
    }

    /**
     * Сохранить конфигурацию пользователя
     */
    public void saveConfig(Long chatId, ImageConfig config) {
        userConfigs.put(chatId, config);
    }

    /**
     * Сохранить загруженное изображение
     */
    public void saveUploadedImage(Long chatId, byte[] imageBytes) {
        userUploadedImages.put(chatId, imageBytes);
    }

    /**
     * Получить загруженное изображение
     */
    public byte[] getUploadedImage(Long chatId) {
        return userUploadedImages.get(chatId);
    }

    /**
     * Очистить ВСЕ данные пользователя
     * УДАЛИТЬ аннотацию @Override и вызов super.clearUserData()
     */
    public void clearUserData(Long chatId) {
        // Очищаем ВСЕ данные
        userStates.remove(chatId);
        tempUsernames.remove(chatId);
        tempEmails.remove(chatId);
        tempPrompts.remove(chatId);
        userConfigs.remove(chatId);
        userUploadedImages.remove(chatId);
    }

    /* МЕТОДЫ ДЛЯ РАБОТЫ С НЕСКОЛЬКИМИ ИЗОБРАЖЕНИЯМИ*/

    public void saveMultipleImages(Long chatId, List<byte[]> images) {
        userMultipleImages.put(chatId, images);
    }

    public List<byte[]> getMultipleImages(Long chatId) {
        return userMultipleImages.get(chatId);
    }

    public void clearMultipleImages(Long chatId) {
        userMultipleImages.remove(chatId);
        mediaGroupIds.remove(chatId);
    }

    public void addImageToCollection(Long chatId, byte[] imageBytes, String mediaGroupId) {
        List<byte[]> images = userMultipleImages.computeIfAbsent(chatId, k -> new ArrayList<>());
        images.add(imageBytes);
        if (mediaGroupId != null) {
            mediaGroupIds.put(chatId, mediaGroupId);
        }
    }

    public boolean hasImagesForMerge(Long chatId) {
        List<byte[]> images = userMultipleImages.get(chatId);
        return images != null && images.size() >= 2;
    }

    public String getMediaGroupId(Long chatId) {
        return mediaGroupIds.get(chatId);
    }

}
