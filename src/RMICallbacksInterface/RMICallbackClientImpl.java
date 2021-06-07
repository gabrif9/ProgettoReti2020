package RMICallbacksInterface;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.RemoteObject;

//implementazione interfaccia RMI del client
public class RMICallbackClientImpl extends RemoteObject implements RMICallbackClient {

    public RMICallbackClientImpl() throws RemoteException{
        super();
    }

    //metodo usato per aggiornare lo stato dell'utente "user"
    @Override
    public void notifyEventFromServer(String user, String status) throws RemoteException {

    }
}
