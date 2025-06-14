package otp.api;

import com.sun.net.httpserver.HttpExchange;
import otp.dao.impl.UserDaoImpl;
import otp.model.UserRole;
import otp.service.UserService;
import otp.util.JsonUtil;
import otp.util.HttpUtils;
import otp.util.JwtUtils;  // Импортируем JwtUtils (если используется где-то в сервисе)

import java.io.IOException;
import java.util.Map;

/**
 * Контроллер аутентификации и регистрации пользователей.
 * Публичный API для работы с пользователями:
 * <ul>
 *   <li>POST /register — регистрация нового пользователя</li>
 *   <li>POST /login    — аутентификация и выдача JWT токена</li>
 * </ul>
 */
public class AuthController {

    // Сервис для работы с пользователями
    private final UserService userService = new UserService(new UserDaoImpl());


    public void handleRegister(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            HttpUtils.sendError(exchange, 405, "Method Not Allowed");
            return;
        }

        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        if (contentType == null || !contentType.contains("application/json")) {
            HttpUtils.sendError(exchange, 415, "Content-Type must be application/json");
            return;
        }

        try {
            // Парсим JSON тело запроса в DTO RegisterRequest
            RegisterRequest req = JsonUtil.fromJson(exchange.getRequestBody(), RegisterRequest.class);

            // Проверка на существование администратора, если роль ADMIN
            if ("ADMIN".equalsIgnoreCase(req.role) && userService.adminExists()) {
                HttpUtils.sendError(exchange, 409, "Admin already exists");
                return;
            }

            // Регистрируем пользователя
            userService.register(req.username, req.password, UserRole.valueOf(req.role.toUpperCase()));

            // Успешно: возвращаем статус 201 Created без тела
            HttpUtils.sendEmptyResponse(exchange, 201);
        } catch (IllegalArgumentException | IllegalStateException e) {
            // Ошибка данных или нарушение логики регистрации
            HttpUtils.sendError(exchange, 409, e.getMessage());
        } catch (Exception e) {
            // Все остальные ошибки
            HttpUtils.sendError(exchange, 500, "Internal server error");
        }
    }

    public void handleLogin(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            HttpUtils.sendError(exchange, 405, "Method Not Allowed");
            return;
        }

        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        if (contentType == null || !contentType.contains("application/json")) {
            HttpUtils.sendError(exchange, 415, "Content-Type must be application/json");
            return;
        }

        try {
            // Парсим JSON тело запроса в DTO LoginRequest
            LoginRequest req = JsonUtil.fromJson(exchange.getRequestBody(), LoginRequest.class);

            // Логиним пользователя и получаем JWT токен
            String token = userService.login(req.username, req.password);

            if (token == null) {
                // Если токен не выдан, значит логин/пароль неверны
                HttpUtils.sendError(exchange, 401, "Unauthorized");
                return;
            }

            // Формируем JSON ответ с токеном
            String json = JsonUtil.toJson(Map.of("token", token));

            // Отправляем JSON с кодом 200 OK
            HttpUtils.sendJsonResponse(exchange, 200, json);
        } catch (IllegalArgumentException e) {
            HttpUtils.sendError(exchange, 401, e.getMessage());
        } catch (Exception e) {
            HttpUtils.sendError(exchange, 500, "Internal server error");
        }
    }

    /**
     * DTO для разбора JSON тела запроса регистрации.
     */
    private static class RegisterRequest {
        public String username;
        public String password;
        public String role;
    }

    /**
     * DTO для разбора JSON тела запроса логина.
     */
    private static class LoginRequest {
        public String username;
        public String password;
    }
}
