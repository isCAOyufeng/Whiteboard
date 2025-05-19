package Server;

import Login.LoginDialog;
import Servants.ServerServant;
import Stubs.WhiteboardServerStub;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;

public class WhiteboardServer {
    public static void main(String[] args) throws MalformedURLException, RemoteException {
        int port = 1099;
        try {
            port = Integer.parseInt(args[1]);
            LocateRegistry.createRegistry(port);
            WhiteboardServerStub serverStub = new ServerServant();
            Naming.rebind("rmi://" + args[0] + ":" + args[1] + "/whiteboard", serverStub);

//            System.out.println("LocalWhiteboard.Whiteboard server is running...");
        } catch (Exception e) {
            LoginDialog.showErrorMessage("RMI Registry already running on port " + port + ". Please try other IP/port.");
            throw e;
        }
    }
}
