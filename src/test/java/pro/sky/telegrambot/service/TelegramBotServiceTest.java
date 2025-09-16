package pro.sky.telegrambot.service;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pro.sky.telegrambot.Reminder;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TelegramBotServiceTest {

    @Mock
    private TelegramBot telegramBot;

    @Mock
    private ReminderService reminderService;

    @InjectMocks
    private TelegramBotService telegramBotService;

    @Test
    void testProcessMesage_StartCommand() {
        // Arrange
        Long chatId = 123456789L;
        String text = "/start";

        // Act
        telegramBotService.processMesage(chatId, text);

        // Assert
        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramBot, times(1)).execute(captor.capture());

        SendMessage sendMessage = captor.getValue();
        assertTrue(sendMessage.getParameters().get("text").toString().contains("Привет! Я бот для напоминаний"));
    }

    @Test
    void testProcessMesage_MyTasksCommand_WithReminders() {
        // Arrange
        Long chatId = 123456789L;
        String text = "/my_tasks";

        Reminder reminder = new Reminder(chatId, "Тестовое напоминание",
                LocalDateTime.of(2025, 1, 1, 12, 0));

        when(reminderService.getUserReminders(chatId)).thenReturn(List.of(reminder));

        // Act
        telegramBotService.processMesage(chatId, text);

        // Assert
        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramBot, times(1)).execute(captor.capture());

        SendMessage sendMessage = captor.getValue();
        assertTrue(sendMessage.getParameters().get("text").toString().contains("Тестовое напоминание"));
    }

    @Test
    void testProcessMesage_MyTasksCommand_NoReminders() {
        // Arrange
        Long chatId = 123456789L;
        String text = "/my_tasks";

        when(reminderService.getUserReminders(chatId)).thenReturn(List.of());

        // Act
        telegramBotService.processMesage(chatId, text);

        // Assert
        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramBot, times(1)).execute(captor.capture());

        SendMessage sendMessage = captor.getValue();
        assertTrue(sendMessage.getParameters().get("text").toString().contains("Нет активных напоминаний"));
    }

    @Test
    void testProcessMesage_ValidReminder() {
        // Arrange
        Long chatId = 123456789L;
        String text = "01.01.2025 12:00 Тестовое напоминание";

        when(reminderService.parseAndSaveReminder(chatId, text)).thenReturn(true);

        // Act
        telegramBotService.processMesage(chatId, text);

        // Assert
        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramBot, times(1)).execute(captor.capture());

        SendMessage sendMessage = captor.getValue();
        assertTrue(sendMessage.getParameters().get("text").toString().contains("Напоминание успешно создано"));
    }

    @Test
    void testProcessMesage_InvalidReminder() {
        // Arrange
        Long chatId = 123456789L;
        String text = "неправильный формат";

        when(reminderService.parseAndSaveReminder(chatId, text)).thenReturn(false);

        // Act
        telegramBotService.processMesage(chatId, text);

        // Assert
        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramBot, times(1)).execute(captor.capture());

        SendMessage sendMessage = captor.getValue();
        assertTrue(sendMessage.getParameters().get("text").toString().contains("Неверный формат сообщения"));
    }

    @Test
    void testScheduleCheckAndSendReminders_WithReminders() {
        // Arrange
        Long chatId = 123456789L;
        LocalDateTime now = LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.MINUTES);

        Reminder reminder = new Reminder(chatId, "Тестовое напоминание", now);
        when(reminderService.getRemindersToSend(now)).thenReturn(List.of(reminder));

        // Act
        telegramBotService.scheduleCheckAndSendReminders();

        // Assert
        verify(reminderService, times(1)).getRemindersToSend(now);
        verify(reminderService, times(1)).markAsSent(List.of(reminder));

        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramBot, times(1)).execute(captor.capture());

        SendMessage sendMessage = captor.getValue();
        assertTrue(sendMessage.getParameters().get("text").toString().contains("Тестовое напоминание"));
    }

    @Test
    void testScheduleCheckAndSendReminders_NoReminders() {
        // Arrange
        LocalDateTime now = LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.MINUTES);
        when(reminderService.getRemindersToSend(now)).thenReturn(List.of());

        // Act
        telegramBotService.scheduleCheckAndSendReminders();

        // Assert
        verify(reminderService, times(1)).getRemindersToSend(now);
        verify(reminderService, never()).markAsSent(any());
        verify(telegramBot, never()).execute(any(SendMessage.class));
    }

    @Test
    void testSendMessage_Success() {
        // Arrange
        Long chatId = 123456789L;
        String messageText = "Тестовое сообщение";

        // Act
        telegramBotService.sendMessage(chatId, messageText);

        // Assert
        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramBot, times(1)).execute(captor.capture());

        SendMessage sendMessage = captor.getValue();
        assertEquals(chatId, sendMessage.getParameters().get("chat_id"));
        assertEquals(messageText, sendMessage.getParameters().get("text"));
    }

    @Test
    void testSendMessage_ExceptionHandling() {
        // Arrange
        Long chatId = 123456789L;
        String messageText = "Тестовое сообщение";

        doThrow(new RuntimeException("Тестовая ошибка")).when(telegramBot).execute(any(SendMessage.class));

        // Act
        telegramBotService.sendMessage(chatId, messageText);

        // Assert - не должно быть исключения, ошибка должна быть залогирована
        verify(telegramBot, times(1)).execute(any(SendMessage.class));
    }

    @Test
    void testShowUserReminders_Formatting() {
        // Arrange
        Long chatId = 123456789L;
        LocalDateTime dateTime1 = LocalDateTime.of(2025, 1, 1, 12, 0);
        LocalDateTime dateTime2 = LocalDateTime.of(2025, 1, 2, 14, 30);

        Reminder reminder1 = new Reminder(chatId, "Первое напоминание", dateTime1);
        Reminder reminder2 = new Reminder(chatId, "Второе напоминание", dateTime2);

        when(reminderService.getUserReminders(chatId)).thenReturn(List.of(reminder1, reminder2));

        // Act
        telegramBotService.showUserReminders(chatId);

        // Assert
        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramBot, times(1)).execute(captor.capture());

        SendMessage sendMessage = captor.getValue();
        String message = sendMessage.getParameters().get("text").toString();

        assertTrue(message.contains("01.01.2025 12:00"));
        assertTrue(message.contains("Первое напоминание"));
        assertTrue(message.contains("02.01.2025 14:30"));
        assertTrue(message.contains("Второе напоминание"));
    }

    @Test
    void testProcessMesage_UnknownCommand() {
        // Arrange
        Long chatId = 123456789L;
        String text = "/unknown_command";

        when(reminderService.parseAndSaveReminder(chatId, text)).thenReturn(false);

        // Act
        telegramBotService.processMesage(chatId, text);

        // Assert - должно обрабатываться как напоминание
        verify(reminderService, times(1)).parseAndSaveReminder(chatId, text);
    }

    @Test
    void testProcessMesage_EmptyMessage() {
        // Arrange
        Long chatId = 123456789L;
        String text = "";

        // Act
        telegramBotService.processMesage(chatId, text);

        // Assert - должно обрабатываться как напоминание
        verify(reminderService, times(1)).parseAndSaveReminder(chatId, text);
    }

    @Test
    void testProcessMesage_WhitespaceMessage() {
        // Arrange
        Long chatId = 123456789L;
        String text = "   ";

        // Act
        telegramBotService.processMesage(chatId, text);

        // Assert - должно обрабатываться как напоминание
        verify(reminderService, times(1)).parseAndSaveReminder(chatId, text);
    }

    @Test
    void testScheduleCheckAndSendReminders_MultipleReminders() {
        // Arrange
        LocalDateTime now = LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.MINUTES);

        Reminder reminder1 = new Reminder(1L, "Напоминание 1", now);
        Reminder reminder2 = new Reminder(2L, "Напоминание 2", now);

        when(reminderService.getRemindersToSend(now)).thenReturn(List.of(reminder1, reminder2));

        // Act
        telegramBotService.scheduleCheckAndSendReminders();

        // Assert
        verify(telegramBot, times(2)).execute(any(SendMessage.class));
        verify(reminderService, times(1)).markAsSent(List.of(reminder1, reminder2));
    }

    @Test
    void testInit_MethodExists() {
        // Просто проверяем, что метод init существует и может быть вызван
        assertDoesNotThrow(() -> telegramBotService.init());
    }
}
