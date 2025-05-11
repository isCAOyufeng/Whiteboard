import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface WhiteboardServerStub extends Remote {
    void broadcastLocalChanges(DrawCommand command) throws RemoteException;
    void registerClient(WhiteboardClientStub clientStub) throws RemoteException, DuplicateUsernameException;
    void sendServerDownMessage() throws RemoteException;
    void removeClient(WhiteboardClientStub clientStub) throws RemoteException;
    void shutDownServer(String ip, int port) throws RemoteException;
    List<DrawCommand> getCommandList() throws RemoteException;
}
