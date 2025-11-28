package org.example.nanobananaprobot.bot.handlers;

import org.telegram.telegrambots.meta.api.objects.CallbackQuery;

public interface CallbackHandler {
    void handleCallback(CallbackQuery callbackQuery);
}

