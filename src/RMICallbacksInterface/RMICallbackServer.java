package RMICallbacksInterface;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.concurrent.ConcurrentHashMap;

public interface RMICallbackServer extends Remote {

    public void registerForCallback(RMICallbackClient ClientInterface) throws RemoteException;

    public void unregisterForCallback(RMICallbackClient ClientInterface) throws RemoteException;

    public void registerForcallbackUpdateProjects(String user, RMICallbackClient clientInterface) throws RemoteException;

    public void unRegisterForcallbackupdateProjects(String user, RMICallbackClient clientInterface) throws RemoteException;
}
