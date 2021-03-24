package pro.gravit.launchermodules.remotecontrol;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RemoteControlConfig {
    public List<RemoteControlToken> list = new ArrayList<>();
    public boolean enabled;

    public RemoteControlToken find(String token) {
        for (RemoteControlToken r : list) {
            if (token.equals(r.token)) {
                return r;
            }
        }
        return null;
    }

    public static class RemoteControlToken {
        public String token;
        public long permissions;
        public boolean allowAll;
        public boolean startWithMode;
        public List<String> commands = new ArrayList<>();

        public RemoteControlToken(String token, long permissions, boolean allowAll, List<String> commands) {
            this.token = token;
            this.permissions = permissions;
            this.allowAll = allowAll;
            this.commands = commands;
        }

        public RemoteControlToken(String token, long permissions, boolean allowAll, String[] commands) {
            this.token = token;
            this.permissions = permissions;
            this.allowAll = allowAll;
            this.commands = Arrays.asList(commands.clone());
        }

        public RemoteControlToken() {
        }
    }
}
