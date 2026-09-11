import androidx.compose.desktop.runtime.core.ServiceBooter;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.sun.tools.javac.Main;
import forms.MainScreen;
import model.Book;
import forms.BookEditorExample;
import forms.SaveButtonListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;

/**
 * 如果遇到
 * Exception in thread "main" java.awt.IllegalComponentStateException: contentPane cannot be set to null.
 * 解决方案：
 * 1. 如果使用maven构建：
 * File->Setting->Build, Execution, Deployment -> Build Tools -> Maven -> Runner
 * 取消勾选Delegate IDE build/run actions to Maven 这个选项
 * 2. 如果使用gradle构建：
 * File->Setting->Build,Execution,Deployment->Build Tools->Gradle->Build and run using
 * 改成IntlliJ IDEA即可，其他改动都不需要。
 *
 */
public class Mian {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        try {
            FlatLightLaf.setup();
            ServiceBooter.INSTANCE.bootstrap(logger);
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    MainScreen screen = new MainScreen();
                    screen.setVisible(true);
                }
            });
        } catch (Exception e) {
            logger.error("err", e);
        }
    }
}
