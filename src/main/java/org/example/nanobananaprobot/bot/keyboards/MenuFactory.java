package org.example.nanobananaprobot.bot.keyboards;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

public interface MenuFactory {
    SendMessage createWelcomeMenu(Long chatId);
    SendMessage createMainMenu(Long chatId);
    SendMessage createMainMenu(Long chatId, boolean afterGeneration);
    SendMessage createSubscriptionMenu(Long chatId);
    SendMessage createInfoMenu(Long chatId);
    SendMessage createContactsMenu(Long chatId);
    SendMessage createStatsMenu(Long chatId); // НОВЫЙ МЕТОД
}