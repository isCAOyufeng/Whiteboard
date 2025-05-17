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

    public ClientServant(String username) throws RemoteException {
        this.username = username;
    }

    @Override
    public void receiveCommand(DrawCommand command) throws RemoteException {
        String username = command.getUsername();
        Point startPoint = command.getStartPoint();
        Point endPoint = command.getEndPoint();
        ToolType currentTool = ToolType.fromDrawCommandType(command.getType());
        Color color = command.getColor();
        int rubberSize = command.getEraserSize();
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
        System.out.println("received server down message.");

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
        System.out.println(message);

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
    public void receiveClearCanvasMessage() throws RemoteException {
        Whiteboard.canvas.clearCanvas();
    }
}
