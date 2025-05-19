package Servants;

import LocalWhiteboard.DrawCommand;
import LocalWhiteboard.ToolType;
import LocalWhiteboard.Whiteboard;
import Stubs.WhiteboardClientStub;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class ClientServant extends UnicastRemoteObject implements WhiteboardClientStub {
    private final String username;
    private final boolean isAdmin;

    public ClientServant(String username, boolean isAdmin) throws RemoteException {
        this.username = username;
        this.isAdmin = isAdmin;
    }

    @Override
    public boolean receiveVerification(String username) throws RemoteException {
        return Whiteboard.showVerificationDialog(username);
    }

    @Override
    public void receiveCommand(DrawCommand command) throws RemoteException {
        String username = command.getUsername();
        Point startPoint = command.getStartPoint();
        Point endPoint = command.getEndPoint();
        ToolType currentTool = ToolType.fromDrawCommandType(command.getType());
        Color color = command.getColor();
        int rubberSize = command.getRubberSize();
        java.util.List<Point> path = command.getPath();
        String text = command.getText();

        Whiteboard.canvas.draw(username, startPoint, endPoint, currentTool, path, color, rubberSize, text);
    }

    @Override
    public void receiveChatMessage(String username, String message) throws RemoteException {
        SwingUtilities.invokeLater(() -> {
            Whiteboard.updateChatArea(username, message);
        });
    }

    @Override
    public void receiveServerDownMessage() throws RemoteException {
        Whiteboard.clientExit(2);

        // wait for 3 seconds, then shut down the program by force
        new Thread(() -> {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.exit(0);
        }).start();
    }

    @Override
    public void receiveServerDownMessage(String message) throws RemoteException {
        Whiteboard.clientExit(1);

        // wait for 3 seconds, then shut down the program by force
        new Thread(() -> {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.exit(0);
        }).start();
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAdmin() throws RemoteException {
        return isAdmin;
    }

    @Override
    public void receiveClearCanvasMessage() throws RemoteException {
        Whiteboard.canvas.clearCanvas();
    }

    @Override
    public void receiveImage(byte[] imageBytes) throws RemoteException {
        ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes);
        try {
            BufferedImage image = ImageIO.read(bais);
            System.out.println(image == null);
            Whiteboard.canvas.loadImage(image);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
