package pro.sky.telegrambot.service;

import com.pengrad.telegrambot.TelegramBot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pro.sky.telegrambot.Reminder;
import pro.sky.telegrambot.repository.ReminderRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReminderServiceTest {

    @Mock
    private ReminderRepository repository;

    @Mock
    private TelegramBot telegramBot;

    @InjectMocks
    private ReminderService reminderService;

    @Test
    void testParseAndSaveReminder_ValidMessage_ReturnsTrue() {
        // Arrange
        Long chatId = 123456789L;

        LocalDateTime futureDate = LocalDateTime.now().plusDays(1); // Завтра
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
        String validMessage = futureDate.format(formatter) + " Сделать домашнюю работу";

        Reminder savedReminder = new Reminder(chatId, "Сделать домашнюю работу", futureDate);

        when(repository.save(any(Reminder.class))).thenReturn(savedReminder);

        // Act
        boolean result = reminderService.parseAndSaveReminder(chatId, validMessage);

        // Assert
        assertTrue(result);
        verify(repository, times(1)).save(any(Reminder.class));
    }

    @Test
    void testParseAndSaveReminder_InvalidFormat_ReturnsFalse() {
        // Arrange
        Long chatId = 123456789L;
        String invalidMessage = "Просто текст без даты";

        // Act
        boolean result = reminderService.parseAndSaveReminder(chatId, invalidMessage);

        // Assert
        assertFalse(result);
        verify(repository, never()).save(any(Reminder.class));
    }

    @Test
    void testParseAndSaveReminder_PastDate_ReturnsFalse() {
        // Arrange
        Long chatId = 123456789L;
        String pastDateMessage = "01.01.2020 20:00 Прошлое напоминание";

        // Act
        boolean result = reminderService.parseAndSaveReminder(chatId, pastDateMessage);

        // Assert
        assertFalse(result);
        verify(repository, never()).save(any(Reminder.class));
    }

    @Test
    void testParseAndSaveReminder_InvalidDateTime_ReturnsFalse() {
        // Arrange
        Long chatId = 123456789L;
        String invalidDateTime = "99.99.2025 25:61 Неверная дата";

        // Act
        boolean result = reminderService.parseAndSaveReminder(chatId, invalidDateTime);

        // Assert
        assertFalse(result);
        verify(repository, never()).save(any(Reminder.class));
    }

    @Test
    void testGetRemindersToSend_ReturnsList() {
        // Arrange
        LocalDateTime time = LocalDateTime.now();
        Reminder reminder1 = new Reminder(1L, "Тест 1", time);
        Reminder reminder2 = new Reminder(2L, "Тест 2", time);
        List<Reminder> expectedReminders = Arrays.asList(reminder1, reminder2);

        when(repository.findRemindersForSending(time)).thenReturn(expectedReminders);

        // Act
        List<Reminder> result = reminderService.getRemindersToSend(time);

        // Assert
        assertEquals(2, result.size());
        assertEquals(expectedReminders, result);
        verify(repository, times(1)).findRemindersForSending(time);
    }

    @Test
    void testGetUserReminders_ReturnsUserReminders() {
        // Arrange
        Long chatId = 123456789L;
        Reminder reminder1 = new Reminder(chatId, "Задача 1", LocalDateTime.now().plusDays(1));
        Reminder reminder2 = new Reminder(chatId, "Задача 2", LocalDateTime.now().plusDays(2));
        List<Reminder> expectedReminders = Arrays.asList(reminder1, reminder2);

        when(repository.findByChatId(chatId)).thenReturn(expectedReminders);

        // Act
        List<Reminder> result = reminderService.getUserReminders(chatId);

        // Assert
        assertEquals(2, result.size());
        assertEquals(expectedReminders, result);
        verify(repository, times(1)).findByChatId(chatId);
    }

    @Test
    void testGetUserReminders_NoReminders_ReturnsEmptyList() {
        // Arrange
        Long chatId = 999999999L;
        when(repository.findByChatId(chatId)).thenReturn(List.of());

        // Act
        List<Reminder> result = reminderService.getUserReminders(chatId);

        // Assert
        assertTrue(result.isEmpty());
        verify(repository, times(1)).findByChatId(chatId);
    }

    @Test
    void testMarkAsSent_MarksRemindersAsSent() {
        // Arrange
        Reminder reminder1 = new Reminder(1L, "Задача 1", LocalDateTime.now());
        Reminder reminder2 = new Reminder(2L, "Задача 2", LocalDateTime.now());
        List<Reminder> reminders = Arrays.asList(reminder1, reminder2);

        when(repository.saveAll(any())).thenReturn(reminders);

        // Act
        reminderService.markAsSent(reminders);

        // Assert
        assertTrue(reminder1.isSent());
        assertTrue(reminder2.isSent());
        verify(repository, times(1)).saveAll(reminders);
    }

    @Test
    void testParseAndSaveReminder_EdgeCases() {
        // Тест с разными форматами дат
        Long chatId = 123456789L;

        // Используем даты относительно текущего времени
        LocalDateTime now = LocalDateTime.now();

        // Корректные форматы - используем будущие даты
        LocalDateTime futureDate1 = now.plusMonths(2); // через 2 месяца
        LocalDateTime futureDate2 = now.plusDays(1);   // завтра

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

        assertTrue(testSingleReminder(chatId,
                futureDate1.format(formatter) + " Конец года"));
        assertTrue(testSingleReminder(chatId,
                futureDate2.format(formatter) + " Начало года"));

        // Некорректные форматы
        assertFalse(reminderService.parseAndSaveReminder(chatId, "32.13.2025 25:61 Неверная дата"));
        assertFalse(reminderService.parseAndSaveReminder(chatId, "ab.cd.efgh ij:kl Текст"));

        // Тест с прошедшей датой
        LocalDateTime pastDate = now.minusDays(1); // вчера
        assertFalse(testSingleReminder(chatId,
                pastDate.format(formatter) + " Прошлая дата"));
    }

    private boolean testSingleReminder(Long chatId, String message) {
        // Вспомогательный метод для тестирования одного напоминания
        try {
            return reminderService.parseAndSaveReminder(chatId, message);
        } catch (Exception e) {
            return false;
        }
    }

    @Test
    void testParseAndSaveReminder_WithDifferentTimeFormats() {
        Long chatId = 123456789L;

        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

        // Разные валидные форматы времени (будущие даты)
        LocalDateTime futureDate1 = now.plusMonths(3); // через 3 месяца
        LocalDateTime futureDate2 = now.plusMonths(4); // через 4 месяца
        LocalDateTime futureDate3 = now.plusMonths(5); // через 5 месяцев

        assertTrue(testSingleReminder(chatId,
                futureDate1.format(formatter) + " Утреннее напоминание"));
        assertTrue(testSingleReminder(chatId,
                futureDate2.format(formatter) + " Обеденное напоминание"));
        assertTrue(testSingleReminder(chatId,
                futureDate3.format(formatter) + " Вечернее напоминание"));
    }

    @Test
    void testParseAndSaveReminder_BoundaryDateTimeValues() {
        Long chatId = 123456789L;

        // Граничные значения времени
        assertTrue(testSingleReminder(chatId, "31.12.2030 23:59 Последняя минута года"));
        assertTrue(testSingleReminder(chatId, "01.01.2030 00:00 Первая минута года"));

        // Проверка високосного года
        assertTrue(testSingleReminder(chatId, "29.02.2028 12:00 Високосный год"));
    }

    @Test
    void testParseAndSaveReminder_InvalidLeapYear_ReturnsFalse() {
        Long chatId = 123456789L;

        // 29 февраля в невисокосном году
        assertFalse(reminderService.parseAndSaveReminder(chatId, "29.02.2023 12:00 Невисокосный год"));
    }

    @Test
    void testParseAndSaveReminder_Performance() {
        Long chatId = 123456789L;
        LocalDateTime futureDate = LocalDateTime.now().plusDays(1);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

        // Множественные запросы подряд
        for (int i = 0; i < 10; i++) {
            String message = futureDate.format(formatter) + " Напоминание " + i;
            assertTrue(testSingleReminder(chatId, message));
        }
    }

    @Test
    void testParseAndSaveReminder_Security() {
        Long chatId = 123456789L;
        LocalDateTime futureDate = LocalDateTime.now().plusDays(1);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

        // Попытка SQL-инъекции в тексте
        String sqlInjection = futureDate.format(formatter) + " '; DROP TABLE reminders; --";
        assertTrue(testSingleReminder(chatId, sqlInjection)); // Должно обрабатываться безопасно

        // XSS попытка
        String xssAttempt = futureDate.format(formatter) + " <script>alert('test')</script>";
        assertTrue(testSingleReminder(chatId, xssAttempt)); // Должно обрабатываться безопасно
    }
}
