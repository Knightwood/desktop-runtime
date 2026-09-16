import androidx.compose.desktop.runtime.core.ServiceBooter;
import androidx.compose.desktop.runtime.savestate.ApplicationSaveStateSaver;
import androidx.compose.desktop.runtime.savestate.Tokens;
import androidx.jvm.swing.lifecycle.core.intent.Singularity;
import com.formdev.flatlaf.FlatLightLaf;
import forms.MainScreen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import java.awt.*;
import java.util.Enumeration;

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
public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static final Font font_1 = new Font("Noto Sans SC", Font.PLAIN, 16);

    public static void main(String[] args) {
        try {
            logger.info("Starting Mian");
//            {//swing原生
//                // 开启全局字体抗锯齿，打开后有些字体显示效果反而不好
////                System.setProperty("awt.useSystemAAFontSettings", "on");
//                System.setProperty("swing.aatext", "true");
//                // 切换为当前操作系统的原生 UI 风格
//                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
//                InitGlobalFont(font_1); // 统一设置字体
//            }
            {//Flataf
                System.setProperty("flatlaf.uiScale", "1.25");
                FlatLightLaf.setup();
            }
            Singularity.INSTANCE.boot();
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
//                    ApplicationSaveStateSaver service = ServiceBooter.INSTANCE.getService(ApplicationSaveStateSaver.class);
                    MainScreen screen = new MainScreen();
//                    screen.setSavedState(service.obtain(Tokens.of("main-0")));
                    screen.setVisible(true);
//                    TestLifecycleFrame testLifecycleFrame = new TestLifecycleFrame();
//                    testLifecycleFrame.setVisible(true);
                }
            });
        } catch (Exception e) {
            logger.error("err", e);
        }
    }

    /**
     * 统一设置字体，父界面设置之后，所有由父界面进入的子界面都不需要再次设置字体
     */
    private static void InitGlobalFont(Font font) {
        FontUIResource fontRes = new FontUIResource(font);
        for (Enumeration<Object> keys = UIManager.getDefaults().keys(); keys.hasMoreElements(); ) {
            Object key = keys.nextElement();
            Object value = UIManager.get(key);
            if (value instanceof FontUIResource) {
                UIManager.put(key, fontRes);
            }
        }
    }
}
