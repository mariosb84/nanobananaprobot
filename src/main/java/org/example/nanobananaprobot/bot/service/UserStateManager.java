package org.example.nanobananaprobot.bot.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserStateManager {

    private final Map<Long, String> userStates = new ConcurrentHashMap<>();
    private final Map<Long, String> tempUsernames = new ConcurrentHashMap<>();
    private final Map<Long, String> tempEmails = new ConcurrentHashMap<>();
    private final Map<Long, String> tempPrompts = new ConcurrentHashMap<>(); /* ДЛЯ ХРАНЕНИЯ ПРОМПТОВ*/

    /* УПРОЩЕННЫЕ СОСТОЯНИЯ*/
    public static final String STATE_NONE = "NONE";
    public static final String STATE_WAITING_USERNAME = "WAITING_FOR_USERNAME";
    public static final String STATE_WAITING_PASSWORD = "WAITING_FOR_PASSWORD";
    public static final String STATE_REGISTER_USERNAME = "REGISTER_USERNAME";
    public static final String STATE_REGISTER_PASSWORD = "REGISTER_PASSWORD";
    public static final String STATE_REGISTER_EMAIL = "REGISTER_EMAIL";
    public static final String STATE_WAITING_PAYMENT_ID = "WAITING_FOR_PAYMENT_ID";
    public static final String STATE_AUTHORIZED_MAIN = "AUTHORIZED_MAIN_MENU";
    public static final String STATE_SUBSCRIPTION_MENU = "SUBSCRIPTION_MENU";

    /* НОВЫЕ СОСТОЯНИЯ ДЛЯ ГЕНЕРАЦИИ*/
    public static final String STATE_WAITING_IMAGE_PROMPT = "WAITING_IMAGE_PROMPT";
    public static final String STATE_WAITING_VIDEO_PROMPT = "WAITING_VIDEO_PROMPT";
    public static final String STATE_GENERATION_IN_PROGRESS = "GENERATION_IN_PROGRESS";

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

    public void clearUserData(Long chatId) {
        userStates.remove(chatId);
        tempUsernames.remove(chatId);
        tempEmails.remove(chatId);
        tempPrompts.remove(chatId);
    }

}
