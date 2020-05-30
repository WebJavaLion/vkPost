package ru.vk.bot.repost.service;

import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMediaGroup;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.media.InputMedia;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.vk.bot.repost.entities.VkAttachment;
import ru.vk.bot.repost.entities.VkPost;
import ru.vk.bot.repost.repository.VkPostRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * @author Lev_S
 */

@Service
public class TelegramBotService extends TelegramLongPollingBot {

    private final Logger LOGGER = LoggerFactory.getLogger(TelegramBotService.class);

    private static final long CHAT_ID = -1001247006240L;// 363052334; -1001243404896L

    private final VkPostRepository vkPostRepository;

    public static boolean isStopped = true;

    @Autowired
    CompetitionService competitionService;

    @Value("${tg.bot.token}")
    private String token;

    @Value("${tg.bot.name}")
    private String name;

    @Autowired
    public TelegramBotService(DefaultBotOptions options,
                              VkPostRepository vkPostRepository) {
        super(options);
        this.vkPostRepository = vkPostRepository;
    }

    private Message send(SendMessage sendMessage) {
        try {
            return execute(sendMessage);

        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
        return null;
    }

    @SneakyThrows
    @Override
    public void onUpdateReceived(Update update) {
        if (update != null) {
            competitionService.execute(update, this::send);
        }
    }

    @Override
    public String getBotUsername() {
        return name;
    }

    @Override
    public String getBotToken() {
        return token;
    }

    public void repost() {
        if (!isStopped) {
            List<VkPost> postsFromDb = vkPostRepository.findAllByIsSentFalseAndPreparedToPostTrue();

            if (!postsFromDb.isEmpty()) {
                postsFromDb.sort(Comparator.comparing(VkPost::getDate));
                for (VkPost post : postsFromDb) {
                    execute(post);
                    LOGGER.info("Post with id: " + post.getId() + " has sent");
                }
            }
            vkPostRepository.saveAll(postsFromDb);
        }
    }

    @Transactional
    public void execute(VkPost post) {
        try {
            if (!post.getAttachments().isEmpty()) {

                List<VkAttachment> attachments = post.getAttachments();

                if (attachments.size() > 1 || StringUtils.isEmpty(post.getText())) {
                    SendMediaGroup mediaGroup = new SendMediaGroup();
                    List<InputMedia> photoList = new ArrayList<>();

                    attachments.stream()
                            .filter(vkAttachment -> (!vkAttachment.getUrl().contains("http") &&
                                    !vkAttachment.getUrl().contains("https")))
                            .forEach(vkAttachment -> {
                                InputMedia media = new InputMediaPhoto();

                                media.setMedia("https://pp.vk.me" + vkAttachment.getUrl());
                                photoList.add(media);
                            });

                    photoList.get(0).setCaption(post.getText());
                    mediaGroup.setChatId(CHAT_ID);
                    mediaGroup.setMedia(photoList);

                    if (!photoList.isEmpty()) {
                        execute(mediaGroup);
                    }
                } else {
                    if (attachments.size() == 1) {
                        SendMessage message = new SendMessage();

                        message.setParseMode("html");
                        message.setChatId(CHAT_ID);

                        if (!attachments.get(0).getUrl().contains("https")) {

                            message.setText(post.getText() + "<a href = \""
                                    + "https://pp.vk.me"
                                    + attachments.get(0).getUrl()
                                    + "\">&#8205;</a>");
                        } else {

                            if (post.getText().contains(attachments.get(0).getUrl())) {
                                post.setText(post.getText().replace(attachments.get(0).getUrl(), ""));
                            }

                            message.setText(post.getText() + "<a href = \""
                                    + attachments.get(0).getUrl()
                                    + "\">&#8205;</a>");

                        }
                        execute(message);
                    }
                }
            } else {
                execute(
                        new SendMessage(
                                CHAT_ID,
                                post.getText()
                        ));
            }
        } catch (TelegramApiException e) {
            e.printStackTrace();
            LOGGER.error(post.toString());
        } finally {
            post.setIsSent(true);
        }
    }

    public void flushDefinedAmountOfPosts(Integer amount)  {
        List<VkPost> allByIsSentFalse = vkPostRepository.findAllByIsSentFalse();

        allByIsSentFalse
                .stream()
                .sorted(Comparator.comparing(VkPost::getDate).reversed())
                .limit(amount)
                .sorted(Comparator.comparing(VkPost::getDate))
                .filter(VkPost::getPreparedToPost)
                .forEach(this::execute);

        allByIsSentFalse
                .stream()
                .filter(VkPost::getPreparedToPost)
                .forEach(p -> p.setIsSent(true));

        vkPostRepository.saveAll(allByIsSentFalse);
    }
}