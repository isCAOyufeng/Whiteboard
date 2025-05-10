import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashSet;
import java.util.Set;

public class ServerServant extends UnicastRemoteObject implements WhiteboardServerStub {
    private final Set<WhiteboardClientStub> clientList = new HashSet<>();
    private final Set<String> usernameList = new HashSet<>();

    protected ServerServant() throws RemoteException {
    }

    @Override
    public void broadcastLocalChanges(DrawCommand command) throws RemoteException {
        for (WhiteboardClientStub clientStub : clientList) {
            try {
                clientStub.receiveCommand(command);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void registerClient(WhiteboardClientStub clientStub) throws RemoteException, DuplicateUsernameException {
        if (usernameList.contains((clientStub.getUsername()))) {
            throw new DuplicateUsernameException("Username already exists.");
        }
        usernameList.add(clientStub.getUsername());
        clientList.add(clientStub);
    }
}
