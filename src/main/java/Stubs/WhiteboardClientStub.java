package Stubs;

import LocalWhiteboard.DrawCommand;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface WhiteboardClientStub extends Remote {
    boolean receiveVerification(String username) throws RemoteException;
    void receiveCommand(DrawCommand command) throws RemoteException;
    void receiveChatMessage(String username, String message) throws RemoteException;
    void receiveServerDownMessage() throws RemoteException;
    void receiveServerDownMessage(String message) throws RemoteException;
    String getUsername() throws RemoteException;
    boolean isAdmin() throws RemoteException;
    void receiveClearCanvasMessage() throws RemoteException;
    void receiveImage(byte[] imageBytes) throws RemoteException;
}
