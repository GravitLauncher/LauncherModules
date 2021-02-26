package ru.tenebrius.launchermodules.discordintegration;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.utils.helper.LogHelper;

public class MessageListener extends ListenerAdapter {

    private final LaunchServer server;

    public MessageListener(LaunchServer srv) {
        server = srv;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        Message message = event.getMessage();
        String msg = message.getContentDisplay();

        if (event.isFromType(ChannelType.TEXT)) {
            TextChannel textChannel = event.getTextChannel();
            if (!textChannel.getId().equals(DSIntegrationModule.config.channelID)) return;
            if (!msg.startsWith(DSIntegrationModule.config.prefix)) return;
            if (DSIntegrationModule.config.adminOnly) {
                Member member = event.getMember();
                if (member == null) return;
                if (!member.hasPermission(Permission.ADMINISTRATOR)) return;
            }
            String command = msg.substring(DSIntegrationModule.config.prefix.length());
            System.out.println(command);
            LogHelper.OutputEnity webLog = new LogHelper.OutputEnity(DSIntegrationModule::sendMsg, LogHelper.OutputTypes.PLAIN);
            LogHelper.addOutput(webLog);
            server.commandHandler.eval(command, true);
            DSIntegrationModule.send(true);
            LogHelper.removeOutput(webLog);
        }
    }
}
