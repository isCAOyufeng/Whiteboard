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
    private byte[] baseImage = null;

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
    public boolean registerClient(WhiteboardClientStub clientStub) throws RemoteException, DuplicateUsernameException {
        boolean accepted = false;
        if (usernameList.contains((clientStub.getUsername()))) {
            throw new DuplicateUsernameException("Username already exists.");
        }
        if (!clientStub.isAdmin()) {
            accepted = sendVerificationToAdmin(clientStub.getUsername());
        }
        if (clientStub.isAdmin() || accepted) {
            usernameList.add(clientStub.getUsername());
            clientList.add(clientStub);
        }
        return accepted;
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
                System.err.println("remote exception.");
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

    @Override
    public void sendImage(byte[] imageBytes) throws RemoteException {
        this.baseImage = imageBytes;
        for (WhiteboardClientStub clientStub : clientList) {
            clientStub.receiveImage(imageBytes);
        }
    }

    @Override
    public byte[] getBaseImage() throws RemoteException {
        return this.baseImage;
    }

    private boolean sendVerificationToAdmin(String username) {
        boolean accepted = false;
        try {
            accepted = clientList.get(0).receiveVerification(username);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return accepted;
    }
}
