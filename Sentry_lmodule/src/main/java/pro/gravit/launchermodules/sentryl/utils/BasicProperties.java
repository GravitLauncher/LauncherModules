package pro.gravit.launchermodules.sentryl.utils;

import io.sentry.Scope;
import pro.gravit.utils.helper.JVMHelper;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class BasicProperties {

    public static void setupBasicProperties(Scope scope) {
        Properties properties = System.getProperties();
        Map<String, String> os = new HashMap<>();
        os.put("Name", properties.getProperty("os.name"));
        os.put("Arch", String.valueOf(JVMHelper.ARCH_TYPE));
        scope.setContexts("OS", os);
        scope.setTag("OS_TYPE", properties.getProperty("os.name"));
        Map<String, String> jvm = new HashMap<>();
        jvm.put("Version", String.valueOf(JVMHelper.JVM_VERSION));
        jvm.put("Bits", String.valueOf(JVMHelper.JVM_BITS));
        jvm.put("runtime_mxbean", String.valueOf(JVMHelper.RUNTIME_MXBEAN.getVmVersion()));

        scope.setContexts("Java", jvm);
        scope.setTag("JVM_VERSION", String.valueOf(JVMHelper.JVM_VERSION));
        Map<String, String> systemProperties = new HashMap<>();

        systemProperties.put("Name", properties.getProperty("os.name"));
        systemProperties.put("file.encoding", properties.getProperty("file.encoding"));
        systemProperties.put("java.class.path", properties.getProperty("java.class.path"));
        systemProperties.put("java.class.version", properties.getProperty("java.class.version"));
        systemProperties.put("java.endorsed.dirs", properties.getProperty("java.endorsed.dirs"));
        systemProperties.put("java.ext.dirs", properties.getProperty("java.ext.dirs"));
        systemProperties.put("java.home", properties.getProperty("java.home"));
        systemProperties.put("java.io.tmpdir", properties.getProperty("java.io.tmpdir"));
        systemProperties.put("os.arch", properties.getProperty("os.arch"));
        systemProperties.put("sun.arch.data.model", properties.getProperty("sun.arch.data.model"));
        systemProperties.put("sun.boot.class.path", properties.getProperty("sun.boot.class.path"));
        systemProperties.put("sun.jnu.encoding", properties.getProperty("sun.jnu.encoding"));
        systemProperties.put("user.language", properties.getProperty("user.language"));
        systemProperties.put("user.timezone", properties.getProperty("user.timezone"));
        systemProperties.put("javafx.runtime.version", properties.getProperty("javafx.runtime.version"));
        scope.setContexts("System Properties", systemProperties);

        DateFormat df = new SimpleDateFormat("'Date:' " + "yyyy.dd.MM" + " 'Time:' " + "HH:mm:ss" + " 'Timezone:' X");
        Calendar calendar = Calendar.getInstance();
        scope.setContexts("User Time", df.format(calendar.getTime()));

    }
}
