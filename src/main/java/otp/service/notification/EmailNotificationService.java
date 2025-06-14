package otp.service.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.*;
import javax.mail.internet.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Реализация NotificationService для отправки OTP-кодов по Email.
 * Конфигурация берётся из файла email.properties в resources.
 */
public class EmailNotificationService implements NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(EmailNotificationService.class);

    private final Session session;
    private final String fromAddress;

    /**
     * Конструктор загружает настройки почты и инициирует JavaMail Session.
     */
    public EmailNotificationService() {
        Properties props = loadConfig();
        this.fromAddress = props.getProperty("email.from");

        // Включаем логирование JavaMail из свойств (mail.debug=true)
        if ("true".equalsIgnoreCase(props.getProperty("mail.debug"))) {
            System.setProperty("mail.debug", "true");
            logger.info("JavaMail debug enabled");
        }

        this.session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                String user = props.getProperty("email.username");
                String pass = props.getProperty("email.password");
                logger.info("Authenticating mail session with user: {}", user);
                return new PasswordAuthentication(user, pass);
            }
        });
    }

    /**
     * Загрузка конфигурации из файла email.properties.
     *
     * @return Properties с настройками SMTP.
     */
    private Properties loadConfig() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("email.properties")) {
            if (is == null) {
                logger.error("email.properties not found in classpath");
                throw new IllegalStateException("email.properties not found in classpath");
            }
            Properties props = new Properties();
            props.load(is);
            logger.info("Loaded email configuration properties");
            return props;
        } catch (IOException e) {
            logger.error("Failed to load email.properties", e);
            throw new RuntimeException("Could not load email configuration", e);
        }
    }

    /**
     * Отправляет письмо с кодом подтверждения на заданный email-адрес.
     *
     * @param recipientEmail email-адрес получателя
     * @param code           OTP-код для отправки
     */
    @Override
    public void sendCode(String recipientEmail, String code) {
        logger.info("Preparing to send OTP code to {}", recipientEmail);
        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromAddress));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(recipientEmail));
            message.setSubject("Your OTP Code");
            message.setText("Your one-time confirmation code is: " + code);

            Transport.send(message);
            logger.info("OTP code successfully sent via Email to {}", recipientEmail);
        } catch (MessagingException e) {
            logger.error("Failed to send OTP email to {}", recipientEmail, e);
            throw new RuntimeException("Email sending failed", e);
        }
    }
}
