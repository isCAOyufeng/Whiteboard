import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import java.util.ArrayList;
import java.util.List;

public class WhiteboardServer {
    public static void main(String[] args) {
        try {
            LocateRegistry.createRegistry(1099);
            WhiteboardServerStub serverStub = new ServerServant();
//            Naming.rebind("rmi://localhost:1099/whiteboard", serverStub);
            Naming.rebind("rmi://" + args[0] + ":" + args[1] + "/whiteboard", serverStub);

            System.out.println("Whiteboard server is running...");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
