package pro.sky.telegrambot.service;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import pro.sky.telegrambot.Reminder;

import javax.annotation.PostConstruct;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramBotService {

    private final TelegramBot telegramBot;
    private final ReminderService reminderService;

    @PostConstruct
    public void init() {
        telegramBot.setUpdatesListener(updates -> {
            processUpdates(updates);
            return UpdatesListener.CONFIRMED_UPDATES_ALL;
        });
        log.info("Telegram bot инициализирован");
    }

    void processUpdates(List<Update> updates) {
        updates.forEach(update -> {
            if (update.message() != null && update.message().text() != null) {
                processMesage(update.message().chat().id(), update.message().text());
            }
        });
    }

    void processMesage(Long chatId, String text) {
        log.info("Получено сообщение: {} из чата {}", text, chatId);

        if ("/start".equals(text)) {
            sendWelcomeMessage(chatId);
        } else if ("/my_tasks".equals(text)) {
            showUserReminders(chatId);
        } else {
            processReminderMessage(chatId, text);
        }
    }

    void sendWelcomeMessage(Long chatId) {
        String message = "Привет! Я бот для напоминаний.\\n\\n" +
                "Отправь мне сообщение в формате:\n" +
                "01.01.2022 20:00 Сделать домашнюю работу\n\n" +
                "И я напомню тебе в указанное время!\n\n" +
                "Команды:\n" +
                "/start - показать это сообщение\n" +
                "/my_tasks - показать мои напоминания";
        sendMessage(chatId, message);
    }

    void processReminderMessage(Long chatId, String text) {
        boolean success = reminderService.parseAndSaveReminder(chatId, text);

        if (success) {
            sendMessage(chatId, "Напоминание успешно создано!");
        } else {
            sendMessage(chatId, "Неверный формат сообщения. Используйте:\n" +
                    "01.01.2022 20:00 Ваш текст напоминания\n\n" +
                    "Убедитесь, что дата и время в будущем!");
        }
    }

    void showUserReminders(Long chatId) {
        var reminders = reminderService.getUserReminders(chatId);

        if (reminders.isEmpty()) {
            sendMessage(chatId, "Нет активных напоминаний");
            return;
        }
        StringBuilder message = new StringBuilder("Напоминания: \n\n");
        for (Reminder reminder : reminders) {
            message.append(String.format("%s: %s\n",
                    reminder.getReminderDateTime().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")),
                    reminder.getMessageText()));
        }
        sendMessage(chatId, message.toString());
    }

    @Scheduled(cron = "0 * * * * *") //проверка каждую минуту
    public void scheduleCheckAndSendReminders() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        List<Reminder> remindersToSend = reminderService.getRemindersToSend(now);

        log.info("Найдено {} напоминаний для отправки.", remindersToSend.size());

        if (!remindersToSend.isEmpty()) {
            for (Reminder reminder : remindersToSend) {
                String message = String.format("Напоминание: %s", reminder.getMessageText());
                sendMessage(reminder.getChatId(), message);
            }
            reminderService.markAsSent(remindersToSend); //отмечаем отправленные
        }
    }

    public void sendMessage(Long chatId, String message) {
        try {
            SendMessage sendMessage = new SendMessage(chatId, message);
            telegramBot.execute(sendMessage);
            log.info("Напоминание отправленное в чат {}: {}", chatId, message);
        } catch (Exception e) {
            log.error("Не удалось отправить напоминание в чат {}: {}", chatId, e);
        }

    }


}
