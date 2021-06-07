package RMICallbacksInterface;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RMICallbackServer extends Remote {

    public void registerForCallback(RMICallbackClient ClientInterface) throws RemoteException;

    public void unregisterForCallback(RMICallbackClient ClientInterface) throws RemoteException;
}
