package RMICallbacksInterface;

import java.rmi.RemoteException;
import java.rmi.server.RemoteObject;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

//implementazione dell'interfaccia del server con i metodi per registrare/deregistrare un client al servizio di callback
public class RMICallbackServerImpl extends RemoteObject implements RMICallbackServer {

    private List<RMICallbackClient> clients;

    public RMICallbackServerImpl() throws RemoteException{
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

    public synchronized void update(String user, String status) throws RemoteException{doCallbacks(user, status);}

    public synchronized void doCallbacks(String user, String status) throws RemoteException{
        Iterator i = clients.iterator();

        while (i.hasNext()){
            RMICallbackClient rmiCallbackClient = (RMICallbackClient) i.next();
            rmiCallbackClient.notifyEventFromServer(user, status);
        }
    }



}
