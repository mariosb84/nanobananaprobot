package org.example.nanobananaprobot.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class PaymentCreateRequest {
    private Amount amount; /* Изменяем с BigDecimal на Amount*/
    private String currency;
    private String description;
    private Map<String, String> metadata;
    private Confirmation confirmation;
    private boolean capture = true;
    private Receipt receipt; /* ДОБАВЛЯЕМ для боевого режима ЮKassa*/

    @Data
    public static class Amount {
        private String value; /* Строка в формате "299.00"*/
        private String currency;
    }

    @Data
    public static class Confirmation {
        private String type = "redirect";
        private String returnUrl;
    }

    /* ДОБАВЛЯЕМ класс Receipt для 54-ФЗ*/
    @Data
    public static class Receipt {
        @JsonProperty("customer")
        private Customer customer;

        @JsonProperty("items")
        private List<Item> items;
    }

    @Data
    public static class Customer {
        @JsonProperty("email")
        private String email; /* Email пользователя для чека*/
    }

    @Data
    public static class Item {
        @JsonProperty("description")
        private String description;

        @JsonProperty("quantity")
        private String quantity = "1";

        @JsonProperty("amount")
        private Amount amount;

        @JsonProperty("vat_code")
        private Integer vatCode = 1; /* 1 = НДС 20%*/
    }

}