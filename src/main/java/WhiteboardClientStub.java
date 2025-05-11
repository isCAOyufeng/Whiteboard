import java.rmi.Remote;
import java.rmi.RemoteException;

public interface WhiteboardClientStub extends Remote {
    void receiveCommand(DrawCommand command) throws RemoteException;
    void receiveServerDownMessage() throws RemoteException;
    String getUsername() throws RemoteException;
}
