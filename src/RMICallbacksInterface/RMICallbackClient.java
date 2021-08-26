package RMICallbacksInterface;

import java.rmi.Remote;
import java.rmi.RemoteException;

//interfaccia del client con il metodo usato dal server per notificare un evento
public interface RMICallbackClient extends Remote {

    public void notifyEventFromServer(String user, String status) throws RemoteException;
    public void addedToNewProjectevent(String project, String MIPAddress) throws RemoteException;
    public void projectRemoved(String nameProject) throws RemoteException;

}
