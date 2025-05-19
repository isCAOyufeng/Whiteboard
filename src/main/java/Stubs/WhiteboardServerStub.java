package Stubs;

import Exceptions.DuplicateUsernameException;
import LocalWhiteboard.DrawCommand;

import java.awt.image.BufferedImage;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface WhiteboardServerStub extends Remote {
    void broadcastLocalChanges(DrawCommand command) throws RemoteException;
    void broadcastChatMessages(String username, String message) throws RemoteException;
    boolean registerClient(WhiteboardClientStub clientStub) throws RemoteException, DuplicateUsernameException;
    void sendServerDownMessage() throws RemoteException;
    void removeClient(WhiteboardClientStub clientStub) throws RemoteException;
    void shutDownServer(String ip, int port) throws RemoteException;
    List<DrawCommand> getCommandList() throws RemoteException;
    void clearCommandList() throws RemoteException;
    void sendClearCanvasMessage() throws RemoteException;
    List<WhiteboardClientStub> getClientList() throws RemoteException;
    void sendImage(byte[] imageBytes) throws RemoteException;
    byte[] getBaseImage() throws  RemoteException;
}
