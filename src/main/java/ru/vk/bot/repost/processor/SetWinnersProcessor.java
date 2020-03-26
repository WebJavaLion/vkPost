package ru.vk.bot.repost.processor;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.vk.bot.repost.entities.ChatManager;
import ru.vk.bot.repost.entities.Competition;
import ru.vk.bot.repost.entities.Winner;
import ru.vk.bot.repost.enums.Action;
import ru.vk.bot.repost.interfaces.Sender;
import ru.vk.bot.repost.interfaces.UpdateHandler;
import ru.vk.bot.repost.repository.CompetitionRepository;

import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@AllArgsConstructor
public class SetWinnersProcessor implements UpdateHandler<Message> {

    private final CompetitionRepository competitionRepository;

    static final Pattern PATTERN = Pattern.compile("^([\\S]+[\\s])[\\S]+([\\s][\\S]+)?$");

    @Override
    public Action getStatus() {
        return Action.SET_WINNERS;
    }

    @Override
    public void handleUpdate(Message update,
                             Sender sender,
                             ChatManager chatManager) {

        String text = update.getText();
        String[] split = text.split(";");

        List<String> filteredList = Stream
                .of(split)
                .map(s ->
                        Stream.of(s.split(" "))
                                .filter(str -> !"".equals(str))
                                .collect(Collectors.joining(" "))
                )
                .collect(Collectors.toList());

        if (filteredList.stream().allMatch(s -> PATTERN.matcher(s).matches())) {

            Competition currentCompetition = chatManager.getCurrentCompetition();
            List<Winner> winners = currentCompetition.getWinners();

            for (String winnerValue : filteredList) {
                String[] winnerValues = winnerValue.split(" ");

                Winner winner = new Winner();
                winner.setUserName(winnerValues[0]);
                winner.setFirstName(winnerValues[1]);
                winner.setCompetition(currentCompetition);

                if (winnerValues.length == 3) {
                    winner.setLastName(winnerValues[2]);
                }
                winners.add(winner);
            }
            competitionRepository.save(currentCompetition);
            currentCompetition.setAction(Action.NONE);

            if (!winners.isEmpty()) {
                sender.send(
                        new SendMessage(
                                update.getFrom()
                                        .getId()
                                        .longValue(),
                                "Победители добавлены")
                );
            }
        } else {
            sender.send(
                    new SendMessage(
                            update.getFrom()
                                    .getId()
                                    .longValue(),
                            "Введите в указанном формате")
            );
        }
    }
}
