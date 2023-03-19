package pro.gravit.launchermodules.sentryl.utils;

import io.sentry.Scope;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.ComputerSystem;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.OSProcess;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.HashMap;
import java.util.Map;

public class OshiUtils {
    private static final SystemInfo systemInfo = new SystemInfo();
    private static final OSProcess process = systemInfo.getOperatingSystem().getCurrentProcess();
    private static final HardwareAbstractionLayer hardwareAbstractionLayer = systemInfo.getHardware();
    private static final GlobalMemory globalMemory = hardwareAbstractionLayer.getMemory();
    private static final Runtime runtime = Runtime.getRuntime();
    public static void systemProperties(Scope scope) {
        try {
            String bits = String.valueOf(systemInfo.getOperatingSystem().getBitness());
            CentralProcessor processor = hardwareAbstractionLayer.getProcessor();
            Map<String, String> system = new HashMap<>();
            system.put("Bits", bits);
            system.put("CPU(s) Logical", String.valueOf(processor.getLogicalProcessorCount()));
            system.put("CPU(s) Physical", String.valueOf(processor.getPhysicalProcessorCount()));
            system.put("CPU(s) Max Freq", formatGHz(processor.getMaxFreq()));
            system.put("Total memory", humanReadableByteCountBin(globalMemory.getTotal()));
            scope.setContexts("Computer System Properties", system);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Map<String, String> makeMemoryProperties() {
        Map<String, String> memory = new HashMap<>();
        memory.put("Global Free memory", humanReadableByteCountBin(globalMemory.getAvailable()));
        memory.put("Process Resident memory", humanReadableByteCountBin(process.getResidentSetSize()));
        memory.put("Process Virtual memory", humanReadableByteCountBin(process.getVirtualSize()));
        memory.put("Java Max memory", humanReadableByteCountBin(runtime.maxMemory()));
        memory.put("Java Free memory", humanReadableByteCountBin(runtime.freeMemory()));
        memory.put("Java Total memory", humanReadableByteCountBin(runtime.totalMemory()));
        return memory;
    }

    public static String humanReadableByteCountBin(long bytes) {
        long absB = bytes == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(bytes);
        if (absB < 1024) {
            return bytes + " B";
        }
        long value = absB;
        CharacterIterator ci = new StringCharacterIterator("KMGTPE");
        for (int i = 40; i >= 0 && absB > 0xfffccccccccccccL >> i; i -= 10) {
            value >>= 10;
            ci.next();
        }
        value *= Long.signum(bytes);
        return String.format("%.1f %ciB", value / 1024.0, ci.current());
    }

    public static String formatGHz(long hz) {
        return String.format("%.2fGHz", hz / 1024.0 / 1024.0 / 1024.0);
    }
}
