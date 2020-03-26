package ru.vk.bot.repost;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;
import ru.vk.bot.repost.service.TelegramBotService;

@SpringBootApplication
@EnableScheduling
public class VkBotApplication {

    public static void main(String[] args) {
        ApiContextInitializer.init();

        ApplicationContext context = SpringApplication.run(VkBotApplication.class, args);
        TelegramBotsApi api = new TelegramBotsApi();

        try {
            api.registerBot(context.getBean(TelegramBotService.class));

        } catch (TelegramApiRequestException e) {
            e.printStackTrace();
        }
    }
}
