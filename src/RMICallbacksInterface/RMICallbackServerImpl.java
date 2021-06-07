package RMICallbacksInterface;

import java.rmi.RemoteException;
import java.rmi.server.RemoteObject;
import java.util.ArrayList;
import java.util.List;

public class RMICallbackServerImpl extends RemoteObject implements RMICallbackServer {

    private List<RMICallbackClient> clients;

    RMICallbackServerImpl() throws RemoteException{
        super();
        clients = new ArrayList<RMICallbackClient>();
    }


    @Override
    public synchronized void registerForCallback(RMICallbackClient clientInterface) throws RemoteException {
        if (!clients.contains(clientInterface)){
            clients.add(clientInterface);
        }
    }

    @Override
    public synchronized void unregisterForCallback(RMICallbackClient clientInterface) throws RemoteException {
        if (clients.remove(clientInterface)) System.out.println("client" + clientInterface.toString() + "unregistered");
        else System.out.println("Client not found");
    }

    public synchronized void update()
}
