package ISGC.up.edu.Comertial_Gerator.Window;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

public class Window {

    public static void main(String[] args) {
        // Create the main frame
        JFrame frame = new JFrame("Comertial Generator");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 400);

        // Create a panel to hold the components
        JPanel panel = new JPanel();
        frame.add(panel);

        // Create a text field to display the selected folder path
        JTextField textField = new JTextField(20);
        textField.setEditable(false); // Make it read-only
        textField.setBounds(50,100,100,100);
        panel.add(textField);

        // Create a button to open the folder picker dialog
        JButton pickFolderButton = new JButton("Pick Folder");
        pickFolderButton.setBounds(50,50,100,100);
        panel.add(pickFolderButton);

        // Create a button to activate the action
        JButton activateButton = new JButton("Activate");
        panel.add(activateButton);

        // Add action listener to the pick folder button
        pickFolderButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY); //Filter for only folders

                int option = fileChooser.showOpenDialog(frame);
                if (option == JFileChooser.APPROVE_OPTION) {
                    File selectedFolder = fileChooser.getSelectedFile();
                    textField.setText(selectedFolder.getAbsolutePath());
                }
            }
        });

        // Add action listener to the activate button
        activateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String folderPath = textField.getText();
                if (!folderPath.isEmpty()) {
                    // Perform your action here
                    JOptionPane.showMessageDialog(frame, "Action activated on folder: " + folderPath);

                } else {
                    JOptionPane.showMessageDialog(frame, "Please select a folder first.");
                }
            }
        });

        // Display the frame
        frame.setVisible(true);
    }
}