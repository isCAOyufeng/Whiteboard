package Servants;

import Exceptions.DuplicateUsernameException;
import LocalWhiteboard.DrawCommand;
import Stubs.WhiteboardClientStub;
import Stubs.WhiteboardServerStub;

import java.awt.image.BufferedImage;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

public class ServerServant extends UnicastRemoteObject implements WhiteboardServerStub {
    private final List<WhiteboardClientStub> clientList = Collections.synchronizedList(new ArrayList<>());
    private final Set<String> usernameList = new HashSet<>();
    private final List<DrawCommand> commandList = new ArrayList<>();

    public ServerServant() throws RemoteException {
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
            clientsCopy = new ArrayList<>(clientList);
        }

        for (WhiteboardClientStub clientStub : clientsCopy) {
            try {
                clientStub.receiveServerDownMessage();
            } catch (RemoteException e) {
                System.out.println("remote exception.");
            }
        }
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
            System.out.println("LocalWhiteboard.Whiteboard server unbound and shut down cleanly.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<DrawCommand> getCommandList() {
        return commandList;
    }

    @Override
    public void clearCommandList() {
        this.commandList.clear();
    }

    @Override
    public void sendClearCanvasMessage() throws RemoteException {
        for (WhiteboardClientStub clientStub : clientList) {
            clientStub.receiveClearCanvasMessage();
        }
    }

    @Override
    public List<WhiteboardClientStub> getClientList() throws RemoteException {
        return this.clientList;
    }
}
