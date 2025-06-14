package otp.main;

import com.sun.net.httpserver.HttpServer;
import otp.api.Dispatcher;
import org.flywaydb.core.Flyway;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.Properties;

/**
 * Точка входа приложения. Поднимает HTTP-сервер на порту из application.properties,
 * регистрирует маршруты через Dispatcher, и запускает миграции Flyway.
 */
public class Application {
    public static void main(String[] args) {
        try {
            // Загружаем конфигурацию
            Properties config = new Properties();
            try (InputStream is = Application.class.getClassLoader()
                    .getResourceAsStream("application.properties")) {
                if (is != null) {
                    config.load(is);
                } else {
                    System.err.println("Не найден файл application.properties");
                    System.exit(1);
                }
            }

            // Запускаем миграции Flyway
            String dbUrl = config.getProperty("db.url");
            String dbUser = config.getProperty("db.user");
            String dbPassword = config.getProperty("db.password");

            Flyway flyway = Flyway.configure()
                    .dataSource(dbUrl, dbUser, dbPassword)
                    .load();

            flyway.migrate();

            // Читаем порт сервера
            int port = Integer.parseInt(config.getProperty("server.port", "8080"));

            // Создаём HTTP-сервер
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

            // Регистрируем маршруты
            Dispatcher dispatcher = new Dispatcher();
            dispatcher.registerRoutes(server);

            // Запускаем сервер
            server.start();
            System.out.println("Server started on http://localhost:" + port);
        } catch (IOException e) {
            System.err.println("Failed to start server: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
