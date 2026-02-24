package org.example.nanobananaprobot.bot.service;

import lombok.extern.slf4j.Slf4j;
import org.example.nanobananaprobot.domain.dto.ImageConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.DefaultAbsSender;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
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

    public void sendMediaGroup(Long chatId, List<byte[]> imagesBytes, List<String> fileNames) {

        /* Реализация для отправки альбома*/
    }

    /**
     * Отправка фото (для изображений до 10MB)
     */
    public void sendPhoto(Long chatId, byte[] photoBytes, String fileName) {
        try {
            SendPhoto sendPhoto = new SendPhoto();
            sendPhoto.setChatId(chatId.toString());
            sendPhoto.setPhoto(new InputFile(new ByteArrayInputStream(photoBytes), fileName));

            execute(sendPhoto);
        } catch (Exception e) {
            log.error("Ошибка отправки фото в чат {}", chatId, e);
            throw new RuntimeException("Не удалось отправить фото", e);
        }
    }

    /**
     * ★ УЛУЧШЕННЫЙ МЕТОД: Отправка документа с повторными попытками
     */
    public void sendDocument(Long chatId, byte[] fileBytes, String fileName, String caption) {
        int maxRetries = 3;
        int retryDelay = 2000; /* 2 секунды между попытками*/

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                log.info("Попытка отправки документа {}/{} ({} MB)...",
                        attempt, maxRetries, fileBytes.length / 1024 / 1024);

                SendDocument sendDoc = new SendDocument();
                sendDoc.setChatId(chatId.toString());
                sendDoc.setDocument(new InputFile(new ByteArrayInputStream(fileBytes), fileName));

                if (caption != null && !caption.isEmpty()) {
                    sendDoc.setCaption(caption);
                }

                /* ★ В библиотеке нет setTimeout, устанавливаем через BotOptions
                 sendDoc.setTimeout(120);  УДАЛИТЬ ЭТУ СТРОКУ*/

                execute(sendDoc);
                log.info("✅ Документ успешно отправлен");
                return;

            } catch (Exception e) {
                log.error("❌ Ошибка отправки документа (попытка {}/{}): {}",
                        attempt, maxRetries, e.getMessage());

                if (attempt == maxRetries) {
                    log.error("Не удалось отправить документ после {} попыток", maxRetries);
                    throw new RuntimeException("Не удалось отправить документ", e);
                }

                /* Ждем перед повторной попыткой*/

                try {
                    Thread.sleep(retryDelay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Прервано ожидание", ie);
                }

                /* Увеличиваем задержку для следующей попытки*/

                retryDelay *= 2;
            }
        }
    }

    /**
     * ★ УМНЫЙ МЕТОД: 4K всегда как документ, остальное по ситуации
     */
    public void sendImageSmart(Long chatId, byte[] imageBytes, String fileName, ImageConfig config) {
        long sizeMB = imageBytes.length / 1024 / 1024;

        /* 4K всегда отправляем как документ (Telegram не сжимает)*/

        if ("4K".equals(config.getResolution())) {
            log.info("4K изображение ({} MB) - отправляю как документ", sizeMB);

            String caption = String.format(
                    "🎨 %s | %s\n📦 Размер: %d MB\n🔗 Оригинальное 4K качество",
                    config.getAspectRatio(),
                    config.getResolution(),
                    sizeMB
            );

            String docName = String.format("4k_%s_%s.jpg",
                    config.getAspectRatio().replace(":", "x"),
                    System.currentTimeMillis());

            sendDocument(chatId, imageBytes, docName, caption);
            return;
        }

        /* Для 1K и 2K - старая логика*/

        if (imageBytes.length > 10 * 1024 * 1024) { // >10MB
            log.info("Изображение слишком большое для фото ({} MB)", sizeMB);

            try {

                /* Пробуем отправить как документ*/

                String caption = String.format(
                        "🎨 %s | %s\n📦 Размер: %d MB\n🔗 Отправлено как документ",
                        config.getAspectRatio(),
                        config.getResolution(),
                        sizeMB
                );

                String docName = String.format("image_%s_%s.jpg",
                        config.getAspectRatio().replace(":", "x"),
                        config.getResolution().toLowerCase());

                sendDocument(chatId, imageBytes, docName, caption);

            } catch (Exception e) {
                log.warn("Не удалось отправить как документ, пробуем сжать...");

                try {

                    /* Fallback: сжимаем*/

                    long targetSize = 9_500_000L;
                    byte[] compressed = smartCompressToSize(imageBytes, targetSize);
                    log.info("Сжато до {} MB", compressed.length / 1024 / 1024);

                    sendMessage(chatId,
                            "⚠️ Изображение было сжато для отправки в Telegram\n" +
                                    "🎨 " + config.getAspectRatio() + " | " + config.getResolution()
                    );

                    sendPhoto(chatId, compressed, fileName);

                } catch (Exception ex) {
                    log.error("Не удалось даже сжать изображение", ex);
                    throw new RuntimeException("Не удалось отправить изображение", ex);
                }
            }

        } else {
            log.info("Отправляю как фото ({} MB)", sizeMB);
            sendPhoto(chatId, imageBytes, fileName);
        }
    }

    /**
     * Сжатие до целевого размера с минимальной потерей качества
     */
    private byte[] smartCompressToSize(byte[] originalBytes, long targetSize) throws IOException {
        ByteArrayInputStream bis = new ByteArrayInputStream(originalBytes);
        BufferedImage image = ImageIO.read(bis);

        /* Начинаем с высокого качества*/

        float quality = 0.95f;
        byte[] result = null;

        while (quality > 0.5f) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(quality);

            writer.setOutput(ImageIO.createImageOutputStream(baos));
            writer.write(null, new IIOImage(image, null, null), param);
            writer.dispose();

            byte[] compressed = baos.toByteArray();

            if (compressed.length <= targetSize) {
                result = compressed;
                log.info("Найдено качество {} -> {} bytes", quality, compressed.length);
                break;
            }

            quality -= 0.05f; /* Уменьшаем качество на 5%*/
        }

        if (result == null) {

            /* Если не удалось - возвращаем максимально сжатое*/

            return compressImage(image, 0.5f);
        }

        return result;
    }

    private byte[] compressImage(BufferedImage image, float quality) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(quality);

        writer.setOutput(ImageIO.createImageOutputStream(baos));
        writer.write(null, new IIOImage(image, null, null), param);
        writer.dispose();

        return baos.toByteArray();
    }

    public void sendPromptExample(Long chatId, String imageName, String caption) {
        try {
            InputStream inputStream = getClass().getClassLoader()
                    .getResourceAsStream("examples/" + imageName);

            if (inputStream == null) {
                log.error("Image not found: examples/{}", imageName);
                sendMessage(chatId, caption);
                return;
            }

            InputFile photo = new InputFile(inputStream, imageName);

            SendPhoto sendPhoto = new SendPhoto();
            sendPhoto.setChatId(chatId.toString());
            sendPhoto.setPhoto(photo);
            sendPhoto.setCaption(caption);
            sendPhoto.setParseMode("Markdown");

            execute(sendPhoto);
            inputStream.close();

        } catch (Exception e) {
            log.error("Error sending prompt example: {}", e.getMessage());
            sendMessage(chatId, caption);
        }
    }

}


