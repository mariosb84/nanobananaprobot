package org.example.nanobananaprobot.bot.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentInfo {
    private String paymentId;
    private String packageType; /* "image" или "video"*/
    private String count;       /* "3", "10", "50" и т.д.*/
    private String price;       /* "39", "99" и т.д.*/
    private Long chatId;
}