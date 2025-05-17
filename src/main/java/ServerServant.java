import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

public class ServerServant extends UnicastRemoteObject implements WhiteboardServerStub {
    private final List<WhiteboardClientStub> clientList = Collections.synchronizedList(new ArrayList<>());
    private final Set<String> usernameList = new HashSet<>();
    private final List<DrawCommand> commandList = new ArrayList<>();

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

        commandList.add(command);
    }

    @Override
    public void broadcastChatMessages(String username, String message) throws RemoteException {
        for (WhiteboardClientStub clientStub : clientList) {
            try {
                clientStub.receiveChatMessage(username, message);
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

    @Override
    public void sendServerDownMessage() throws RemoteException {
        List<WhiteboardClientStub> clientsCopy;
        synchronized (clientList) {
//            System.out.println("executed.");
            clientsCopy = new ArrayList<>(clientList);
        }

        for (WhiteboardClientStub clientStub : clientsCopy) {
//            System.out.println("executed.");
            try {
                clientStub.receiveServerDownMessage();
//                System.out.println("executed.");
            } catch (RemoteException e) {
                System.out.println("remote exception.");
            }
        }
//        System.out.println("execution ends.");
    }

    @Override
    public void removeClient(WhiteboardClientStub clientStub) throws RemoteException {
        this.clientList.remove(clientStub);
        this.usernameList.remove(clientStub.getUsername());
    }

    @Override
    public void shutDownServer(String ip, int port) throws RemoteException {
        try {
            Naming.unbind("rmi://" + ip + ":" + port + "/whiteboard");
            UnicastRemoteObject.unexportObject(this, true);
            System.out.println("Whiteboard server unbound and shut down cleanly.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public List<DrawCommand> getCommandList() {
        return commandList;
    }

    @Override
    public List<WhiteboardClientStub> getClientList() throws RemoteException {
        return this.clientList;
    }
}
