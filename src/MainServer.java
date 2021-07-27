
import registrationInterfaceRMI.RegistrationInterface;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RemoteServer;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class MainServer extends RemoteServer implements RegistrationInterface {

    ArrayList<Project> projectList = new ArrayList<Project>();
    ConcurrentHashMap<String, String> registeredUsersData = new ConcurrentHashMap<>();
    public static int DEFAULT_PORT = 5000;
    public static int DEFAULT_PORT_RMI = 3000;

    //register a new user
    public  String register (String nickUtente, String password) throws RemoteException{
        if (nickUtente!=null && !nickUtente.isEmpty() && password!=null && !password.isEmpty()){
            String result = registeredUsersData.putIfAbsent(nickUtente, password);

            //registration success
            if (result==null){
                return "Registration success";

            }
            else throw new IllegalArgumentException();
        }
        else throw new IllegalArgumentException();
    }

    public static void main(String[] args) {
        try {
            MainServer mainServer = new MainServer();

            //stub for registration
            RegistrationInterface stub = (RegistrationInterface) UnicastRemoteObject.exportObject(mainServer, 0);

            LocateRegistry.createRegistry(DEFAULT_PORT_RMI);
            Registry r = LocateRegistry.getRegistry(DEFAULT_PORT_RMI);

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


            while (true) {
                try {
                    System.out.println("ciclo selector");
                    selector.select();
                }catch (IOException e){
                    e.printStackTrace();
                    break;
                }

                Set<SelectionKey> readyKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = readyKeys.iterator();

                while (iterator.hasNext()){
                    SelectionKey key = iterator.next();
                    iterator.remove();

                    try {
                        //accept a connection from a client
                        if (key.isAcceptable()){
                            ServerSocketChannel server = (ServerSocketChannel) key.channel();
                            SocketChannel clientChannel = server.accept();
                            clientChannel.configureBlocking(false);

                            //register this channel to the selector with 2 operations
                            clientChannel.register(selector, SelectionKey.OP_READ);
                            System.out.println("connesso con il client");

                        } else if (key.isReadable()){ //if some client ask to do something (like "login", or other methods)
                            SocketChannel client = (SocketChannel) key.channel();
                            ByteBuffer buffer = ByteBuffer.allocate(1024);
                            while (client.read(buffer)!=0){ }
                            buffer.flip();

                            Charset latin1 = StandardCharsets.ISO_8859_1;
                            CharBuffer latin1Buffer = latin1.decode(buffer);

                            String command = new String(latin1Buffer.array());
                            String[] arrayStringCommand = command.split(" ");
                            //System.out.println(arrayStringCommand[1]);

                            //TODO avviare un thread e passare al thread le informazioni necessarie per avviare l'operazione richiesta dal client
                        }
                    }catch (IOException e){
                        key.cancel();
                    }
                }
            }

        }catch (IOException e){
            e.printStackTrace();
        }

    }

    //synchronized method for delete a project
    public void deleteProject(){
        synchronized (projectList){
            //TODO
        }
    }
}
