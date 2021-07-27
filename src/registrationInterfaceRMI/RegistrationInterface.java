package registrationInterfaceRMI;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RegistrationInterface extends Remote {

    //set a new user if the nickUtente does not already exist
    String register (String nickUtente, String password) throws RemoteException;
}
