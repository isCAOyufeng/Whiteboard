import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.PrinterJob;
import java.io.File;
import java.rmi.RemoteException;

public class Whiteboard {
    public static JFrame frame;
    public static DrawWhiteBoard canvas;
    private static LoginDialog loginDialog;
    private static String username;
    private static JTextArea chatArea;
    private static JTextField messageField;

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

            Whiteboard.launchWhiteboardGUI(username, loginDialog.getIp(), loginDialog.getPort(), loginDialog.isAdmin());

            System.out.println("whiteboard window created.");
    }

    public static void launchWhiteboardGUI(String username, String ip, int port, boolean isAdmin) {
        SwingUtilities.invokeLater(() -> {
            // 创建主窗口
            frame = new JFrame("Whiteboard - " + username + (isAdmin ? " Admin" : ""));
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            // 创建工具面板
            JPanel toolsPanel = new JPanel();
            String[] tools = {"Line", "Triangle", "Rectangle", "Oval", "FreeDraw", "Text", "Rubber"};
            JComboBox<String> toolSelector = new JComboBox<>(tools);
            JButton colorButton = new JButton("Choose Color");
            JSlider eraserSlider = new JSlider(5, 50, 10);

            // 配置橡皮擦滑块
            eraserSlider.setMajorTickSpacing(15);
            eraserSlider.setPaintTicks(true);
            eraserSlider.setPaintLabels(true);
            eraserSlider.setVisible(false);

            // 工具选择事件
            toolSelector.addActionListener(e -> {
                String selected = (String) toolSelector.getSelectedItem();
                ToolType tool = ToolType.valueOf(selected.toUpperCase());
                canvas.setTool(tool);

                colorButton.setVisible(tool != ToolType.RUBBER);
                eraserSlider.setVisible(tool == ToolType.RUBBER);
                toolsPanel.revalidate();
                toolsPanel.repaint();
            });

            // 颜色选择事件
            colorButton.addActionListener(e -> {
                Color color = JColorChooser.showDialog(frame, "Select Color", Color.BLACK);
                if (color != null) canvas.setColor(color);
            });

            // 橡皮擦大小事件
            eraserSlider.addChangeListener(e -> canvas.setEraserSize(eraserSlider.getValue()));

            // 添加工具面板组件
            toolsPanel.add(new JLabel("Tool:"));
            toolsPanel.add(toolSelector);
            toolsPanel.add(colorButton);
            toolsPanel.add(eraserSlider);

            // 创建聊天面板
            JPanel chatPanel = new JPanel(new BorderLayout());
            chatPanel.setPreferredSize(new Dimension(200, 600));

            // 创建聊天区域
            chatArea = new JTextArea();
            chatArea.setEditable(false);
            chatArea.setLineWrap(true);
            chatArea.setWrapStyleWord(true);
            JScrollPane chatScrollPane = new JScrollPane(chatArea);

            // 创建消息输入区域
            JPanel messagePanel = new JPanel(new BorderLayout());
            messageField = new JTextField();
            JButton sendButton = new JButton("Send");

            // 发送消息事件
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

            // 添加发送消息事件监听
            sendButton.addActionListener(sendMessage);
            messageField.addActionListener(sendMessage);

            // 组装消息面板
            messagePanel.add(messageField, BorderLayout.CENTER);
            messagePanel.add(sendButton, BorderLayout.EAST);

            // 组装聊天面板
            chatPanel.add(chatScrollPane, BorderLayout.CENTER);
            chatPanel.add(messagePanel, BorderLayout.SOUTH);

            // 如果是管理员，添加管理功能
            if (isAdmin) {
                JButton saveWhiteboard = new JButton("save whiteboard");
                JButton userList = new JButton("view users");
                toolsPanel.add(saveWhiteboard);
                toolsPanel.add(userList);

                // 保存白板事件
                saveWhiteboard.addActionListener(e -> {
                    JFileChooser fileChooser = new JFileChooser();
                    fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                    fileChooser.setSelectedFile(new File("whiteboard.pdf"));

                    if (fileChooser.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
                        try {
                            File file = fileChooser.getSelectedFile();
                            if (!file.getName().toLowerCase().endsWith(".pdf")) {
                                file = new File(file.getAbsolutePath() + ".pdf");
                            }

                            // Create a new document
                            PDDocument document = new PDDocument();
                            PDPage page = new PDPage(new PDRectangle(canvas.getWidth(), canvas.getHeight()));
                            document.addPage(page);

                            // Create the image for the PDF
                            BufferedImage image = new BufferedImage(
                                    canvas.getWidth(),
                                    canvas.getHeight(),
                                    BufferedImage.TYPE_INT_RGB
                            );
                            canvas.paint(image.getGraphics());

                            // Convert the image to PDF
                            PDImageXObject pdImage = LosslessFactory.createFromImage(document, image);
                            PDPageContentStream contentStream = new PDPageContentStream(document, page);
                            contentStream.drawImage(pdImage, 0, 0);
                            contentStream.close();

                            // Save the PDF
                            document.save(file);
                            document.close();

                            JOptionPane.showMessageDialog(frame,
                                    "Whiteboard has been saved as PDF: " + file.getAbsolutePath(),
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
                });

                // 用户列表事件
                userList.addActionListener(e -> {
                    try {
                        // 获取所有客户端
                        java.util.List<WhiteboardClientStub> clients = loginDialog.getServerStub().getClientList();

                        // 创建用户列表对话框
                        JDialog userDialog = new JDialog(frame, "Connected Users", true);
                        userDialog.setLayout(new BorderLayout());

                        // 创建用户列表模型
                        DefaultListModel<String> listModel = new DefaultListModel<>();
                        for (WhiteboardClientStub client : clients) {
                            listModel.addElement(client.getUsername());
                        }

                        // 创建用户列表
                        JList<String> userJList = new JList<>(listModel);
                        JScrollPane scrollPane = new JScrollPane(userJList);

                        // 创建踢出按钮
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
                                    System.out.println("executed.");
                                    clientToKick.receiveServerDownMessage("kicked out");
                                    System.out.println("executed.");
                                    listModel.remove(selectedIndex);
                                    System.out.println("executed.");
                                } catch (RemoteException ex) {
                                    JOptionPane.showMessageDialog(userDialog,
                                            "Error kicking user: " + ex.getMessage(),
                                            "Error",
                                            JOptionPane.ERROR_MESSAGE);
                                }
                            }
                        });

                        // 添加组件到对话框
                        userDialog.add(scrollPane, BorderLayout.CENTER);
                        userDialog.add(kickButton, BorderLayout.SOUTH);

                        // 设置对话框大小和位置
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

            // 创建分割面板
            JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, canvas, chatPanel);
            splitPane.setResizeWeight(0.8);

            // 设置窗口布局
            frame.setLayout(new BorderLayout());
            frame.add(toolsPanel, BorderLayout.NORTH);
            frame.add(splitPane, BorderLayout.CENTER);

            // 配置窗口
            frame.setSize(1000, 600);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

//    public static void launchWhiteboardGUI(String username, String ip, int port, boolean isAdmin) {
//        SwingUtilities.invokeLater(() -> {
//            frame = new JFrame("Whiteboard - " + username + (isAdmin ? " Admin" : ""));
//
//            JPanel toolsPanel = new JPanel();
//            String[] tools = {"Line", "Triangle", "Rectangle", "Oval", "FreeDraw", "Text", "Rubber"};
//            JComboBox<String> toolSelector = new JComboBox<>(tools);
//            toolSelector.addActionListener(e -> {
//                String selected = (String) toolSelector.getSelectedItem();
//                canvas.setTool(ToolType.valueOf(selected.toUpperCase()));
//            });
//
//            JButton colorButton = new JButton("Choose Color");
//            JSlider eraserSlider = new JSlider(5, 50, 10);
//            eraserSlider.setMajorTickSpacing(15);
//            eraserSlider.setPaintTicks(true);
//            eraserSlider.setPaintLabels(true);
//            eraserSlider.setVisible(false);
//
//            toolSelector.addActionListener(e -> {
//                String selected = ((String) toolSelector.getSelectedItem()).toUpperCase();
//                ToolType tool = ToolType.valueOf(selected);
//                canvas.setTool(tool);
//
//                if (tool == ToolType.RUBBER) {
//                    colorButton.setVisible(false);
//                    eraserSlider.setVisible(true);
//                } else {
//                    colorButton.setVisible(true);
//                    eraserSlider.setVisible(false);
//                }
//                toolsPanel.revalidate();
//                toolsPanel.repaint();
//            });
//
//            colorButton.addActionListener(e -> {
//                Color color = JColorChooser.showDialog(frame, "Select Color", Color.BLACK);
//                if (color != null) canvas.setColor(color);
//            });
//
//            eraserSlider.addChangeListener(e -> {
//                canvas.setEraserSize(eraserSlider.getValue());
//            });
//
//            toolsPanel.add(new JLabel("Tool:"));
//            toolsPanel.add(toolSelector);
//            toolsPanel.add(colorButton);
//            toolsPanel.add(eraserSlider);
//
//            if (isAdmin) {
//                JButton saveWhiteboard = new JButton("save whiteboard");
//                JButton userList = new JButton("view users");
//                toolsPanel.add(saveWhiteboard);
//                toolsPanel.add(userList);
//
//                saveWhiteboard.addActionListener(e -> {
//                    JFileChooser fileChooser = new JFileChooser();
//                    fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
//                    fileChooser.setSelectedFile(new File("whiteboard.pdf"));
//
//                    if (fileChooser.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
//                        try {
//                            File file = fileChooser.getSelectedFile();
//                            if (!file.getName().toLowerCase().endsWith(".pdf")) {
//                                file = new File(file.getAbsolutePath() + ".pdf");
//                            }
//
//                            // Create a new document
//                            PDDocument document = new PDDocument();
//                            PDPage page = new PDPage(new PDRectangle(canvas.getWidth(), canvas.getHeight()));
//                            document.addPage(page);
//
//                            // Create the image for the PDF
//                            BufferedImage image = new BufferedImage(
//                                    canvas.getWidth(),
//                                    canvas.getHeight(),
//                                    BufferedImage.TYPE_INT_RGB
//                            );
//                            canvas.paint(image.getGraphics());
//
//                            // Convert the image to PDF
//                            PDImageXObject pdImage = LosslessFactory.createFromImage(document, image);
//                            PDPageContentStream contentStream = new PDPageContentStream(document, page);
//                            contentStream.drawImage(pdImage, 0, 0);
//                            contentStream.close();
//
//                            // Save the PDF
//                            document.save(file);
//                            document.close();
//
//                            JOptionPane.showMessageDialog(frame,
//                                    "Whiteboard has been saved as PDF: " + file.getAbsolutePath(),
//                                    "Save Successful",
//                                    JOptionPane.INFORMATION_MESSAGE);
//                        } catch (Exception ex) {
//                            JOptionPane.showMessageDialog(frame,
//                                    "Error saving PDF: " + ex.getMessage(),
//                                    "Save Failed",
//                                    JOptionPane.ERROR_MESSAGE);
//                            ex.printStackTrace();
//                        }
//                    }
//                });
//
//                userList.addActionListener(e -> {
//                    try {
//                        // 获取所有客户端
//                        java.util.List<WhiteboardClientStub> clients = loginDialog.getServerStub().getClientList();
//
//                        // 创建用户列表对话框
//                        JDialog userDialog = new JDialog(frame, "Connected Users", true);
//                        userDialog.setLayout(new BorderLayout());
//
//                        // 创建用户列表模型
//                        DefaultListModel<String> listModel = new DefaultListModel<>();
//                        for (WhiteboardClientStub client : clients) {
//                            listModel.addElement(client.getUsername());
//                        }
//
//                        // 创建用户列表
//                        JList<String> userJList = new JList<>(listModel);
//                        JScrollPane scrollPane = new JScrollPane(userJList);
//
//                        // 创建踢出按钮
//                        JButton kickButton = new JButton("Kick Selected User");
//                        kickButton.addActionListener(event -> {
//                            int selectedIndex = userJList.getSelectedIndex();
//                            if (selectedIndex == 0) {
//                                JOptionPane.showMessageDialog(userDialog,
//                                        "Error kicking user: you cannot kick out yourself.",
//                                        "Error",
//                                        JOptionPane.ERROR_MESSAGE);
//                            }
//                            if (selectedIndex > 0) {
//                                try {
//                                    WhiteboardClientStub clientToKick = clients.get(selectedIndex);
//                                    loginDialog.getServerStub().removeClient(clientToKick);
//                                    System.out.println("executed.");
//                                    clientToKick.receiveServerDownMessage("kicked out");
//                                    System.out.println("executed.");
//                                    listModel.remove(selectedIndex);
//                                    System.out.println("executed.");
//                                } catch (RemoteException ex) {
//                                    JOptionPane.showMessageDialog(userDialog,
//                                            "Error kicking user: " + ex.getMessage(),
//                                            "Error",
//                                            JOptionPane.ERROR_MESSAGE);
//                                }
//                            }
//                        });
//
//                        // 添加组件到对话框
//                        userDialog.add(scrollPane, BorderLayout.CENTER);
//                        userDialog.add(kickButton, BorderLayout.SOUTH);
//
//                        // 设置对话框大小和位置
//                        userDialog.setSize(300, 400);
//                        userDialog.setLocationRelativeTo(frame);
//                        userDialog.setVisible(true);
//
//                    } catch (RemoteException ex) {
//                        JOptionPane.showMessageDialog(frame,
//                                "Error getting user list: " + ex.getMessage(),
//                                "Error",
//                                JOptionPane.ERROR_MESSAGE);
//                    }
//                });
//            }
//
//            JButton exit = new JButton("exit");
//            exit.addActionListener(e -> {
//                if (isAdmin) {
//                    managerExit();
//                } else {
//                    clientExit(0);
//                    System.exit(0);
//                }
//            });
//            toolsPanel.add(exit);
//
//            frame.add(toolsPanel, BorderLayout.NORTH);
//            frame.add(canvas, BorderLayout.CENTER);
//            frame.pack();
//            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//            frame.setVisible(true);
//        });
//    }

    public static void clientExit(int indicator) {
        if (indicator == 1) {
            // 创建一个新线程来显示对话框，这样不会阻塞主线程
            new Thread(() -> {
                JOptionPane.showMessageDialog(
                        frame,
                        "You have been kicked out by the admin.",
                        "Kicked Out",
                        JOptionPane.WARNING_MESSAGE);
            }).start();

            // 设置一个定时器，3秒后自动关闭客户端
//            Timer timer = new Timer(8000, e -> {
//                try {
//                    loginDialog.getServerStub().removeClient(loginDialog.getClientStub());
//                } catch (RemoteException ex) {
//                    ex.printStackTrace();
//                }
//                frame.dispose();
//            });
//            timer.setRepeats(false);
//            timer.start();
        } else {
            try {
                loginDialog.getServerStub().removeClient(loginDialog.getClientStub());
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            frame.dispose();
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
            // 自动滚动到最新消息
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        }
    }

    public static String getUsername() {
        return username;
    }
}
