package ru.vk.bot.repost.processor;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.vk.bot.repost.entities.ChatManager;
import ru.vk.bot.repost.entities.Competition;
import ru.vk.bot.repost.enums.Action;
import ru.vk.bot.repost.interfaces.Sender;
import ru.vk.bot.repost.interfaces.UpdateHandler;
import ru.vk.bot.repost.repository.CompetitionRepository;
import ru.vk.bot.repost.util.KeyboardUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Component
@AllArgsConstructor
public class MessageProcessor implements UpdateHandler<Message> {

    private final CompetitionRepository competitionRepository;

    @Override
    public Action getStatus() {
        return Action.SET_TEXT;
    }

    @Override
    public void handleUpdate(Message update,
                             Sender sender,
                             ChatManager chatManager) {

        Competition currentCompetition = chatManager.getCurrentCompetition();

        currentCompetition.setText(update.getText());
        currentCompetition.setAction(Action.NONE);

        competitionRepository.save(currentCompetition);

        sender.send(
                new SendMessage(
                        update.getFrom().getId().longValue(),
                        update.getText()
                ).setReplyMarkup(getMessageKeyboard(currentCompetition))
        );
        sender.send(
                new SendMessage(
                        update.getFrom().getId().longValue(),
                        "Установите дату"
                ).setReplyMarkup(KeyboardUtil.getCompetitionOptionsMenuKeyboard())
        );
    }

    private InlineKeyboardMarkup getMessageKeyboard(Competition competition) {
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();

        List<List<InlineKeyboardButton>> buttons = new ArrayList<>(
                Collections.singletonList(
                        Arrays.asList(
                                new InlineKeyboardButton()
                                        .setText("- участников")
                                        .setCallbackData(Action.AMOUNT_OF_SUBSCRIBERS.getValue() + " " +
                                                competition.getId() +
                                                " -"),
                                new InlineKeyboardButton()
                                        .setText("+ участников")
                                        .setCallbackData(Action.AMOUNT_OF_SUBSCRIBERS.getValue() + " " +
                                                competition.getId() +
                                                " +"))
                )
        );
        return keyboard.setKeyboard(buttons);
    }
}

