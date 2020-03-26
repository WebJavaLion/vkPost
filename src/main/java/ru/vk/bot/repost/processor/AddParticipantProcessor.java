package ru.vk.bot.repost.processor;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.vk.bot.repost.entities.ChatManager;
import ru.vk.bot.repost.entities.Competition;
import ru.vk.bot.repost.entities.Participant;
import ru.vk.bot.repost.enums.Action;
import ru.vk.bot.repost.interfaces.Sender;
import ru.vk.bot.repost.interfaces.UpdateHandler;
import ru.vk.bot.repost.repository.CompetitionRepository;
import ru.vk.bot.repost.repository.ParticipantRepository;

import java.util.Collections;
import java.util.Optional;

@Component
public class AddParticipantProcessor implements UpdateHandler<CallbackQuery> {

    private final CompetitionRepository competitionRepository;
    private final TelegramLongPollingBot bot;
    private final ParticipantRepository participantRepository;

    public AddParticipantProcessor(CompetitionRepository competitionRepository,
                                   @Lazy TelegramLongPollingBot bot,
                                   ParticipantRepository participantRepository) {
        this.competitionRepository = competitionRepository;
        this.bot = bot;
        this.participantRepository = participantRepository;
    }

    @Override
    public Action getStatus() {
        return Action.ADD_PARTICIPANT;
    }

    @Override
    public void handleUpdate(CallbackQuery update,
                             Sender sender,
                             ChatManager chatManager) {

        EditMessageReplyMarkup editMessageReplyMarkup = new EditMessageReplyMarkup();

        Optional<Competition> competitionOptional =
                competitionRepository
                        .findById(Long.valueOf(
                                update.getData().split(" ")[1]));

        if (competitionOptional.isPresent()) {
            Competition competition = competitionOptional.get();

            if (!competition.getFinished()) {
                User from = update.getFrom();
                if (competition.getParticipants().stream().noneMatch(p -> p.getTelegramId().equals(from.getId()))) {
                    Participant participant =
                            participantRepository
                                    .findByTelegramId(from.getId())
                                    .orElseGet(() -> {

                                        Participant newParticipant = new Participant();

                                        newParticipant.setFirstName(from.getFirstName());
                                        newParticipant.setLastName(from.getLastName());
                                        newParticipant.setUserName(from.getUserName());
                                        newParticipant.setTelegramId(from.getId());

                                        return newParticipant;
                                    });

                    competition.getParticipants().add(participant);
                    competitionRepository.save(competition);

                    editMessageReplyMarkup
                            .setReplyMarkup(
                                    new InlineKeyboardMarkup().setKeyboard(
                                            Collections.singletonList(
                                                    Collections.singletonList(
                                                            new InlineKeyboardButton()
                                                                    .setText(
                                                                            "(" +
                                                                                    competition
                                                                                            .getParticipants().size() +
                                                                                    ") Участвовать!")
                                                                    .setCallbackData(Action.ADD_PARTICIPANT.getValue() +
                                                                            " " +
                                                                            competition.getId())
                                                    )
                                            )
                                    ))
                            .setChatId(competition.getChat().getChatId())
                            .setMessageId(competition.getMessageId());
                    try {
                        bot.execute(editMessageReplyMarkup);
                    } catch (TelegramApiException e) {
                        throw new RuntimeException(e.getMessage());
                    }
                }
            }
        }
    }
}

