import androidx.jvm.swing.lifecycle.core.intent.Singularity;
import com.formdev.flatlaf.FlatLightLaf;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

class PrintWindowStateFrame extends JFrame {
    private static final Logger log = LoggerFactory.getLogger(TestLifecycleFrame.class);
    WindowAdapter adapter;

    public PrintWindowStateFrame() {
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        adapter = new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                super.windowOpened(e);
                log.info("windowOpened");
            }

            @Override
            public void windowIconified(WindowEvent e) {
                super.windowIconified(e);
                log.info("windowIconified");
            }

            @Override
            public void windowDeiconified(WindowEvent e) {
                super.windowDeiconified(e);
                log.info("windowDeiconified");
            }

            @Override
            public void windowActivated(WindowEvent e) {
                super.windowActivated(e);
                log.info("windowActivated");
            }

            @Override
            public void windowDeactivated(WindowEvent e) {
                super.windowDeactivated(e);
                log.info("windowDeactivated");
            }

            @Override
            public void windowStateChanged(WindowEvent e) {
                super.windowStateChanged(e);
            }

            @Override
            public void windowGainedFocus(WindowEvent e) {
                super.windowGainedFocus(e);
                log.info("windowGainedFocus");
            }

            @Override
            public void windowLostFocus(WindowEvent e) {
                super.windowLostFocus(e);
                log.info("windowLostFocus");
            }

            public void windowClosing(WindowEvent e) {
                log.info("Closing");
            }

            public void windowClosed(WindowEvent e) {
                log.info("Closed");
            }
        };
        this.addWindowListener(adapter);
        this.addWindowStateListener(adapter);
        this.addWindowFocusListener(adapter);
        initComponents();
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        scrollPane1 = new JScrollPane();
        tv_bookInfo = new JTextArea();
        contentView = new JPanel();
        label1 = new JLabel();
        hSpacer1 = new JPanel(null);
        button1 = new JButton();

        //======== this ========
        setTitle("\u4e3b\u7a97\u53e3");
        var contentPane = getContentPane();
        contentPane.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), 0, 0));

        //======== scrollPane1 ========
        {
            scrollPane1.setViewportView(tv_bookInfo);
        }
        contentPane.add(scrollPane1, new GridConstraints(0, 0, 1, 1,
                GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                null, null, null));

        //======== contentView ========
        {
            contentView.setLayout(new BoxLayout(contentView, BoxLayout.X_AXIS));

            //---- label1 ----
            label1.setText("\u4e66\u7c4d\u7a97\u53e3");
            contentView.add(label1);

            //---- hSpacer1 ----
            hSpacer1.setMinimumSize(new Dimension(16, 12));
            contentView.add(hSpacer1);

            //---- button1 ----
            button1.setText("\u6253\u5f00");
            contentView.add(button1);
        }
        contentPane.add(contentView, new GridConstraints(1, 0, 1, 1,
                GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                null, null, null));
        setSize(469, 250);
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    private JScrollPane scrollPane1;
    private JTextArea tv_bookInfo;
    private JPanel contentView;
    private JLabel label1;
    private JPanel hSpacer1;
    private JButton button1;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on

}
