package ru.vk.bot.repost.service;

import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.ServiceActor;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import ru.vk.bot.repost.repository.VkPostRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
class VkApiRequestServiceTest {

    @Configuration
    static class Config {

        @MockBean
        ServiceActor actor;
        @MockBean
        VkApiClient client;
        @MockBean
        VkPostRepository repository;
        @MockBean
        TelegramBotService service;

        @Bean
        public VkApiRequestService service() {
            return new VkApiRequestService(actor, client, repository, service);
        }
    }
    @MockBean
    VkPostRepository repository;

    @Autowired
    VkApiRequestService service;

    @Test
    void validateText() {
        Map<String, String> objectObjectMap = Mockito.<String, String>anyMap();
        List<String> any = Mockito.any();
        Map<String, String> mock =Mockito.anyMap();

        String validateText = service.validateText("asfasf [club124124|TTT] fwqwq [club4444|qqqww] faf");
        System.out.println(validateText);
        assertEquals(validateText, "asfasf TTT fwqwq qqqww faf");
    }
}