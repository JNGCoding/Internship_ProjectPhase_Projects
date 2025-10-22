package org.jngcoding.zipper.utility;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> createAndShowGui());
    }

    private static void createAndShowGui() {
        JFrame frame = new JFrame("Zip/Unzip Utility");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(700, 400);

        JPanel panel = new JPanel(new BorderLayout(8, 8));

        JTextArea status = new JTextArea();
        status.setEditable(false);
        status.setLineWrap(true);
        status.setWrapStyleWord(true);

        JPanel controls = new JPanel();
        controls.setLayout(new FlowLayout(FlowLayout.LEFT));

        JButton selectFilesBtn = new JButton("Select Files/Folders to Zip");
        JButton createZipBtn = new JButton("Create Zip");
        JButton selectZipBtn = new JButton("Select Zip to Unzip");
        JButton unzipBtn = new JButton("Unzip");

        controls.add(selectFilesBtn);
        controls.add(createZipBtn);
        controls.add(selectZipBtn);
        controls.add(unzipBtn);

        panel.add(controls, BorderLayout.NORTH);
        panel.add(new JScrollPane(status), BorderLayout.CENTER);

        // state
        List<File> selectedToZip = new ArrayList<>();
        final File[] selectedZipFile = new File[1];

        selectFilesBtn.addActionListener((ActionEvent e) -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setMultiSelectionEnabled(true);
            chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            int res = chooser.showOpenDialog(frame);
            if (res == JFileChooser.APPROVE_OPTION) {
                File[] files = chooser.getSelectedFiles();
                selectedToZip.clear();
                Collections.addAll(selectedToZip, files);
                status.append("Selected " + files.length + " item(s) to zip:\n");
                for (File f : files) status.append("  " + f.getAbsolutePath() + "\n");
            }
        });

        createZipBtn.addActionListener((ActionEvent e) -> {
            if (selectedToZip.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "No files/folders selected to zip.", "Warning", JOptionPane.WARNING_MESSAGE);
                return;
            }
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Save Zip File");
            chooser.setSelectedFile(new File("archive.zip"));
            int res = chooser.showSaveDialog(frame);
            if (res != JFileChooser.APPROVE_OPTION) return;
            File target = chooser.getSelectedFile();
            status.append("Creating zip: " + target.getAbsolutePath() + "\n");
            // long running -> background
            new SwingWorker<Void, String>() {
                @Override
                protected Void doInBackground() {
                    try {
                        ZipUtils.zipFiles(selectedToZip, target);
                        publish("Zip created: " + target.getAbsolutePath());
                    } catch (IOException ex) {
                        publish("Error creating zip: " + ex.getMessage());
                    }
                    return null;
                }

                @Override
                protected void process(List<String> chunks) {
                    for (String s : chunks) status.append(s + "\n");
                }
            }.execute();
        });

        selectZipBtn.addActionListener((ActionEvent e) -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(new FileNameExtensionFilter("Zip files", "zip"));
            int res = chooser.showOpenDialog(frame);
            if (res == JFileChooser.APPROVE_OPTION) {
                selectedZipFile[0] = chooser.getSelectedFile();
                status.append("Selected zip: " + selectedZipFile[0].getAbsolutePath() + "\n");
            }
        });

        unzipBtn.addActionListener((ActionEvent e) -> {
            if (selectedZipFile[0] == null) {
                JOptionPane.showMessageDialog(frame, "No zip file selected.", "Warning", JOptionPane.WARNING_MESSAGE);
                return;
            }
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setDialogTitle("Select target directory to extract");
            int res = chooser.showOpenDialog(frame);
            if (res != JFileChooser.APPROVE_OPTION) return;
            File targetDir = chooser.getSelectedFile();
            status.append("Unzipping to: " + targetDir.getAbsolutePath() + "\n");
            new SwingWorker<Void, String>() {
                @Override
                protected Void doInBackground() {
                    try {
                        ZipUtils.unzip(selectedZipFile[0], targetDir);
                        publish("Unzip completed to: " + targetDir.getAbsolutePath());
                    } catch (IOException ex) {
                        publish("Error unzipping: " + ex.getMessage());
                    }
                    return null;
                }

                @Override
                protected void process(List<String> chunks) {
                    for (String s : chunks) status.append(s + "\n");
                }
            }.execute();
        });

        frame.getContentPane().add(panel);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}