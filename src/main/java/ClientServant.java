import javax.swing.*;
import java.awt.*;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class ClientServant extends UnicastRemoteObject implements WhiteboardClientStub {
    private String username;

    protected ClientServant(String username) throws RemoteException {
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
//        if (this.username.equals(username)) {
//            return;
//        }
        SwingUtilities.invokeLater(() -> {
            Whiteboard.updateChatArea(username, message);
        });
    }

    @Override
    public void receiveServerDownMessage() throws RemoteException {
        Whiteboard.clientExit(0);
        System.out.println("received server down message.");
        System.exit(0);
    }

    @Override
    public void receiveServerDownMessage(String message) throws RemoteException {
        Whiteboard.clientExit(1);
        System.out.println(message);

        new Thread(() -> {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.exit(0);
        }).start();
    }

    public String getUsername() {
        return username;
    }
}
