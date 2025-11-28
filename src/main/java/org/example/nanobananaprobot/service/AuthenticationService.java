package org.example.nanobananaprobot.service;

import lombok.RequiredArgsConstructor;
import org.example.nanobananaprobot.domain.dto.SignInRequest;
import org.example.nanobananaprobot.domain.dto.SignUpRequest;
import org.example.nanobananaprobot.domain.model.Role;
import org.example.nanobananaprobot.domain.model.User;
import org.example.nanobananaprobot.errors.InvalidCredentialsException;
import org.example.nanobananaprobot.errors.UserAlreadyExistsException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserServiceData userService;
    /*private final PasswordEncoder passwordEncoder;*/
    /*private final AuthenticationManager authenticationManager;*/

    /**
     * Регистрация пользователя
     *
     * @param request данные пользователя
     * @return User
     */
    public Optional<User> signUp(SignUpRequest request) {

        /* Проверяем, не существует ли пользователь*/
        if (userService.findUserByUsername(request.getUsername()) != null) {
            throw new UserAlreadyExistsException("User already exists: " + request.getUsername());
           /* return Optional.empty(); */ /* пользователь уже существует*/
        }

        User user = User.builder()
                .username(request.getUsername())
                /*.password(passwordEncoder.encode(request.getPassword()))*/    /* убираем шифрование пароля для входа на сайт(храним в БД так как есть)*/
                .password(request.getPassword())
                .email(request.getEmail())
                .phone("+79210000000")
                .roles(List.of(Role.ROLE_USER))
                .build();

        return userService.add(user);
    }

    /**
     * Аутентификация пользователя
     *
     * @param request данные пользователя
     * @return User
     */

    /*public Optional<User>  signIn(SignInRequest request) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                request.getUsername(),
                request.getPassword()
        ));
        return Optional.ofNullable(userService.findUserByUsername(request.getUsername()));
    }*/

    public Optional<User> signIn(SignInRequest request) {                                      /*  ПРОСТАЯ проверка без Spring Security*/

        /* ПРОСТАЯ проверка без Spring Security*/

        User user = userService.findUserByUsername(request.getUsername());
        if (user != null && user.getPassword().equals(request.getPassword())) {
            return Optional.of(user);
        }
        throw new InvalidCredentialsException("Invalid User login/password !!! : " + request.getUsername());
        /*return Optional.empty();*/
    }

}


