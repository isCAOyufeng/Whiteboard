import javax.swing.*;
import java.awt.*;
import java.rmi.RemoteException;

public class Whiteboard {
    public static JFrame frame;
    public static DrawWhiteBoard canvas;
    private static LoginDialog loginDialog;
    private static String username;


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
            frame = new JFrame("Whiteboard - " + username + (isAdmin ? " Admin" : ""));

            JPanel toolsPanel = new JPanel();
            String[] tools = {"Line", "Triangle", "Rectangle", "Oval", "FreeDraw", "Text", "Rubber"};
            JComboBox<String> toolSelector = new JComboBox<>(tools);
            toolSelector.addActionListener(e -> {
                String selected = (String) toolSelector.getSelectedItem();
                canvas.setTool(ToolType.valueOf(selected.toUpperCase()));
            });

            JButton colorButton = new JButton("Choose Color");
            JSlider eraserSlider = new JSlider(5, 50, 10);
            eraserSlider.setMajorTickSpacing(15);
            eraserSlider.setPaintTicks(true);
            eraserSlider.setPaintLabels(true);
            eraserSlider.setVisible(false);

            toolSelector.addActionListener(e -> {
                String selected = ((String) toolSelector.getSelectedItem()).toUpperCase();
                ToolType tool = ToolType.valueOf(selected);
                canvas.setTool(tool);

                if (tool == ToolType.RUBBER) {
                    colorButton.setVisible(false);
                    eraserSlider.setVisible(true);
                } else {
                    colorButton.setVisible(true);
                    eraserSlider.setVisible(false);
                }
                toolsPanel.revalidate();
                toolsPanel.repaint();
            });

            colorButton.addActionListener(e -> {
                Color color = JColorChooser.showDialog(frame, "Select Color", Color.BLACK);
                if (color != null) canvas.setColor(color);
            });

            eraserSlider.addChangeListener(e -> {
                canvas.setEraserSize(eraserSlider.getValue());
            });

            toolsPanel.add(new JLabel("Tool:"));
            toolsPanel.add(toolSelector);
            toolsPanel.add(colorButton);
            toolsPanel.add(eraserSlider);

            if (isAdmin) {
                JButton saveWhiteboard = new JButton("save whiteboard");
                JButton userList = new JButton("view users");
                toolsPanel.add(saveWhiteboard);
                toolsPanel.add(userList);
                // corresponding listeners
            }

            JButton exit = new JButton("exit");
            exit.addActionListener(e -> {
                if (isAdmin) {
                    managerExit();
                } else {
                    clientExit();
                    System.exit(0);
                }
            });
            toolsPanel.add(exit);

            frame.add(toolsPanel, BorderLayout.NORTH);
            frame.add(canvas, BorderLayout.CENTER);
            frame.pack();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setVisible(true);
        });
    }

    public static void clientExit() {
        try {
            loginDialog.getServerStub().removeClient(loginDialog.getClientStub());
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        frame.dispose();
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

    public static String getUsername() {
        return username;
    }
}
