package forms;

import java.awt.*;
import androidx.jvm.swing.lifecycle.jFrame.ComponentJFrame;
import androidx.savedstate.SavedState;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class MainScreen  extends ComponentJFrame {

    public MainScreen() {
        setUI();
    }

    @Override
    public void onCreate(@Nullable SavedState savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    private void setUI() {
        initComponents();
        button1.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                BookEditorExample.start();
            }
        });
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        contentPane = new JPanel();
        label1 = new JLabel();
        button1 = new JButton();

        //======== this ========
        var contentPane2 = getContentPane();
        contentPane2.setLayout(new BorderLayout());

        //======== contentPane ========
        {
            contentPane.setLayout(new FlowLayout());

            //---- label1 ----
            label1.setText("\u4e66\u7c4d\u7a97\u53e3");
            label1.setVerticalAlignment(SwingConstants.TOP);
            contentPane.add(label1);

            //---- button1 ----
            button1.setText("\u6253\u5f00");
            contentPane.add(button1);
        }
        contentPane2.add(contentPane, BorderLayout.CENTER);
        setSize(325, 192);
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    private JPanel contentPane;
    private JLabel label1;
    private JButton button1;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
