package org.example.nanobananaprobot.bot.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.DefaultAbsSender;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.ByteArrayInputStream;
import java.util.List;

@Slf4j
@Service
public class TelegramService extends DefaultAbsSender {

    @Value("${tg.token}")
    private String botToken;

    public TelegramService() {
        super(new DefaultBotOptions());
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    public void sendMessage(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error sending message: {}", e.getMessage());
        }
    }

    public void sendMessage(SendMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error sending message: {}", e.getMessage());
        }
    }

    public void answerCallback(AnswerCallbackQuery answer) {
        try {
            execute(answer);
        } catch (TelegramApiException e) {
            log.error("Error answering callback: {}", e.getMessage());
        }
    }

    public void sendPhoto(Long chatId, byte[] photoBytes, String fileName) {
        try {
            SendPhoto sendPhoto = new SendPhoto();
            sendPhoto.setChatId(chatId.toString());
            InputFile inputFile = new InputFile(new ByteArrayInputStream(photoBytes), fileName);
            sendPhoto.setPhoto(inputFile);
            // Используем this.execute(), а не telegramBot.execute()
            execute(sendPhoto); // <- Ключевое исправление
        } catch (TelegramApiException e) {
            log.error("Ошибка отправки фото в чат {}", chatId, e);
            throw new RuntimeException("Не удалось отправить фото", e);
        }
    }

    public void sendMediaGroup(Long chatId, List<byte[]> imagesBytes, List<String> fileNames) {
        // Реализация для отправки альбома
    }

}


