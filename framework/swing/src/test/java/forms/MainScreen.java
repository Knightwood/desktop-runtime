package forms;

import java.awt.*;
import javax.swing.border.*;

import androidx.compose.desktop.runtime.core.intent.ComponentResultCallback;
import androidx.compose.desktop.runtime.core.intent.LaunchMode;
import androidx.compose.desktop.runtime.savestate.Tokens;
import androidx.core.bundle.Bundle;
import androidx.jvm.swing.lifecycle.core.intent.LaunchJFrameIntent;
import androidx.jvm.swing.lifecycle.jFrame.ComponentJFrame;
import androidx.jvm.swing.lifecycle.jFrame.JFrameManager;
import androidx.savedstate.SavedState;
import com.intellij.uiDesigner.core.*;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class MainScreen extends ComponentJFrame {
    private static final Logger logger = LoggerFactory.getLogger(MainScreen.class);

    public MainScreen() {
        setupUI();
    }

    @Override
    public void onCreate(@Nullable SavedState savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    private void setupUI() {
        initComponents();
        button1.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                LaunchJFrameIntent intent = new LaunchJFrameIntent(this, BookEditorExample.class, LaunchMode.STANDARD);
                intent.setTokenForJava(Tokens.of("BookEditorExample"));
                JFrameManager.openJFrameForResult(intent, new ComponentResultCallback() {
                    @Override
                    public void invoke(int resultCode, @Nullable Bundle data) {
                        logger.info("LaunchJFrameIntent resultCode: {}, data: {}", resultCode, data);
                        tv_bookInfo.setText(data.toString());
                    }
                });
            }
        });
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
