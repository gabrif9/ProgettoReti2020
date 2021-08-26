package RMICallbacksInterface;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.RemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

//implementazione dell'interfaccia del server con i metodi per registrare/deregistrare un client al servizio di callback
public class RMICallbackServerImpl extends RemoteObject implements RMICallbackServer {

    private List<RMICallbackClient> clients;
    private HashMap<String, RMICallbackClient> clientForUpdateProjectRegistered;

    public RMICallbackServerImpl() throws RemoteException{
        super();
        clients = new ArrayList<RMICallbackClient>();
        clientForUpdateProjectRegistered = new HashMap<>();
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

    public synchronized void update(String user, String status) throws RemoteException {doCallbacks(user, status);}

    public synchronized void doCallbacks(String user, String status) throws RemoteException{
        Iterator i = clients.iterator();

        while (i.hasNext()){
            RMICallbackClient rmiCallbackClient = (RMICallbackClient) i.next();
            rmiCallbackClient.notifyEventFromServer(user, status);
        }
    }

    public synchronized void registerForcallbackUpdateProjects(String user, RMICallbackClient clientInterface){
        clientForUpdateProjectRegistered.putIfAbsent(user, clientInterface);
    }

    public synchronized void unRegisterForcallbackupdateProjects(String user, RMICallbackClient clientInterface){
        clientForUpdateProjectRegistered.remove(user);
    }

    public synchronized void updateProject(String user, String project, String MIPAddress) throws RemoteException{doCallbacksProjectAdded(user, project, MIPAddress);}

    public synchronized void doCallbacksProjectAdded(String user, String nameProject, String MIPAddress) throws RemoteException{
        clientForUpdateProjectRegistered.get(user).addedToNewProjectevent(nameProject, MIPAddress);
    }

    public synchronized void doCallbacksProjectremoved(String user, String nameProject) throws RemoteException{
        clientForUpdateProjectRegistered.get(user).projectRemoved(nameProject);
    }
}
