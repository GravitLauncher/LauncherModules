package pro.gravit.launchermodules.sentryl.utils;

import io.sentry.Scope;
import pro.gravit.utils.helper.JVMHelper;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class BasicProperties {

    public static void setupBasicProperties(Scope scope) {
        Map<String, String> os = new HashMap<>();
        os.put("Name", System.getProperties().getProperty("os.name"));
        os.put("Arch", String.valueOf(JVMHelper.ARCH_TYPE));
        scope.setContexts("OS", os);
        scope.setTag("OS_TYPE", System.getProperties().getProperty("os.name"));
        Map<String, String> jvm = new HashMap<>();
        jvm.put("Version", String.valueOf(JVMHelper.JVM_VERSION));
        jvm.put("Bits", String.valueOf(JVMHelper.JVM_BITS));
        jvm.put("runtime_mxbean", String.valueOf(JVMHelper.RUNTIME_MXBEAN.getVmVersion()));

        scope.setContexts("Java", jvm);
        scope.setTag("JVM_VERSION", String.valueOf(JVMHelper.JVM_VERSION));
        Map<String, String> systemProperties = new HashMap<>();
        systemProperties.put("Name", System.getProperties().getProperty("os.name"));
        systemProperties.put("file.encoding", System.getProperties().getProperty("file.encoding"));
        systemProperties.put("java.class.path", System.getProperties().getProperty("java.class.path"));
        systemProperties.put("java.class.version", System.getProperties().getProperty("java.class.version"));
        systemProperties.put("java.endorsed.dirs", System.getProperties().getProperty("java.endorsed.dirs"));
        systemProperties.put("java.ext.dirs", System.getProperties().getProperty("java.ext.dirs"));
        systemProperties.put("java.home", System.getProperties().getProperty("java.home"));
        systemProperties.put("java.io.tmpdir", System.getProperties().getProperty("java.io.tmpdir"));
        systemProperties.put("os.arch", System.getProperties().getProperty("os.arch"));
        systemProperties.put("sun.arch.data.model", System.getProperties().getProperty("sun.arch.data.model"));
        systemProperties.put("sun.boot.class.path", System.getProperties().getProperty("sun.boot.class.path"));
        systemProperties.put("sun.jnu.encoding", System.getProperties().getProperty("sun.jnu.encoding"));
        systemProperties.put("user.language", System.getProperties().getProperty("user.language"));
        systemProperties.put("user.timezone", System.getProperties().getProperty("user.timezone"));
        scope.setContexts("System Properties", systemProperties);

        DateFormat df = new SimpleDateFormat("'Date:' " + "yyyy.dd.MM" + " 'Time:' " + "HH:mm:ss" + " 'Timezone:' X");
        Calendar calendar = Calendar.getInstance();
        scope.setContexts("User Time", df.format(calendar.getTime()));

    }
}
