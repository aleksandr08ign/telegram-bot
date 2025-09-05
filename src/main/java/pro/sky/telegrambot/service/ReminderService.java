package pro.sky.telegrambot.service;

import com.pengrad.telegrambot.TelegramBot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import pro.sky.telegrambot.Reminder;
import pro.sky.telegrambot.repositiry.ReminderRepository;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Slf4j
@Service
@RequiredArgsConstructor
public class ReminderService {

    private final TelegramBot telegramBot;

    private final ReminderRepository repository;

    public List<Reminder> getRemindersToSend(LocalDateTime time) {
        return repository.findRemindersForSending(time);
    }

    @Transactional
    public void markAsSent(List<Reminder> reminders) {
        reminders.forEach(r -> r.setSent(true));
        repository.saveAll(reminders);
    }

    // Паттерн для сообщения: "01.01.2022 20:00 Сделать домашнюю работу"
    private static final Pattern MESSAGE_PATTERN = Pattern.compile("([0-9\\.\\:\\s]{16})(\\s)(.+)");

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    public boolean parseAndSaveReminder(Long chatId, String message) {
        Matcher matcher = MESSAGE_PATTERN.matcher(message);

        if (matcher.find()) {
            String dataTimeString = matcher.group(1);
            String taskText = matcher.group(3);

            try {
                LocalDateTime reminderDataTime = LocalDateTime.parse(dataTimeString, DATE_TIME_FORMATTER);
                if (reminderDataTime.isBefore(LocalDateTime.now())) {
                    log.warn("Попытка создать напоминание в прошлом: {}", reminderDataTime);
                    return false;
                }

                Reminder reminder = new Reminder(chatId, taskText, reminderDataTime);
                repository.save(reminder);
                log.info("Напоминание создано: {}", reminder);
                return true;
            } catch (DateTimeParseException e) {
                log.error("Не верно указана дата или время: {}", dataTimeString, e);
                return false;
            }
        }
        log.warn("Не верный формат сообщения: {}", message);
        return false;
    }

    //все напоминания пользователя
    public List<Reminder> getUserReminders(Long chatId) {
        return repository.findByChatId(chatId);
    }
}
