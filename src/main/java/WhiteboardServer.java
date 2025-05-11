import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;

public class WhiteboardServer {
    public static void main(String[] args) {
        try {
            int port = Integer.parseInt(args[1]);

            // 尝试连接已有 registry
            try {
                Registry registry = LocateRegistry.getRegistry(port);
                registry.list(); // 如果成功，说明 registry 已存在
                System.out.println("RMI Registry already running on port " + port);
            } catch (RemoteException e) {
                // 如果获取失败，说明 registry 不存在，创建一个新的
                LocateRegistry.createRegistry(port);
                System.out.println("Created new RMI Registry on port " + port);
            }

            WhiteboardServerStub serverStub = new ServerServant();

            // 注册 stub
            Naming.rebind("rmi://" + args[0] + ":" + args[1] + "/whiteboard", serverStub);

            System.out.println("Whiteboard server is running...");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
