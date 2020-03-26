package ru.vk.bot.repost.controller;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.vk.bot.repost.service.TelegramBotService;

/**
 * @author Lev_S
 */

@RestController
@RequestMapping("/management")
@RequiredArgsConstructor
public class ManagementController {

    private final Logger LOGGER = LoggerFactory.getLogger(ManagementController.class);

    private final TelegramBotService service;


    @GetMapping("/proceed")
    public void proceedInvocation(@RequestParam(value = "amount", defaultValue = "1") Integer amount,
                                  @RequestParam(value = "jobStart", defaultValue = "true") boolean doStartJob) {
        if (TelegramBotService.isStopped) {

            service.flushDefinedAmountOfPosts(amount);
            LOGGER.info(amount + " posts have been sent");

            if (doStartJob) {
                TelegramBotService.isStopped = false;
                LOGGER.info("Requesting has been proceeded");
            }
        }
    }

    @GetMapping("/stop")
    public void stopInvocation() {
        if (!TelegramBotService.isStopped) {
            TelegramBotService.isStopped = true;
            LOGGER.info("Requesting has been stopped");

        }
    }

    @GetMapping("/status")
    public String getStatusOfJob() {
        return TelegramBotService.isStopped ? "Job was stopped" : "Job is working";
    }

    @GetMapping("/change")
    public void change() {
        
    }
}

