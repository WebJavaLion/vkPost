package ru.vk.bot.repost.scheduler;

import lombok.AllArgsConstructor;
import org.springframework.boot.autoconfigure.info.ProjectInfoProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.vk.bot.repost.entities.Participant;
import ru.vk.bot.repost.entities.Winner;
import ru.vk.bot.repost.repository.CompetitionRepository;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
@AllArgsConstructor
@Transactional
public class CompleteCompetitionScheduler {

    private final TelegramLongPollingBot bot;
    private final CompetitionRepository repository;

    @Scheduled(fixedDelay = 30000)
    public void completeCompetitions() {
        repository.findAllByFinishDate(LocalDateTime.now(ZoneId.of("Europe/Moscow")).withSecond(0).withNano(0))
                .forEach(competition -> {

                    String editedMessage = competition.getText();
                    EditMessageText editMessageText = new EditMessageText();
                    StringBuilder builder = new StringBuilder();

                    if (!competition.getFinished()) {
                        List<Participant> participants = new ArrayList<>(competition.getParticipants());

                        if (participants.size() >= competition.getAmountOfWinners() &&
                                CollectionUtils.isEmpty(competition.getWinners())) {

                            Collections.shuffle(participants);

                            List<Participant> winners = participants.stream()
                                    .limit(competition.getAmountOfWinners())
                                    .collect(Collectors.toList());

                            for (Participant winner : winners) {
                                builder
                                        .append("<a href = \"https://t.me/")
                                        .append(winner.getUserName())
                                        .append("\">")
                                        .append(winner.getFirstName());
                                if (winner.getLastName() != null) {
                                    builder.append(" ").append(winner.getLastName());
                                }
                                builder.append("</a>, ");
                            }
                        } else if (!CollectionUtils.isEmpty(competition.getWinners())) {

                            List<Winner> winners = competition.getWinners();
                            for (Winner winner : winners) {
                                builder
                                        .append("<a href = \"https://t.me/")
                                        .append(winner.getUserName())
                                        .append("\">")
                                        .append(winner.getFirstName());
                                if (winner.getLastName() != null) {
                                    builder.append(" ").append(winner.getLastName());
                                }
                                builder.append("</a>, ");
                            }
                        }

                        editedMessage = editedMessage +
                                "\n" + "******\n Победители: " +
                                builder.delete(builder.length() - 2, builder.length());

                        editMessageText.setChatId(competition.getChat().getChatId());
                        editMessageText.setMessageId(competition.getMessageId());
                        editMessageText.disableWebPagePreview();
                        editMessageText.setText(editedMessage);
                        editMessageText.setParseMode("html");
                        editMessageText.enableHtml(true);

                        competition.setFinished(true);
                        repository.save(competition);

                        try {
                            bot.execute(editMessageText);
                        } catch (TelegramApiException e) {
                            e.printStackTrace();
                        }
                    }
                });
    }
}
