package time.series.chart;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class MinimumSizeJFrame {

    public static void main(String[] args) throws ClassNotFoundException, UnsupportedLookAndFeelException, IllegalAccessException, InstantiationException {

//        System.setProperty("swing.defaultlaf", "com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
//        String defaultLookAndFeelClassName = UIManager.getSystemLookAndFeelClassName();
//        UIManager.setLookAndFeel(defaultLookAndFeelClassName);

        final JFrame frame = new JFrame("Frame Title");

        final JButton openModalDialog = new JButton("OpenModalDialog");
        openModalDialog.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openModalDialog(frame);
            }
        });

        final JPanel contentPane1 = new JPanel(new BorderLayout(5, 5));
        contentPane1.add(openModalDialog);

        frame.setContentPane(contentPane1);
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

    private static void openModalDialog(JFrame frame) {
        final JPanel contentPane2 = new JPanel(new GridBagLayout());

        final GridBagConstraints gbc = new GridBagConstraints();

        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.insets = new Insets(2, 2, 2, 2);

        gbc.gridx++;
        contentPane2.add(createTextScrollPane(), gbc);
        gbc.gridx++;
        contentPane2.add(createTextScrollPane(), gbc);
        gbc.gridx++;
        contentPane2.add(createTextScrollPane(), gbc);


        final JDialog dialog = new JDialog(frame, true);
        dialog.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        dialog.setContentPane(contentPane2);
        dialog.pack();
        dialog.setMinimumSize(dialog.getSize());
        dialog.setVisible(true);
    }

    private static JScrollPane createTextScrollPane() {
        final String text = "alsdkjfalsdjna asd fapfpfpau \n" +
                            "fapufafaa alsdkjfalsdjna asd fapfpfpau \n" +
                            "fapufafaa alsdkjfalsdjna asd fapfpfpau fapufafaa \n" +
                            "alsdkjfalsdjna asd fapfpfpau fapufafaa \n" +
                            "alsdkjfalsdjna asd fapfpfpau fapufafaa \n" +
                            "alsdkjfalsdjna asd fapfpfpau fapufafaa alsdkjfalsdjna \n" +
                            "asd fapfpfpau fapufafaa alsdkjfalsdjna asd \n" +
                            "fapfpfpau fapufafaa alsdkjfalsdjna asd fapfpfpau \n" +
                            "fapufafaa alsdkjfalsdjna asd fapfpfpau \n" +
                            "fapufafaa alsdkjfalsdjna asd fapfpfpau fapufafaa \n" +
                            "alsdkjfalsdjna asd fapfpfpau fapufafaa alsdkjfalsdjna \n" +
                            "asd fapfpfpau fapufafaa alsdkjfalsdjna asd fapfpfpau \n" +
                            "fapufafaa alsdkjfalsdjna asd fapfpfpau fapufafaa \n" +
                            "alsdkjfalsdjna asd fapfpfpau fapufafaa alsdkjfalsdjna \n" +
                            "asd fapfpfpau fapufafaa alsdkjfalsdjna asd \n" +
                            "fapfpfpau fapufafaa alsdkjfalsdjna asd \n" +
                            "fapfpfpau fapufafaa alsdkjfalsdjna asd \n" +
                            "fapfpfpau fapufafaa \n" +
                            "alsdkjfalsdjna asd \n" +
                            "alsdkjfalsdjna asd fapfpfpau fapufafaa \n" +
                            "alsdkjfalsdjna asd fapfpfpau fapufafaa \n" +
                            "alsdkjfalsdjna asd fapfpfpau fapufafaa alsdkjfalsdjna \n" +
                            "asd fapfpfpau fapufafaa alsdkjfalsdjna asd \n" +
                            "fapfpfpau fapufafaa alsdkjfalsdjna asd fapfpfpau \n" +
                            "fapufafaa alsdkjfalsdjna asd fapfpfpau \n" +
                            "fapufafaa alsdkjfalsdjna asd fapfpfpau fapufafaa \n" +
                            "alsdkjfalsdjna asd fapfpfpau fapufafaa alsdkjfalsdjna \n" +
                            "asd fapfpfpau fapufafaa alsdkjfalsdjna asd fapfpfpau \n" +
                            "fapufafaa alsdkjfalsdjna asd fapfpfpau fapufafaa \n" +
                            "alsdkjfalsdjna asd fapfpfpau fapufafaa alsdkjfalsdjna \n" +
                            "asd fapfpfpau fapufafaa alsdkjfalsdjna asd \n" +
                            "fapfpfpau fapufafaa alsdkjfalsdjna asd \n" +
                            "fapfpfpau fapufafaa alsdkjfalsdjna asd \n" +
                            "fapfpfpau fapufafaa \n" +
                            "alsdkjfalsdjna asd \n" +
                            "alsdkjfalsdjna asd fapfpfpau fapufafaa \n" +
                            "alsdkjfalsdjna asd fapfpfpau fapufafaa \n" +
                            "alsdkjfalsdjna asd fapfpfpau fapufafaa alsdkjfalsdjna \n" +
                            "asd fapfpfpau fapufafaa alsdkjfalsdjna asd \n" +
                            "fapfpfpau fapufafaa alsdkjfalsdjna asd fapfpfpau \n" +
                            "fapufafaa alsdkjfalsdjna asd fapfpfpau \n" +
                            "fapufafaa alsdkjfalsdjna asd fapfpfpau fapufafaa \n" +
                            "alsdkjfalsdjna asd fapfpfpau fapufafaa alsdkjfalsdjna \n" +
                            "asd fapfpfpau fapufafaa alsdkjfalsdjna asd fapfpfpau \n" +
                            "fapufafaa alsdkjfalsdjna asd fapfpfpau fapufafaa \n" +
                            "alsdkjfalsdjna asd fapfpfpau fapufafaa alsdkjfalsdjna \n" +
                            "asd fapfpfpau fapufafaa alsdkjfalsdjna asd \n" +
                            "fapfpfpau fapufafaa alsdkjfalsdjna asd \n" +
                            "fapfpfpau fapufafaa alsdkjfalsdjna asd \n" +
                            "fapfpfpau fapufafaa \n" +
                            "alsdkjfalsdjna asd \n" +
                            "fapfpfpau fapufafaa";

        final JScrollPane tap = new JScrollPane(new JTextArea(text));
        tap.setPreferredSize(new Dimension(200, 350));
        return tap;
    }
}
