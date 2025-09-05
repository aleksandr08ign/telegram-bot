package pro.sky.telegrambot.configuration;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.DeleteMyCommands;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.logging.Logger;

@Configuration
public class TelegramBotConfiguration {

    @Value("${telegram.bot.token}")
    private String token;

    @Bean
    public TelegramBot telegramBot() {
        validateToken(token);

        TelegramBot bot = new TelegramBot(token);
        System.out.println("Telegram-бот успешно инициализирован с помощью токена: " + maskToken(token));

        clearBotCommands(bot);
        return bot;
    }

        private void validateToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalStateException("Токен Telegram-бота не настроен");
        }
        if (!token.matches("^\\d+:.*$")) {
            throw new IllegalStateException("Недопустимый формат токена Telegram-бота");
        }
        }

        private String maskToken(String token) {
        if (token.length() <= 10) {
            return "***";
        }
        return token.substring(0,10) + "***";
        }

        private void clearBotCommands(TelegramBot bot) {
        try {
            bot.execute(new DeleteMyCommands());
            System.out.println("Команды бота успешно обработаны");
        } catch (Exception e) {
            System.out.println("Предупреждение: Не удалось очистить команды бота - " +e.getMessage());
        }
        }

}
