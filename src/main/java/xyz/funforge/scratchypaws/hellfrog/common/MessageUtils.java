package xyz.funforge.scratchypaws.hellfrog.common;

import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.MessageBuilder;

import java.util.ArrayList;
import java.util.List;

public class MessageUtils {

    public static void sendLongMessage(MessageBuilder messageBuilder, TextChannel channel) {

        if (messageBuilder.getStringBuilder().length() <= 2000) {
            messageBuilder.send(channel);
        } else {
            String[] lines = messageBuilder.getStringBuilder().toString()
                    .split("\n");
            List<String> rebuilds = new ArrayList<>(lines.length);
            for (String line : lines) {
                if (line.length() > 1999) {
                    rebuilds.addAll(CommonUtils.splitEqually(line, 1999));
                } else {
                    rebuilds.add(line);
                }
            }

            MessageBuilder msg = new MessageBuilder();
            for (String line : rebuilds) {
                int current = msg.getStringBuilder().length();
                int lineLen = line.length();
                if (current + lineLen + 1 > 2000) { // учитываем в т.ч. \n как 1 символ переноса
                    msg.send(channel);
                    msg = new MessageBuilder();
                }
                msg.append(line).appendNewLine();
            }
            if (msg.getStringBuilder().length() > 0) {
                msg.send(channel);
            }
        }
    }
}
