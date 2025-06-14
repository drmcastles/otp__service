package otp.api;

import otp.util.JwtUtils;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


@WebFilter("/api/*")  // Применяется ко всем запросам с префиксом /api
public class JwtAuthFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Можно добавить инициализацию, если потребуется
    }

    @Override
    public void doFilter(javax.servlet.ServletRequest request,
                         javax.servlet.ServletResponse response,
                         FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Извлекаем заголовок Authorization
        String authHeader = httpRequest.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7); // Убираем "Bearer "
            if (JwtUtils.validateToken(token)) {
                // Валидация успешна — можно извлечь данные пользователя
                String username = JwtUtils.extractUsername(token);
                httpRequest.setAttribute("username", username);

                // Продолжаем цепочку фильтров
                chain.doFilter(request, response);
            } else {
                // Токен невалиден — возвращаем 401 с сообщением
                httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token");
            }
        } else {
            // Токен отсутствует или формат неверный — 401 с сообщением
            httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing or invalid Authorization header");
        }
    }

    @Override
    public void destroy() {
        // Очистка ресурсов при завершении работы фильтра (если нужно)
    }
}
