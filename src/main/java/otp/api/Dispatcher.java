package otp.api;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpContext;
import otp.model.UserRole;


public class Dispatcher {
    private final AuthController authController = new AuthController();
    private final UserController userController = new UserController();
    private final AdminController adminController = new AdminController();

    /**
     * Регистрация всех маршрутов и подключение фильтров аутентификации.
     *
     * @param server экземпляр HttpServer
     */
    public void registerRoutes(HttpServer server) {
        // Публичные маршруты — не требуют авторизации
        server.createContext("/register", authController::handleRegister);
        server.createContext("/login", authController::handleLogin);

        // Маршруты для пользователей с ролью USER
        HttpContext genCtx = server.createContext("/otp/generate", userController::generateOtp);
        genCtx.getFilters().add(new AuthFilter(UserRole.USER));

        HttpContext valCtx = server.createContext("/otp/validate", userController::validateOtp);
        valCtx.getFilters().add(new AuthFilter(UserRole.USER));

        // Маршруты для администраторов с ролью ADMIN
        HttpContext configCtx = server.createContext("/admin/config", adminController::updateOtpConfig);
        configCtx.getFilters().add(new AuthFilter(UserRole.ADMIN));

        HttpContext usersCtx = server.createContext("/admin/users", exchange -> {
            String method = exchange.getRequestMethod();
            switch (method.toUpperCase()) {
                case "GET":
                    adminController.listUsers(exchange);
                    break;
                case "DELETE":
                    adminController.deleteUser(exchange);
                    break;
                default:
                    exchange.sendResponseHeaders(405, -1); // Method Not Allowed
            }
        });
        usersCtx.getFilters().add(new AuthFilter(UserRole.ADMIN));
    }
}
