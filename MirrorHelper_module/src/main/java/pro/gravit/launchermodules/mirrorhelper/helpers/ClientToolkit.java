package pro.gravit.launchermodules.mirrorhelper.helpers;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClientToolkit {
    public static List<String> findValuesForKey(String input, String key) {
        String regex = "--" + Pattern.quote(key) + "\\s+([^ -][^ ]*)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);

        List<String> values = new ArrayList<>();

        while (matcher.find()) {
            values.add("--" + key);
            values.add(matcher.group(1));
        }
        return values;
    }
}
