package ru.vk.bot.repost.util;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import ru.vk.bot.repost.enums.Action;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public final class KeyboardUtil {

    private KeyboardUtil() {

    }

    public static ReplyKeyboardMarkup getCommonKeyboard() {

        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setSelective(true);
        keyboard.setResizeKeyboard(true);

        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton().setText(Action.SHOW_CHAT.getValue()));
        row1.add(new KeyboardButton().setText(Action.CREATED.getValue()));

        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton().setText(Action.ADD_CHAT.getValue()));

        List<KeyboardRow> rows = new ArrayList<>(Arrays.asList(row1, row2));

        return keyboard.setKeyboard(rows);
    }

    public static ReplyKeyboardMarkup getCompetitionOptionsMenuKeyboard() {

        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);
        keyboard.setSelective(true);

        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton().setText(Action.EDIT_MESSAGE.getValue()));
        row1.add(new KeyboardButton().setText(Action.SET_DATE.getValue()));

        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton().setText(Action.CANCEL.getValue()));

        KeyboardRow row3 = new KeyboardRow();
        row3.add(new KeyboardButton().setText(Action.PUBLISH.getValue()));
        row3.add(new KeyboardButton().setText(Action.PROVIDE_WINNERS.getValue()));

        return keyboard.setKeyboard(Arrays.asList(row1, row2, row3));
    }
}
