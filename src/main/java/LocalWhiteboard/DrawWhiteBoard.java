package LocalWhiteboard;

import Stubs.WhiteboardServerStub;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DrawWhiteBoard extends JPanel {
    private ToolType currentTool = ToolType.LINE;
    private Color currentColor = Color.BLACK;
    private Point startPoint, endPoint;
    private final List<Point> path = new ArrayList<>();
    private final BufferedImage canvasImage;
    private final Graphics2D g2;
    private int rubberSize = 10;
    private String username = null;
    private Point usernameLabelPoint = null;

    public DrawWhiteBoard(WhiteboardServerStub serverStub) {
        setPreferredSize(new Dimension(800, 600));
        canvasImage = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB);
        g2 = canvasImage.createGraphics();
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, 800, 600);
        g2.setColor(currentColor);

        List<DrawCommand> commandList = List.of();
        try {
            commandList = serverStub.getCommandList();
        } catch (RemoteException e) {
            System.err.println("failed fetching command list.");
        }

        for (DrawCommand command : commandList) {
            this.draw(command.getUsername(), command.getStartPoint(), command.getEndPoint(), ToolType.fromDrawCommandType(command.getType()), command.getPath(), command.getColor(), command.getRubberSize(), command.getText());
        }

        byte[] imageBytes = null;
        try {
            imageBytes = serverStub.getBaseImage();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        if (imageBytes != null) {
            ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes);
            BufferedImage image = null;
            try {
                image = ImageIO.read(bais);
                if (image != null) {
                    this.loadImage(image);
                }
            } catch (IOException e) {
                System.err.println("Failed to load image: " + e.getMessage());
                e.printStackTrace();
            }
        }

        MouseAdapter mouseAdapter = new MouseAdapter() {
            DrawCommand command;

            public void mousePressed(MouseEvent e) {
                startPoint = e.getPoint();
                if (currentTool == ToolType.FREEDRAW) {
                    path.add(startPoint);
                    g2.setColor(currentColor);
                    g2.fillOval(e.getX(), e.getY(), 2, 2);
                    repaint();
                }

                if (currentTool == ToolType.RUBBER) {
                    path.add(startPoint);
                }

                if (currentTool == ToolType.TEXT) {
                    String inputText = JOptionPane.showInputDialog("Enter text:");
                    if (inputText != null && !inputText.trim().isEmpty()) {
                        g2.setColor(currentColor);
                        g2.setFont(new Font("Arial", Font.PLAIN, 16));
                        g2.drawString(inputText, e.getX(), e.getY());
                        repaint();
                    }

                    // broadcast here
                    command = new DrawCommand(Whiteboard.getUsername(), ToolType.fromToolType(ToolType.TEXT), startPoint, startPoint, currentColor, inputText, rubberSize);
                    try {
                        serverStub.broadcastLocalChanges(command);
                    } catch (RemoteException ex) {
                        System.err.println("broadcast failed.");
                    }

                    DrawWhiteBoard.this.username = Whiteboard.getUsername();
                    DrawWhiteBoard.this.usernameLabelPoint = startPoint;
                    repaint();
                    Timer timer = new Timer(1000, e1 -> {
                        DrawWhiteBoard.this.username = null;
                        DrawWhiteBoard.this.usernameLabelPoint = null;
                        repaint();
                    });
                    timer.setRepeats(false);
                    timer.start();
                }
            }

            public void mouseDragged(MouseEvent e) {
                endPoint = e.getPoint();
                if (currentTool == ToolType.FREEDRAW || currentTool == ToolType.RUBBER) {
                    path.add(endPoint);
                    if (currentTool == ToolType.RUBBER) {
                        g2.setColor(Color.WHITE);
                        g2.setStroke(new BasicStroke(rubberSize));
                    } else {
                        g2.setColor(currentColor);
                        g2.setStroke(new BasicStroke(1));
                    }
                    g2.drawLine(startPoint.x, startPoint.y, endPoint.x, endPoint.y);
                    startPoint = endPoint;
                    repaint();
                }
            }

            public void mouseReleased(MouseEvent e) {
                endPoint = e.getPoint();
                if (currentTool == ToolType.RUBBER || currentTool == ToolType.FREEDRAW) {
                    path.add(endPoint);
                    if (currentTool == ToolType.RUBBER) {
                        command = new DrawCommand(Whiteboard.getUsername(), ToolType.fromToolType(ToolType.RUBBER), path, currentColor, rubberSize);
                    } else {
                        command = new DrawCommand(Whiteboard.getUsername(), ToolType.fromToolType(ToolType.FREEDRAW), path, currentColor, rubberSize);
                    }
                    // broadcast here
                    try {
                        serverStub.broadcastLocalChanges(command);
                    } catch (RemoteException ex) {
                        System.err.println("broadcast failed.");
                    }
                    path.clear();
                    DrawWhiteBoard.this.username = Whiteboard.getUsername();
                    DrawWhiteBoard.this.usernameLabelPoint = endPoint;
                    repaint();
                    Timer timer = new Timer(1000, e1 -> {
                        DrawWhiteBoard.this.username = null;
                        DrawWhiteBoard.this.usernameLabelPoint = null;
                        repaint();
                    });
                    timer.setRepeats(false);
                    timer.start();
                    return;
                }

                g2.setColor(currentColor);
                g2.setStroke(new BasicStroke(1));
                switch (currentTool) {
                    case LINE:
                        g2.drawLine(startPoint.x, startPoint.y, endPoint.x, endPoint.y);
                        break;
                    case TRIANGLE:
                        int x1 = startPoint.x;
                        int y1 = endPoint.y;
                        int x2 = (startPoint.x + endPoint.x) / 2;
                        int y2 = startPoint.y;
                        int x3 = endPoint.x;
                        int y3 = endPoint.y;

                        int[] xPoints = {x1, x2, x3};
                        int[] yPoints = {y1, y2, y3};
                        g2.drawPolygon(xPoints, yPoints, 3);
                        break;
                    case RECTANGLE:
                        g2.drawRect(Math.min(startPoint.x, endPoint.x),
                                Math.min(startPoint.y, endPoint.y),
                                Math.abs(startPoint.x - endPoint.x),
                                Math.abs(startPoint.y - endPoint.y));
                        break;
                    case OVAL:
                        g2.drawOval(Math.min(startPoint.x, endPoint.x),
                                Math.min(startPoint.y, endPoint.y),
                                Math.abs(startPoint.x - endPoint.x),
                                Math.abs(startPoint.y - endPoint.y));
                        break;
                    default:
                        break;
                }
                repaint();

                DrawWhiteBoard.this.username = Whiteboard.getUsername();
                DrawWhiteBoard.this.usernameLabelPoint = endPoint;
                repaint();
                Timer timer = new Timer(1000, e1 -> {
                    DrawWhiteBoard.this.username = null;
                    DrawWhiteBoard.this.usernameLabelPoint = null;
                    repaint();
                });
                timer.setRepeats(false);
                timer.start();

                command = new DrawCommand(Whiteboard.getUsername(), ToolType.fromToolType(currentTool), startPoint, endPoint, currentColor, null, rubberSize);
                // broadcast here
                try {
                    serverStub.broadcastLocalChanges(command);
                } catch (RemoteException ex) {
                    System.err.println("broadcast failed.");
                }
            }
        };

        addMouseListener(mouseAdapter);
        addMouseMotionListener(mouseAdapter);
    }

    public void setTool(ToolType tool) {
        this.currentTool = tool;
    }

    public void setColor(Color color) {
        this.currentColor = color;
    }

    public void setRubberSize(int size) {
        this.rubberSize = size;
    }

    public void draw(String username, Point startPoint, Point endPoint, ToolType toolType, List<Point> path, Color color, int rubberSize, String text) {
        g2.setColor(color);
        g2.setStroke(new BasicStroke(1));
        this.setRubberSize(rubberSize);
        switch (toolType) {
            case LINE -> g2.drawLine(startPoint.x, startPoint.y, endPoint.x, endPoint.y);
            case TRIANGLE -> {
                int x1 = startPoint.x;
                int y1 = endPoint.y;
                int x2 = (startPoint.x + endPoint.x) / 2;
                int y2 = startPoint.y;
                int x3 = endPoint.x;
                int y3 = endPoint.y;

                int[] xPoints = {x1, x2, x3};
                int[] yPoints = {y1, y2, y3};
                g2.drawPolygon(xPoints, yPoints, 3);
            }
            case RECTANGLE -> g2.drawRect(Math.min(startPoint.x, endPoint.x),
                        Math.min(startPoint.y, endPoint.y),
                        Math.abs(startPoint.x - endPoint.x),
                        Math.abs(startPoint.y - endPoint.y));
            case OVAL -> g2.drawOval(Math.min(startPoint.x, endPoint.x),
                    Math.min(startPoint.y, endPoint.y),
                    Math.abs(startPoint.x - endPoint.x),
                    Math.abs(startPoint.y - endPoint.y));
            case FREEDRAW -> {
                for (int i = 1; i < path.size(); i++) {
                    Point p1 = path.get(i - 1);
                    Point p2 = path.get(i);
                    g2.drawLine(p1.x, p1.y, p2.x, p2.y);
                }
                repaint();
            }
            case RUBBER -> {
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(this.rubberSize));
                for (int i = 1; i < path.size(); i++) {
                    Point p1 = path.get(i - 1);
                    Point p2 = path.get(i);
                    g2.drawLine(p1.x, p1.y, p2.x, p2.y);
                }
                repaint();
            }
            case TEXT -> {
                if (text != null && !text.trim().isEmpty()) {
                    g2.setFont(new Font("Arial", Font.PLAIN, 16));
                    g2.drawString(text, startPoint.x, startPoint.y);
                    repaint();
                }
            }
        }

        DrawWhiteBoard.this.username = username;
        DrawWhiteBoard.this.usernameLabelPoint = (toolType == ToolType.TEXT) ? startPoint : endPoint;
        repaint();
        Timer timer = new Timer(1000, e1 -> {
            DrawWhiteBoard.this.username = null;
            DrawWhiteBoard.this.usernameLabelPoint = null;
            repaint();
        });
        timer.setRepeats(false);
        timer.start();
    }

    public void clearCanvas() {
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.setColor(currentColor);
        repaint();
    }

    public void loadImage(BufferedImage image) {
        g2.drawImage(image, 0, 0, 800, 600, null);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(canvasImage, 0, 0, null);

        if (username != null && usernameLabelPoint != null) {
            g.setColor(Color.RED);
            g.setFont(new Font("Arial", Font.BOLD, 12));
            g.drawString(username, usernameLabelPoint.x + 5, usernameLabelPoint.y - 5);
        }
    }
}
