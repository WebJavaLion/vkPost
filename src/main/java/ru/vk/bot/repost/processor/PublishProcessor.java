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
import ru.vk.bot.repost.repository.ChatManagerRepository;
import ru.vk.bot.repost.repository.CompetitionRepository;
import ru.vk.bot.repost.util.KeyboardUtil;

import java.util.Collections;

@Component
@AllArgsConstructor
public class PublishProcessor implements UpdateHandler<Message> {

    private final CompetitionRepository competitionRepository;
    private final ChatManagerRepository chatManagerRepository;

    @Override
    public Action getStatus() {
        return Action.PUBLISH;
    }


    @Override
    public void handleUpdate(Message update, Sender sender, ChatManager chatManager) {
        Competition currentCompetition = chatManager.getCurrentCompetition();

        if (currentCompetition.getFinishDate() != null) {

            Message sentMessage = sender.send(
                    new SendMessage(
                            chatManager
                                    .getCurrentManagedChat()
                                    .getChatId()
                            ,
                            currentCompetition.getText()
                    )
                            .setReplyMarkup(getParticipantCounterKey(currentCompetition))
            );

            sender.send(new SendMessage(
                    update.getFrom().getId().longValue(),
                    "Конкурс опубликован")
                    .setReplyMarkup(KeyboardUtil.getCommonKeyboard()));

            currentCompetition.setChat(chatManager.getCurrentManagedChat());
            currentCompetition.setMessageId(sentMessage.getMessageId());
            competitionRepository.save(currentCompetition);

            chatManager.setCurrentCompetition(null);
            chatManagerRepository.save(chatManager);
        } else {
            sender.send(
                    new SendMessage(
                            update.getFrom().getId().longValue(),
                            "Установите дату")
            );
        }
    }

    public InlineKeyboardMarkup getParticipantCounterKey(Competition competition) {
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();

        return keyboard.setKeyboard(
                Collections.singletonList(
                        Collections.singletonList(
                                new InlineKeyboardButton()
                                        .setText(
                                                "(" +
                                                competition
                                                        .getParticipants()
                                                        .size() +
                                                        ") Участвовать!"
                                        )
                                        .setCallbackData(
                                                Action.ADD_PARTICIPANT.getValue() +
                                                " " +
                                                competition.getId())
                        )
                )
        );
    }
}
