import java.rmi.Remote;
import java.rmi.RemoteException;

public interface WhiteboardServerStub extends Remote {
    void broadcastLocalChanges(DrawCommand command) throws RemoteException;
    void registerClient(WhiteboardClientStub clientStub) throws RemoteException, DuplicateUsernameException;
}
