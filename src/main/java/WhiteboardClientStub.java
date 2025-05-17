import java.rmi.Remote;
import java.rmi.RemoteException;

public interface WhiteboardClientStub extends Remote {
    void receiveCommand(DrawCommand command) throws RemoteException;
    void receiveChatMessage(String username, String message) throws RemoteException;
    void receiveServerDownMessage() throws RemoteException;
    void receiveServerDownMessage(String message) throws RemoteException;
    String getUsername() throws RemoteException;
}
