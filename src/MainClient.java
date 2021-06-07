import registrationInterfaceRMI.RegistrationInterface;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

public class MainClient {

    private ArrayList<String> listUsers;
    private ConcurrentHashMap<String, String> listOnlineUsers; //e.g.: Dario, Online
    public static int DEFAULT_PORT = 5000;

    public MainClient() {
        listOnlineUsers = new ConcurrentHashMap<>();
        listUsers = new ArrayList<>();
    }

    public void updateListUsers(String nickUtente){
        listUsers.add(nickUtente);
    }

    public void updateOnlineUsers(String nickUser, String status){
        listOnlineUsers.replace(nickUser, status);
    }

    public static void main(String[] args) {

        MainClient client = new MainClient();

        RegistrationInterface serverObject;
        Remote remoteObject;


        //REGISTRATION
        try {
            Registry r = LocateRegistry.getRegistry(3000);
            remoteObject = r.lookup("REGISTER");
            serverObject = (RegistrationInterface) remoteObject;

            Scanner in = new Scanner(System.in);
            System.out.println("Inserire nome utente da registrare: ");
            String nickUtente = in.nextLine();

            System.out.println("Inserire una password: ");
            String password = in.nextLine();

            //register a new user
            serverObject.register(nickUtente, password);

            //deserialize the object with the resultCode inside
            try (FileInputStream fis = new FileInputStream("resultStream");
                 ObjectInputStream oin = new ObjectInputStream(fis)){

                RegistrationResult result = (RegistrationResult) oin.readObject();
                if (result.getResult() == 200){
                    System.out.println("Registrazione avvenuta con successo!");
                }
            }catch (IOException | ClassNotFoundException e){
                e.printStackTrace();
            }


        }catch (IllegalArgumentException e) {
            System.out.println("Nickname gia' presente");
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (NotBoundException e){
            e.printStackTrace();
        }

        //CONTACT SERVER
        try {
            //connessione del client al server, immediatamente dopo la registrazione
            SocketChannel clientChannel = SocketChannel.open();

            clientChannel.configureBlocking(false);
            clientChannel.connect(new InetSocketAddress(DEFAULT_PORT));

        } catch (IOException e){
            e.printStackTrace();
        }


    }

}
