package forms;

import java.awt.*;
import javax.swing.border.*;

import androidx.compose.desktop.runtime.core.ServiceBooter;
import androidx.compose.desktop.runtime.savestate.ApplicationSaveStateSaver;
import androidx.compose.desktop.runtime.savestate.Token;
import androidx.compose.desktop.runtime.savestate.Tokens;
import androidx.core.bundle.Bundle;
import androidx.jvm.swing.lifecycle.jFrame.ComponentJFrame;
import androidx.jvm.swing.lifecycle.jFrame.LifecycleJFrame;
import androidx.jvm.swing.lifecycle.viewmodel.JavaCreationExtras;
import androidx.jvm.swing.lifecycle.viewmodel.JavaViewModelProvider;
import androidx.lifecycle.viewmodel.CreationExtras;
import androidx.savedstate.SavedState;
import com.intellij.uiDesigner.core.*;
import model.*;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import viewmodel.BookEditorViewModel;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Date;

public class BookEditorExample extends ComponentJFrame {
    private static final Logger logger = LoggerFactory.getLogger(BookEditorExample.class);
    private BookEditorViewModel viewModel;

    public BookEditorExample() {
        setupUI();
    }

    private void setupUI() {
        initComponents();
        setTitle("Book Editor");
        // Populate the genre combo box
        populateGenreComboBox();

        // Save button event listener
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveChanges();
            }
        });
        // Cancel button event listener
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cancelChanges();
            }
        });
    }

    @Override
    public void onCreate(@Nullable SavedState savedInstanceState) {
        super.onCreate(savedInstanceState);
        logger.debug("onCreate savedInstanceState={}", savedInstanceState);
        //创建viewmodel
        CreationExtras extras = JavaCreationExtras.mutableCreationExtrasOf(
                this,
                mutableCreationExtras -> {
                    mutableCreationExtras.set(BookEditorViewModel.Companion.getCreationExtrasKey1(), 11);
                }
        );
        viewModel = JavaViewModelProvider.create(
                this,
                BookEditorViewModel.Companion.getFactory(),
                extras
        ).get(BookEditorViewModel.class);

        try {
            Bundle o = viewModel.getSavedStateHandle().get("data");
            Book book = viewModel.parse(o);
            resetBookInfo(book);
        }catch (Exception e){//第一次肯定不会恢复成功
            logger.error(e.getMessage(),e);
        }
    }

    private void saveChanges() {
        String authorName = authorNameField.getText();
        String bookName = bookNameField.getText();
        Genre genre = (Genre) genreComboBox.getSelectedItem();
        boolean isTaken = isTakenCheckBox.isSelected();

        // Create Author object
        Author author = new Author(authorName, ""); // Set the author name

        // Create Book object
        Book book = new Book(author, genre, bookName);
        book.setTaken(isTaken);
        viewModel.print();
        //回传结果
        Bundle data = viewModel.save(book);
        resetBookInfo(new Book());
        setResult(LifecycleJFrame.SUCCESS, data);
        finish();
    }

    private void resetBookInfo(Book book) {
        // Reset fields
        authorNameField.setText(book.getAuthor().getName());
        bookNameField.setText(book.getName());
        genreComboBox.setSelectedIndex(book.getGenre().ordinal());
        isTakenCheckBox.setSelected(book.isTaken());
    }


    private void cancelChanges() {
        // Reset fields
        bookNameField.setText("");
        genreComboBox.setSelectedIndex(0);
        isTakenCheckBox.setSelected(false);
    }

    private void populateGenreComboBox() {
        // Get the combo box model
        DefaultComboBoxModel<Genre> comboBoxModel = (DefaultComboBoxModel<Genre>) genreComboBox.getModel();

        // Add genre values to the combo box model
        for (Genre genre : Genre.values()) {
            comboBoxModel.addElement(genre);
        }
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        contentView = new JPanel();
        bookInfoView = new JPanel();
        label1 = new JLabel();
        bookNameField = new JTextField();
        label2 = new JLabel();
        authorNameField = new JTextField();
        label3 = new JLabel();
        genreComboBox = new JComboBox();
        isTakenCheckBox = new JCheckBox();
        buttonBar = new JPanel();
        saveButton = new JButton();
        cancelButton = new JButton();

        //======== this ========
        var contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        //======== contentView ========
        {
            contentView.setBorder(new EmptyBorder(12, 12, 12, 12));
            contentView.setLayout(new BorderLayout());

            //======== bookInfoView ========
            {
                bookInfoView.setLayout(new GridLayoutManager(4, 2, new Insets(0, 0, 0, 0), -1, -1, false, true));

                //---- label1 ----
                label1.setText("\u6807\u9898");
                bookInfoView.add(label1, new GridConstraints(0, 0, 1, 1,
                    GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE,
                    GridConstraints.SIZEPOLICY_CAN_SHRINK,
                    GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                    null, null, null));
                bookInfoView.add(bookNameField, new GridConstraints(0, 1, 1, 1,
                    GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                    GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                    GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                    null, null, null));

                //---- label2 ----
                label2.setText("\u4f5c\u8005");
                bookInfoView.add(label2, new GridConstraints(1, 0, 1, 1,
                    GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE,
                    GridConstraints.SIZEPOLICY_CAN_SHRINK,
                    GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                    null, null, null));
                bookInfoView.add(authorNameField, new GridConstraints(1, 1, 1, 1,
                    GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                    GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                    GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                    null, null, null));

                //---- label3 ----
                label3.setText("\u79cd\u7c7b");
                bookInfoView.add(label3, new GridConstraints(2, 0, 1, 1,
                    GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE,
                    GridConstraints.SIZEPOLICY_CAN_SHRINK,
                    GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                    null, null, null));
                bookInfoView.add(genreComboBox, new GridConstraints(2, 1, 1, 1,
                    GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                    GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                    GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                    null, null, null));

                //---- isTakenCheckBox ----
                isTakenCheckBox.setText("\u4e0d\u53ef\u7528");
                bookInfoView.add(isTakenCheckBox, new GridConstraints(3, 1, 1, 1,
                    GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                    GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                    GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                    null, null, null));
            }
            contentView.add(bookInfoView, BorderLayout.CENTER);

            //======== buttonBar ========
            {
                buttonBar.setBorder(new EmptyBorder(12, 0, 0, 0));
                buttonBar.setLayout(new GridBagLayout());
                ((GridBagLayout)buttonBar.getLayout()).columnWidths = new int[] {0, 85, 80};
                ((GridBagLayout)buttonBar.getLayout()).columnWeights = new double[] {1.0, 0.0, 0.0};

                //---- saveButton ----
                saveButton.setText("OK");
                buttonBar.add(saveButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 0, 5), 0, 0));

                //---- cancelButton ----
                cancelButton.setText("Cancel");
                buttonBar.add(cancelButton, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 0, 0), 0, 0));
            }
            contentView.add(buttonBar, BorderLayout.PAGE_END);
        }
        contentPane.add(contentView, BorderLayout.CENTER);
        setSize(500, 375);
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    private JPanel contentView;
    private JPanel bookInfoView;
    private JLabel label1;
    private JTextField bookNameField;
    private JLabel label2;
    private JTextField authorNameField;
    private JLabel label3;
    private JComboBox genreComboBox;
    private JCheckBox isTakenCheckBox;
    private JPanel buttonBar;
    private JButton saveButton;
    private JButton cancelButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
