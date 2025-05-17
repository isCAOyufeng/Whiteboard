package LocalWhiteboard;

import Stubs.WhiteboardClientStub;
import Stubs.WhiteboardServerStub;
import Login.LoginDialog;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.PDFRenderer;
import LocalWhiteboard.ToolType;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;

public class Whiteboard {
    public static JFrame frame;
    public static DrawWhiteBoard canvas;
    private static LoginDialog loginDialog;
    private static String username;
    private static JTextArea chatArea;
    private static JTextField messageField;
    private static File currentFile = null;

    public static void main(String[] args) {
            loginDialog = new LoginDialog();
            while (true) {
                username = loginDialog.getUsername();
                if (loginDialog.isSucceed()) {
                    break;
                }
            }

            System.out.println("success");

            canvas = new DrawWhiteBoard(loginDialog.getServerStub());

            Whiteboard.launchWhiteboardGUI(username, loginDialog.isAdmin());

            System.out.println("whiteboard window created.");
    }

    public static void launchWhiteboardGUI(String username, boolean isAdmin) {
        SwingUtilities.invokeLater(() -> {
            frame = new JFrame("LocalWhiteboard.Whiteboard - " + username + (isAdmin ? " Admin" : ""));
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            JPanel toolsPanel = new JPanel();
            String[] tools = {"Line", "Triangle", "Rectangle", "Oval", "FreeDraw", "Text", "Rubber"};
            JComboBox<String> toolSelector = new JComboBox<>(tools);
            JButton colorButton = new JButton("Choose Color");
            JSlider eraserSlider = new JSlider(5, 50, 10);

            eraserSlider.setMajorTickSpacing(15);
            eraserSlider.setPaintTicks(true);
            eraserSlider.setPaintLabels(true);
            eraserSlider.setVisible(false);

            toolSelector.addActionListener(e -> {
                String selected = (String) toolSelector.getSelectedItem();
                ToolType tool = ToolType.valueOf(selected.toUpperCase());
                canvas.setTool(tool);

                colorButton.setVisible(tool != ToolType.RUBBER);
                eraserSlider.setVisible(tool == ToolType.RUBBER);
                toolsPanel.revalidate();
                toolsPanel.repaint();
            });

            colorButton.addActionListener(e -> {
                Color color = JColorChooser.showDialog(frame, "Select Color", Color.BLACK);
                if (color != null) canvas.setColor(color);
            });

            eraserSlider.addChangeListener(e -> canvas.setEraserSize(eraserSlider.getValue()));

            toolsPanel.add(new JLabel("Tool:"));
            toolsPanel.add(toolSelector);
            toolsPanel.add(colorButton);
            toolsPanel.add(eraserSlider);

            JPanel chatPanel = new JPanel(new BorderLayout());
            chatPanel.setPreferredSize(new Dimension(200, 600));

            chatArea = new JTextArea();
            chatArea.setEditable(false);
            chatArea.setLineWrap(true);
            chatArea.setWrapStyleWord(true);
            JScrollPane chatScrollPane = new JScrollPane(chatArea);

            JPanel messagePanel = new JPanel(new BorderLayout());
            messageField = new JTextField();
            JButton sendButton = new JButton("Send");

            ActionListener sendMessage = e -> {
                String message = messageField.getText().trim();
                if (!message.isEmpty()) {
                    try {
                        loginDialog.getServerStub().broadcastChatMessages(username, message);
                        messageField.setText("");
                    } catch (RemoteException ex) {
                        JOptionPane.showMessageDialog(frame,
                                "Error sending message: " + ex.getMessage(),
                                "Error",
                                JOptionPane.ERROR_MESSAGE);
                    }
                }
            };

            sendButton.addActionListener(sendMessage);
            messageField.addActionListener(sendMessage);

            messagePanel.add(messageField, BorderLayout.CENTER);
            messagePanel.add(sendButton, BorderLayout.EAST);

            chatPanel.add(chatScrollPane, BorderLayout.CENTER);
            chatPanel.add(messagePanel, BorderLayout.SOUTH);

            if (isAdmin) {
                JButton userList = new JButton("view users");
                toolsPanel.add(userList);
                JMenuBar menuBar = new JMenuBar();
                JMenu fileMenu = new JMenu("File");

                JMenuItem newItem = new JMenuItem("New");
                newItem.addActionListener(e -> {
                    canvas.clearCanvas();
                    try {
                        loginDialog.getServerStub().clearCommandList();
                        loginDialog.getServerStub().sendClearCanvasMessage();
                    } catch (RemoteException ex) {
                        ex.printStackTrace();
                    }
                });

                JMenuItem openItem = new JMenuItem("Open");
                openItem.addActionListener(e -> {
                    JFileChooser fileChooser = new JFileChooser();
                    fileChooser.setFileFilter(new FileNameExtensionFilter("PDF files", "pdf"));

                    if (fileChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                        try {
                            File file = fileChooser.getSelectedFile();
                            PDDocument document = PDDocument.load(file);
                            PDPage page = document.getPage(0);

                            PDFRenderer pdfRenderer = new PDFRenderer(document);
                            BufferedImage image = pdfRenderer.renderImageWithDPI(0, 300);
                            canvas.loadImage(image);

                            document.close();
                        } catch (IOException ex) {
                            JOptionPane.showMessageDialog(frame,
                                    "Error opening file: " + ex.getMessage(),
                                    "Open Failed",
                                    JOptionPane.ERROR_MESSAGE);
                        }
                    }
                });

                JMenuItem saveItem = new JMenuItem("Save");
                saveItem.addActionListener(e -> {
                    if (currentFile != null) {
                        saveWhiteboardToPDF(currentFile);
                    } else {
                        saveAsWhiteboard();
                    }
                });

                JMenuItem saveAsItem = new JMenuItem("Save As");
                saveAsItem.addActionListener(e -> saveAsWhiteboard());

                JMenuItem closeItem = new JMenuItem("Close");
                closeItem.addActionListener(e -> managerExit());

                fileMenu.add(newItem);
                fileMenu.add(openItem);
                fileMenu.add(saveItem);
                fileMenu.add(saveAsItem);
                fileMenu.addSeparator();
                fileMenu.add(closeItem);

                menuBar.add(fileMenu);
                frame.setJMenuBar(menuBar);

                userList.addActionListener(e -> {
                    try {
                        java.util.List<WhiteboardClientStub> clients = loginDialog.getServerStub().getClientList();

                        JDialog userDialog = new JDialog(frame, "Connected Users", true);
                        userDialog.setLayout(new BorderLayout());

                        DefaultListModel<String> listModel = new DefaultListModel<>();
                        for (WhiteboardClientStub client : clients) {
                            listModel.addElement(client.getUsername());
                        }

                        JList<String> userJList = new JList<>(listModel);
                        JScrollPane scrollPane = new JScrollPane(userJList);

                        JButton kickButton = new JButton("Kick Selected User");
                        kickButton.addActionListener(event -> {
                            int selectedIndex = userJList.getSelectedIndex();
                            if (selectedIndex == 0) {
                                JOptionPane.showMessageDialog(userDialog,
                                        "Error kicking user: you cannot kick out yourself.",
                                        "Error",
                                        JOptionPane.ERROR_MESSAGE);
                            }
                            if (selectedIndex > 0) {
                                try {
                                    WhiteboardClientStub clientToKick = clients.get(selectedIndex);
                                    loginDialog.getServerStub().removeClient(clientToKick);
                                    clientToKick.receiveServerDownMessage("kicked out");
                                    listModel.remove(selectedIndex);
                                } catch (RemoteException ex) {
                                    JOptionPane.showMessageDialog(userDialog,
                                            "Error kicking user: " + ex.getMessage(),
                                            "Error",
                                            JOptionPane.ERROR_MESSAGE);
                                }
                            }
                        });

                        userDialog.add(scrollPane, BorderLayout.CENTER);
                        userDialog.add(kickButton, BorderLayout.SOUTH);

                        userDialog.setSize(300, 400);
                        userDialog.setLocationRelativeTo(frame);
                        userDialog.setVisible(true);

                    } catch (RemoteException ex) {
                        JOptionPane.showMessageDialog(frame,
                                "Error getting user list: " + ex.getMessage(),
                                "Error",
                                JOptionPane.ERROR_MESSAGE);
                    }
                });
            }

            JButton exit = new JButton("exit");
            exit.addActionListener(e -> {
                if (isAdmin) {
                    managerExit();
                } else {
                    clientExit(0);
                    System.exit(0);
                }
            });
            toolsPanel.add(exit);

            JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, canvas, chatPanel);
            splitPane.setResizeWeight(0.8);

            frame.setLayout(new BorderLayout());
            frame.add(toolsPanel, BorderLayout.NORTH);
            frame.add(splitPane, BorderLayout.CENTER);

            frame.setSize(1000, 600);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    public static void clientExit(int indicator) {
        if (indicator == 1) {
            new Thread(() -> {
                JOptionPane.showMessageDialog(
                        frame,
                        "You have been kicked out by the admin. Shutting down in 3 seconds.",
                        "Kicked Out",
                        JOptionPane.WARNING_MESSAGE);
            }).start();
        } else if (indicator == 2) {
            new Thread(() -> {
                JOptionPane.showMessageDialog(
                        frame,
                        "LocalWhiteboard.Whiteboard closed by manager. Shutting down in 3 seconds.",
                        "Server Down",
                        JOptionPane.WARNING_MESSAGE);
            }).start();
        }

        try {
            loginDialog.getServerStub().removeClient(loginDialog.getClientStub());
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public static void managerExit() {
        System.out.println("managerExit called.");

        new Thread(() -> {
            try {
                loginDialog.getServerStub().sendServerDownMessage();
                System.out.println("sendServerDownMessage done.");

                loginDialog.getServerStub().shutDownServer(loginDialog.getIp(), loginDialog.getPort());
                System.out.println("shutDownServer done.");
            } catch (RemoteException ex) {
                System.err.println("Exception in background thread:");
                ex.printStackTrace();
            }

            SwingUtilities.invokeLater(() -> {
                System.out.println("Disposing frame...");
                frame.dispose();
                System.exit(0);
            });
        }).start();
    }

    public static void updateChatArea(String username, String message) {
        if (chatArea != null) {
            chatArea.append(username + ": " + message + "\n");
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        }
    }

    public static String getUsername() {
        return username;
    }

    private static void saveWhiteboardToPDF(File file) {
        try {
            PDDocument document = new PDDocument();
            PDPage page = new PDPage(new PDRectangle(canvas.getWidth(), canvas.getHeight()));
            document.addPage(page);

            BufferedImage image = new BufferedImage(
                    canvas.getWidth(),
                    canvas.getHeight(),
                    BufferedImage.TYPE_INT_RGB
            );
            canvas.paint(image.getGraphics());

            PDImageXObject pdImage = LosslessFactory.createFromImage(document, image);
            PDPageContentStream contentStream = new PDPageContentStream(document, page);
            contentStream.drawImage(pdImage, 0, 0);
            contentStream.close();

            document.save(file);
            document.close();
            currentFile = file;

            JOptionPane.showMessageDialog(frame,
                    "LocalWhiteboard.Whiteboard has been saved as PDF: " + file.getAbsolutePath(),
                    "Save Successful",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(frame,
                    "Error saving PDF: " + ex.getMessage(),
                    "Save Failed",
                    JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    private static void saveAsWhiteboard() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setSelectedFile(new File("whiteboard.pdf"));

        if (fileChooser.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (!file.getName().toLowerCase().endsWith(".pdf")) {
                file = new File(file.getAbsolutePath() + ".pdf");
            }
            saveWhiteboardToPDF(file);
        }
    }
}
