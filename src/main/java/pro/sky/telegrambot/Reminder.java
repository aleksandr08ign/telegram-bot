package pro.sky.telegrambot;

import lombok.*;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reminder")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Reminder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chat_id", nullable = false)
    private Long chatId;

    @Column(name = "message_text", nullable = false, length = 1000)
    private String messageText;

    @Column(name = "reminder_date_time", nullable = false)
    private LocalDateTime reminderDateTime;

    @Column(name = "created", nullable = false) //создан
    private LocalDateTime created;

    @Column(name = "sent", nullable = false) //отправлен
    private boolean sent;

    public Reminder (Long chatId, String messageText, LocalDateTime reminderDateTime) {
        this.chatId = chatId;
        this.messageText = messageText;
        this.reminderDateTime = reminderDateTime;
        this.created = LocalDateTime.now();
        this.sent = false;
    }

}
