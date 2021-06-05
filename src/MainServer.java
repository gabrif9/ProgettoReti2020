
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RemoteServer;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ConcurrentHashMap;

public class MainServer extends RemoteServer implements RegistrationInterface {

    ConcurrentHashMap<String, String> registeredUsersData = new ConcurrentHashMap<>();
    public static int DEFAULT_PORT = 5000;

    //register a new user
    public  int register (String nickUtente, String password) throws RemoteException{
        if (nickUtente!=null && !nickUtente.isEmpty() && password!=null && !password.isEmpty()){
            String result = registeredUsersData.putIfAbsent(nickUtente, password);

            if (result==null){
                try (FileOutputStream resultStream = new FileOutputStream("resultStream");
                     ObjectOutputStream out = new ObjectOutputStream(resultStream);){
                    //serialize the result code
                    RegistrationResult resultCode = new RegistrationResult(200); //code result if the registration was successful

                    out.writeObject(resultCode);
                } catch (IOException e){
                    e.printStackTrace();
                }
            }
            else throw new IllegalArgumentException();
        }
        else throw new IllegalArgumentException();
        return 0;
    }

    public static void main(String[] args) {
        try {
            MainServer mainServer = new MainServer();

            RegistrationInterface stub = (RegistrationInterface) UnicastRemoteObject.exportObject(mainServer, 0);

            LocateRegistry.createRegistry(3000);
            Registry r = LocateRegistry.getRegistry(3000);

            r.rebind("REGISTER", stub);
            System.out.println("Ready for registration");
        } catch (RemoteException e){
            e.printStackTrace();
        }

        ServerSocketChannel serverSocketChannel;
        ServerSocket serverSocket;
        Selector selector;

        //OPEN A TCP CONNECTION
        try {
            /**
             * Open a serverSocketChannel and bind a Socket to it
             */
            serverSocketChannel = ServerSocketChannel.open();
            serverSocket = serverSocketChannel.socket();
            InetSocketAddress inetSocketAddress = new InetSocketAddress(DEFAULT_PORT);

            serverSocket.bind(inetSocketAddress);

            serverSocketChannel.configureBlocking(false);

            /**
             * Open a selector and register it to the ServerSocketChannel
             */
            selector = Selector.open();
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        }catch (IOException e){
            e.printStackTrace();
        }

        while (true) {
            try {
                selector.select();
                //TODO avviare un nuovo thread che si occupera' del client che ha richiesto la connessione
            }
        }

    }
}
