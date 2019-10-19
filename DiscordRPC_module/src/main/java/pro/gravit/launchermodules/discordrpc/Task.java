package pro.gravit.launchermodules.discordrpc;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TimerTask;

public class Task extends TimerTask {


    //Чтоб наверняка, если что-то пошло не так.
    @Override
    public void run() {
        Set<Thread> threads = Thread.getAllStackTraces().keySet();
        List<String> names = new ArrayList<>();
        threads.forEach(t -> names.add(t.getName()));
        if (!names.contains("main")) {
            club.minnced.discord.rpc.DiscordRPC.INSTANCE.Discord_Shutdown();
            if (DiscordRPC.thr != null) DiscordRPC.thr.interrupt();
        }
    }
}
