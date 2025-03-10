package jp.okiislandsh.util.wifitoys;

import androidx.annotation.NonNull;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AbsTest {

    protected static final class Log {
        private static final @NonNull SimpleDateFormat fmt = new SimpleDateFormat("k:mm:dd.SSS");
        public static void d(@NonNull String string){
            System.out.println(fmt.format(new Date())+"\t"+string);
        }
        public static void w(@NonNull String string){
            Logger.getGlobal().log(Level.WARNING, fmt.format(new Date())+"\t"+string);
        }
        public static void w(@NonNull String string, @NonNull Throwable throwable){
            Logger.getGlobal().log(Level.WARNING, fmt.format(new Date())+"\t"+string, throwable);
        }
    }

}
