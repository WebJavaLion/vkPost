package ru.vk.bot.repost.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.vk.bot.repost.entities.ChatManager;
import ru.vk.bot.repost.entities.Competition;
import ru.vk.bot.repost.enums.Action;
import ru.vk.bot.repost.interfaces.Sender;
import ru.vk.bot.repost.interfaces.UpdateHandler;
import ru.vk.bot.repost.repository.ChatManagerRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
public class CompetitionService {

    private final TelegramBotService botService;
    private final ChatManagerRepository repository;
    private final Map<Action, UpdateHandler<Message>> messageHandlerMap;
    private final Map<Action, UpdateHandler<CallbackQuery>> callbackHandlerMap;

    @Autowired
    public CompetitionService(TelegramBotService botService,
                              ChatManagerRepository chatManagerRepository,
                              Set<UpdateHandler<Message>> messageHandlers,
                              Set<UpdateHandler<CallbackQuery>> callbackHandlers) {

        this.botService = botService;
        this.repository = chatManagerRepository;

        messageHandlerMap = new HashMap<>();
        for (UpdateHandler<Message> messageHandler : messageHandlers) {
            messageHandlerMap.put(messageHandler.getStatus(), messageHandler);
        }

        callbackHandlerMap = new HashMap<>();
        for (UpdateHandler<CallbackQuery> callbackHandler : callbackHandlers) {
            callbackHandlerMap.put(callbackHandler.getStatus(), callbackHandler);
        }
    }

    @Transactional
    public void execute(Update update, Sender sender) {

        if (update.hasMessage() && update.getMessage().isUserMessage()) {
            Message message = update.getMessage();

            Optional<ChatManager> chatManagerOptional =
                    repository
                            .findByTelegramId(message.getFrom().getId());

            if (chatManagerOptional.isPresent()) {

                ChatManager manager = chatManagerOptional.get();
                Competition currentCompetition = manager.getCurrentCompetition();
                UpdateHandler<Message> messageUpdateHandler;

                if (currentCompetition != null) {

                    if (currentCompetition.getAction() == null) {
                        Action action = Action.getStatusMap().get(message.getText());

                        if (action != null) {
                            messageUpdateHandler = messageHandlerMap.get(action);
                            messageUpdateHandler.handleUpdate(message, sender, manager);
                        } else {
                            messageHandlerMap.get(Action.START)
                                    .handleUpdate(message, sender, null);
                        }
                    } else {
                        if (Action.NONE.equals(currentCompetition.getAction())) {

                            if (Action.getStatusMap().get(message.getText()) != null) {
                                messageUpdateHandler =
                                        messageHandlerMap.get(Action.getStatusMap().get(message.getText()));
                            } else {
                                messageUpdateHandler = messageHandlerMap.get(Action.DEFAULT);
                            }
                        } else if(Action.CANCEL.getValue().equals(message.getText())) {
                            messageUpdateHandler = messageHandlerMap.get(Action.CANCEL);

                        } else {
                            messageUpdateHandler = messageHandlerMap.get(currentCompetition.getAction());
                        }
                        messageUpdateHandler.handleUpdate(message, sender, manager);
                    }
                } else {
                    if (Action.getStatusMap().containsKey(message.getText())) {
                        messageUpdateHandler = messageHandlerMap
                                .get(Action.getStatusMap()
                                        .get(message.getText())
                                );

                        messageUpdateHandler.handleUpdate(message, sender, manager);

                    } else if (message.getForwardFromChat() != null) {
                        messageHandlerMap.get(Action.ADD_NEW_CHAT).handleUpdate(message, sender, manager);
                    } else {
                        sender.send(
                                new SendMessage(message.getFrom().getId().longValue(),
                                        "Чтобы создать розыгрыш, нажмите соответствующую кнопку"));
                    }
                }
            } else {
                messageHandlerMap.get(Action.START).handleUpdate(message, sender, null);
            }
        } else if (update.hasCallbackQuery()) {
            CallbackQuery callbackQuery = update.getCallbackQuery();

            Action key = Action.getStatusMap()
                    .get(callbackQuery.getData().split(" ")[0]);

            UpdateHandler<CallbackQuery> callbackQueryUpdateHandler =
                    callbackHandlerMap.get(key);

            if (Action.ADD_PARTICIPANT.equals(key)) {
                callbackQueryUpdateHandler.handleUpdate(callbackQuery, sender, null);

            } else {
                Optional<ChatManager> chatManagerOptional =
                        repository.findByTelegramId(callbackQuery.getFrom().getId());

                chatManagerOptional.ifPresent(
                        chatManager ->
                                callbackQueryUpdateHandler
                                        .handleUpdate(
                                                callbackQuery,
                                                sender,
                                                chatManager));
            }
            AnswerCallbackQuery answerCallbackQuery = new AnswerCallbackQuery();
            answerCallbackQuery.setCallbackQueryId(callbackQuery.getId());

            try {
                botService.execute(answerCallbackQuery);

            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }
}
