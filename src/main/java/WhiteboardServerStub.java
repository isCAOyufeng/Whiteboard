import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface WhiteboardServerStub extends Remote {
    void broadcastLocalChanges(DrawCommand command) throws RemoteException;
    void registerClient(WhiteboardClientStub clientStub) throws RemoteException, DuplicateUsernameException;
    List<DrawCommand> getCommandList() throws RemoteException;
}
