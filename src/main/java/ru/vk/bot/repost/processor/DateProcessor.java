package ru.vk.bot.repost.processor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.vk.bot.repost.entities.ChatManager;
import ru.vk.bot.repost.entities.Competition;
import ru.vk.bot.repost.enums.Action;
import ru.vk.bot.repost.interfaces.Sender;
import ru.vk.bot.repost.interfaces.UpdateHandler;
import ru.vk.bot.repost.repository.CompetitionRepository;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;


@Component
@AllArgsConstructor
public class DateProcessor implements UpdateHandler<Message> {


    private final CompetitionRepository competitionRepository;


    @Override
    public Action getStatus() {
        return Action.WRITE_DATE;
    }

    @Override
    public void handleUpdate(Message update,
                             Sender sender,
                             ChatManager chatManager) {

        LocalDateTime finishedDateTime = null;
        try {
            finishedDateTime = parseMessageToLocalDateTime(update.getText());
        } catch (Exception e) {

            sendValidateError(
                    sender,
                    update.getFrom()
                            .getId()
                            .longValue()
            );
        }
        if (finishedDateTime != null &&
                LocalDateTime.now(ZoneId.of("Europe/Moscow")).compareTo(finishedDateTime) < 0) {

            Competition currentCompetition = chatManager.getCurrentCompetition();

            currentCompetition.setFinishDate(finishedDateTime);
            currentCompetition.setAction(Action.NONE);

            competitionRepository.save(currentCompetition);

            sender.send(
                    new SendMessage(
                            update.getFrom().getId().longValue(),
                            "Дата окончания: " + finishedDateTime)
            );
        } else {
            sendValidateError(
                    sender,
                    update.getFrom()
                            .getId()
                            .longValue()
            );
        }
    }

    private LocalDateTime parseMessageToLocalDateTime(String text) {
        String[] splited = text.split(" ");
        LocalDateTime localDateTime = null;

        if (splited.length == 4) {
            String[] time = splited[3].split(":");

            if (time.length == 2) {
                localDateTime =
                        LocalDateTime.parse(
                                text,
                                DateTimeFormatter.ofPattern("yyyy MM dd HH:mm")
                        )
                                .withSecond(0)
                                .withNano(0);
            }
        }
        return localDateTime;
    }

    private void sendValidateError(Sender sender, Long id) {
        sender.send(
                new SendMessage(
                        id,
                        "Неверный формат даты")
        );
    }
}
