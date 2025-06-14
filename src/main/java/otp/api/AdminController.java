package otp.api;

import com.sun.net.httpserver.HttpExchange;
import otp.dao.impl.OtpCodeDaoImpl;
import otp.dao.impl.OtpConfigDaoImpl;
import otp.dao.impl.UserDaoImpl;
import otp.model.User;
import otp.service.AdminService;
import otp.util.JsonUtil;
import otp.util.HttpUtils;

import java.io.IOException;
import java.net.URI;
import java.util.List;

/**
 * Контроллер для административных операций (роль ADMIN).
 *
 * <p>Доступные маршруты:
 * <ul>
 *   <li>PATCH  /admin/config     — изменить длину и время жизни OTP-кодов</li>
 *   <li>GET    /admin/users      — получить список всех пользователей без админов</li>
 *   <li>DELETE /admin/users/{id} — удалить пользователя и связанные OTP-коды</li>
 * </ul>
 * </p>
 */
public class AdminController {

    private final AdminService adminService = new AdminService(
            new OtpConfigDaoImpl(),
            new UserDaoImpl(),
            new OtpCodeDaoImpl()
    );


    public void updateOtpConfig(HttpExchange exchange) throws IOException {
        if (!"PATCH".equalsIgnoreCase(exchange.getRequestMethod())) {
            HttpUtils.sendError(exchange, 405, "Method Not Allowed");
            return;
        }

        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        if (contentType == null || !contentType.contains("application/json")) {
            HttpUtils.sendError(exchange, 415, "Content-Type must be application/json");
            return;
        }

        try {
            // Десериализация запроса в DTO
            ConfigRequest req = JsonUtil.fromJson(exchange.getRequestBody(), ConfigRequest.class);

            // Валидация входных данных
            if (req.length <= 0 || req.ttlSeconds <= 0) {
                HttpUtils.sendError(exchange, 400, "Length and ttlSeconds must be positive integers");
                return;
            }

            adminService.updateOtpConfig(req.length, req.ttlSeconds);

            HttpUtils.sendEmptyResponse(exchange, 204);
        } catch (IllegalArgumentException e) {
            HttpUtils.sendError(exchange, 400, e.getMessage());
        } catch (Exception e) {
            // Логирование исключения могло бы помочь в диагностике
            e.printStackTrace();
            HttpUtils.sendError(exchange, 500, "Internal server error");
        }
    }


    public void listUsers(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            HttpUtils.sendError(exchange, 405, "Method Not Allowed");
            return;
        }

        try {
            List<User> users = adminService.getAllUsersWithoutAdmins();

            String json = JsonUtil.toJson(users);

            HttpUtils.sendJsonResponse(exchange, 200, json);
        } catch (Exception e) {
            e.printStackTrace();
            HttpUtils.sendError(exchange, 500, "Internal server error");
        }
    }


    public void deleteUser(HttpExchange exchange) throws IOException {
        if (!"DELETE".equalsIgnoreCase(exchange.getRequestMethod())) {
            HttpUtils.sendError(exchange, 405, "Method Not Allowed");
            return;
        }

        try {
            URI uri = exchange.getRequestURI();

            String[] segments = uri.getPath().split("/");

            // Лучше проверить, что сегмент с ID существует
            if (segments.length == 0) {
                HttpUtils.sendError(exchange, 400, "User ID not provided");
                return;
            }

            Long id = Long.valueOf(segments[segments.length - 1]);

            adminService.deleteUserAndCodes(id);

            HttpUtils.sendEmptyResponse(exchange, 204);
        } catch (NumberFormatException e) {
            HttpUtils.sendError(exchange, 400, "Invalid user ID");
        } catch (IllegalArgumentException e) {
            HttpUtils.sendError(exchange, 404, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            HttpUtils.sendError(exchange, 500, "Internal server error");
        }
    }

    /**
     * DTO для разбора JSON тела PATCH запроса /admin/config.
     */
    private static class ConfigRequest {
        public int length;
        public int ttlSeconds;
    }
}
