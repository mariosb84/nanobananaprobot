package org.example.nanobananaprobot.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SignUpRequest {

    /*@Size(min = 5, max = 50, message = "Имя пользователя должно содержать от 5 до 50 символов")*/
    @NotBlank(message = "Имя пользователя не может быть пустыми")
    private String username;

    /*@Size(min = 8, max = 255, message = "Длина пароля должна быть от 8 до 255 символов")*/
    private String password;

    @NotNull(message = "Email must be non null")
    private String email;

}
