package otp.api;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.Filter.Chain;
import otp.model.User;
import otp.model.UserRole;
import otp.util.HttpUtils;
import otp.util.TokenManager;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;


public class AuthFilter extends Filter {
    private static final Logger logger = Logger.getLogger(AuthFilter.class.getName());

    private final UserRole requiredRole;

    /**
     * @param requiredRole минимальная роль пользователя для доступа к ресурсу
     */
    public AuthFilter(UserRole requiredRole) {
        this.requiredRole = requiredRole;
    }

    @Override
    public String description() {
        return "Фильтр аутентификации и проверки роли (ROLE >= " + requiredRole + ")";
    }

    @Override
    public void doFilter(HttpExchange exchange, Chain chain) throws IOException {
        try {
            String authHeader = exchange.getRequestHeaders().getFirst("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                // Отсутствует заголовок или формат неверен
                HttpUtils.sendError(exchange, 401, "Missing or invalid Authorization header");
                return;
            }

            String token = authHeader.substring(7).trim();

            // Получаем пользователя по токену
            User user = TokenManager.getUser(token);

            if (user == null) {
                // Токен невалиден или истёк
                HttpUtils.sendError(exchange, 401, "Invalid or expired token");
                return;
            }

            // Проверка роли: лучше иметь метод сравнения в enum для большей гибкости
            if (!hasSufficientRole(user.getRole(), requiredRole)) {
                HttpUtils.sendError(exchange, 403, "Forbidden");
                return;
            }

            // Сохраняем пользователя в атрибуте для последующего использования в хендлерах
            exchange.setAttribute("user", user);

            // Передаём управление дальше
            chain.doFilter(exchange);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Ошибка в AuthFilter", e);
            HttpUtils.sendError(exchange, 500, "Internal server error");
        }
    }

    /**
     * Метод сравнивает роли пользователя и требуемую роль.
     * Использует ordinal(), но можно расширить для более сложной логики.
     *
     * @param userRole     роль пользователя
     * @param requiredRole минимальная требуемая роль
     * @return true, если роль пользователя >= требуемой роли
     */
    private boolean hasSufficientRole(UserRole userRole, UserRole requiredRole) {
        return userRole.ordinal() >= requiredRole.ordinal();
    }
}
