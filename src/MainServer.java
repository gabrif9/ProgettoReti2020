
import RMICallbacksInterface.RMICallbackServer;
import RMICallbacksInterface.RMICallbackServerImpl;
import com.sun.tools.javac.Main;
import registrationInterfaceRMI.RegistrationInterface;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.IntBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RemoteServer;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;


public class MainServer extends RemoteServer implements RegistrationInterface {


    private static RMICallbackServerImpl ROS;
    private static ThreadPoolExecutor executor;
    ArrayList<Project> projectList;
    private static ConcurrentHashMap<String, String> registeredUsersData; //hashmap with username and psw
    private static HashMap<String, String> usersStatus;
    HashMap<SocketChannel, String> channelBinding; //hashmap that bind a channel with a user
    public static int DEFAULT_PORT = 5000;
    public static int DEFAULT_PORT_RMI = 3000;


    public MainServer(){
        usersStatus = new HashMap<>();
        channelBinding = new HashMap<>();
        executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        projectList = new ArrayList<Project>();
        registeredUsersData = new ConcurrentHashMap<>();
        try {
           ROS = new RMICallbackServerImpl();
        }catch (RemoteException e){
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {

        try {
            MainServer mainServer = new MainServer();

            //RMI implementation

            //registration
            RegistrationInterface stubRegister = (RegistrationInterface) UnicastRemoteObject.exportObject(mainServer, 0);

            LocateRegistry.createRegistry(DEFAULT_PORT_RMI);
            Registry r = LocateRegistry.getRegistry(DEFAULT_PORT_RMI);

            r.rebind("REGISTER", stubRegister);
            System.out.println("Ready for registration");

            //callback
            String name = "ServerCallback";
            RMICallbackServer stubCallback = (RMICallbackServer) UnicastRemoteObject.exportObject(ROS, 3000);
            r.bind(name, stubCallback);

        } catch (RemoteException e){
            e.printStackTrace();
        } catch (AlreadyBoundException e){
            System.err.println("Already bound");
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

                            Charset charset = StandardCharsets.UTF_8;
                            CharBuffer charBuffer = charset.decode(buffer);

                            String command = new String(charBuffer.array());
                            String[] arrayStringCommand = command.split(" ");
                            System.out.println(arrayStringCommand[0]);

                            //TODO avviare un thread e passare al thread le informazioni necessarie per avviare l'operazione richiesta dal client
                            switch (arrayStringCommand[0].trim()){
                                case "login":
                                    String loginResult = login(arrayStringCommand[1].trim(), arrayStringCommand[2].trim());
                                    System.out.println(loginResult);
                                    ByteBuffer resultCode = ByteBuffer.allocate(24);
                                    //restituisco al client il codice 200 se l'operazione e' andata a buon fine, 400 se la password e' sbagliata e 404 se l'utente non esiste
                                    switch (loginResult.trim()){
                                        case "ok":
                                            resultCode.putInt(200);
                                            resultCode.flip();

                                           while (resultCode.hasRemaining()){
                                               try {
                                                   while (client.write(resultCode)!=0){ }
                                               }catch (IOException e){
                                                   System.out.println("Errore durante la scrittura sul channel 'client' ");
                                               }
                                           }
                                        break;

                                        case "wrong password":
                                            System.out.println("dopo controllo comando");
                                            int code1 = 400;
                                            resultCode.put(((byte) code1));
                                            resultCode.flip();

                                            while (resultCode.hasRemaining()){
                                                try {
                                                    client.write(resultCode);
                                                }catch (IOException e){
                                                    System.out.println("Errore durante la scrittura sul channel 'client' ");
                                                }
                                            }
                                            break;

                                        case "the user does not exist":
                                            int code2 = 404;
                                            resultCode.put(((byte) code2));
                                            resultCode.flip();

                                            while (resultCode.hasRemaining()){
                                                try {
                                                    client.write(resultCode);
                                                }catch (IOException e){
                                                    System.out.println("Errore durante la scrittura sul channel 'client' ");
                                                }
                                            }
                                            break;
                                    }
                                break;
                                    //TODO implementare gli altri comandi da qui
                            }


                            //start a thread for different command
                           // executor.execute(new ExecutorClientTask());
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

    //register a new user
    public String register (String nickUtente, String password) throws RemoteException{
        if (nickUtente!=null && !nickUtente.isEmpty() && password!=null && !password.isEmpty()){
            String result = registeredUsersData.putIfAbsent(nickUtente, password);
            usersStatus.putIfAbsent(nickUtente, "offline");

            //registration success
            if (result==null){
                return "Registration success";
            }
            else throw new IllegalArgumentException();
        }
        else throw new IllegalArgumentException();
    }

    public static String login(String user, String psw){
        try {
            //controllo se l'username e' presente e la password e' corretta
            //System.out.println(psw + " dentro login");
            if (registeredUsersData.get(user).equals(psw)){

                //se lo sono aggiorno lo stato dell'utente da offline a online e restituisco la nuova hashmap con gli stati aggiornati
                usersStatus.put(user, "online");
                try{
                    FileOutputStream fos = new FileOutputStream("usersHashmap.ser");
                    ObjectOutputStream usersDataOOS = new ObjectOutputStream(fos);

                    usersDataOOS.writeObject(usersStatus);
                    System.out.println("usersStatus serialized");
                    fos.close();
                    usersDataOOS.close();

                    //update all the client with callback
                    ROS.update(user, "Online");

                }catch (IOException e){
                    e.printStackTrace();
                }
                return "ok";

            } else {
                //se la password e' sbagliata
                return "wrong password";
            }
        }catch (NullPointerException e){
            return "the user does not exist";
        }

    }

//    public boolean checkUserPsw(String user, String psw){
//        if (registeredUsersData)
//    }

    //synchronized method for delete a project
    public void deleteProject(){
        synchronized (projectList){
            //TODO
        }
    }
}
