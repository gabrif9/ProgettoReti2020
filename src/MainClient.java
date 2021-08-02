import RMICallbacksInterface.RMICallbackClient;
import RMICallbacksInterface.RMICallbackServer;
import registrationInterfaceRMI.RegistrationInterface;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RemoteObject;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MainClient extends RemoteObject implements RMICallbackClient {

    private int loggedIn = 0;
    private RMICallbackClient stub = null;
    private RMICallbackClient ROC;
    private ArrayList<String> listUsers;
    private ConcurrentHashMap<String, String> listOnlineUsers; //e.g.: Dario, Online
    public static int DEFAULT_PORT = 5000;
    public static int DEFAULT_PORT_CALLBACK = 3000;
    SocketChannel clientChannel;

    //CONSTRUCTOR
    public MainClient() {
        listUsers = new ArrayList<>();
    }

    /**
     * Comandi possibili da terminale:
     *
     * register: avvia il processo di registrazione
     * login: avvia il processo di login
     * logout: effettua il logout dalla piattaforma
     * listUsers: visualizza gli utenti me il loro stato registrati sulla piattaforma
     * listOnlineUsers: visualizza la lista degli utenti online
     * listProjects: visualizza la lista dei progetti di cui l'utente e' membro (messaggio: nessun progetto nel caso in cui l'utente non sia membro di nessun progetto
     * createProject: richiede la creazione di un nuovo progetto
     * addMember: aggiunge un utente gia' registrato alla piattaforma ad un determinato progetto
     * showMember: recupera la lista dei membri del progetto
     * showCards: recupera la lista delle card associate ad un progetto
     * showCard: recupera le informazioni di una data card di un progetto
     * addCard: aggiunge una card ad un progetto
     * moveCard: sposta una card di un progetto da una lista ad un'altra (solo spostamenti consentiti)
     * getCardHistory: richiede la storia delle card (cronologia degli spostamenti)
     * readChat: visualizza i messaggi nella chat
     * sendChatMsg: l'utente invia un messaggio alla chat associata al progetto
     * cancelProject: un membro del progetto richiede la cancellazione dello stesso
     */

    //comandi da mostrare alla prima connessione
    String commandTableLoginRegister = ("Select a command by entering the corresponding number and pressing enter\n" +
            "1 - register \n" +
            "2 - login\n");


    //comandi da mostrare dopo il login
    String commandTableRegisteredUser = ("Select a command by entering the corresponding number and pressing enter\n" + "1 - logout\n" + "2 - listUsers\n" + "3 - listOnlineUsers\n" + "4 - listProjects\n" +
            "5 - createProject\n" + "6 - addMember\n" + "7 - showMember\n" + "8 - showCards\n" + "9 - showCard\n" + "10 - addCard\n" + "11 - moveCard\n" + "12 - getCardHistory\n" + "13 - readChat\n" + "14 - sendChatMsg\n" +
            "15 - sendChatMsg\n" + "16 - cancelProject\n");


    public static void main (String[]args){

        MainClient client = new MainClient();
        client.beforeLoginCommand();

    }

    public void beforeLoginCommand () {
        System.out.println(commandTableLoginRegister);
        Scanner commandNumber = new Scanner(System.in);
        int command = 0;
        try {
            command = commandNumber.nextInt();
        } catch (InputMismatchException e){
            System.err.println("Inserisci un numero");
            beforeLoginCommand();
        }

        switch (command) {
            case 1:
                //stranamente funziona
                register(); //metodo che serve per registrare l'utente e che connette il client al server tramite tcp
                beforeLoginCommand();
                break;
            case 2:
                //login
                while (listOnlineUsers == null && loggedIn == 0) {//return the list of user and their current state, null if the user doesn't exist
                    listOnlineUsers = login();

                }
                System.out.println(loggedIn);
                registerForCallback();
                afterLoginCommand();
                break;

            default:
                System.err.println("Command not found");
                beforeLoginCommand();
        }
    }

    public void afterLoginCommand () {
        System.out.println(commandTableRegisteredUser);
        Scanner commandNumber = new Scanner(System.in);
        int command = commandNumber.nextInt();
        switch (command) {
            case 1:
                //logout
                String logout = "logout";
                sendCommand(logout);
                try {
                    clientChannel.close();
                }catch (IOException e){
                    e.printStackTrace();
                }
                loggedIn = 0;
                unregisterForCallback();
                listOnlineUsers = null;
                beforeLoginCommand();
                break;

            case 2:
                //listUsers
                System.out.println(listOnlineUsers);
                operationTerminated();
                break;

            case 3:
                //listOnlineUsers
                Set<String> users = listOnlineUsers.keySet();

                //Iterator for check the online users on my hashmap with the user and their current status
                Iterator<String> usersIterator = users.iterator();
                int countOnlineUsers = 0;
                System.out.println("Users Online: ");
                while (usersIterator.hasNext()){
                    String username = usersIterator.next();
                    if (listOnlineUsers.get(username).equals("online")){
                        countOnlineUsers++;
                        System.out.println(username);
                    }
                    System.out.println(countOnlineUsers + " Users online");
                }
                operationTerminated();
                break;

            case 4:
                //listProjects
            case 5:
                //createProject
            case 6:
                //addMember
            case 7:
                //showMember
            case 8:
                //showCards
            case 9:
                //showCard
            case 10:
                //addCard
            case 11:
                //moveCard
            case 12:
                //getCardHistory
            case 13:
                //readChat
            case 14:
                //sendChatMsg
            case 15:
                //sendChatMsg
            case 16:
                //cancelProject
            default:
                System.err.println("command not found");
                afterLoginCommand();

        }
    }


    public void operationTerminated(){
        Scanner scanner = new Scanner(System.in);
        System.out.println("Press 0 than enter to show the menu'");
        int commandExit = scanner.nextInt();

        if (commandExit == 0){
            afterLoginCommand();
        } else System.err.println("Please press 0 than enter");

    }

    @Override
    public void notifyEventFromServer(String user, String status) throws RemoteException {
        if (listOnlineUsers.containsKey(user)){
            listOnlineUsers.put(user, status);
        }else {
            System.err.println("user not found in listOnlineUsers");
        }
    }

    public void registerForCallback(){
        try {
            Registry registry = LocateRegistry.getRegistry(DEFAULT_PORT_CALLBACK);
            String name = "ServerCallback";
            RMICallbackServer server = (RMICallbackServer) registry.lookup(name);

            ROC = this;
            //se lo stub e' null il client non e' mai stato avviato, altrimenti significa che lo stub e' gia' registrato sul registry
            if (stub==null){
                stub = (RMICallbackClient) UnicastRemoteObject.exportObject(ROC, 0);
            }
            server.registerForCallback(stub);
            System.out.println("registered for callback");

        }catch (RemoteException e){
            e.printStackTrace();
        }catch (NotBoundException e){
            System.err.println("ServerCallback not found on registry");
        }
    }

    public void unregisterForCallback(){
        try {
            Registry registry = LocateRegistry.getRegistry(DEFAULT_PORT_CALLBACK);
            String name = "ServerCallback";
            RMICallbackServer server = (RMICallbackServer) registry.lookup(name);

            server.unregisterForCallback(stub);
        }catch (RemoteException e){
            e.printStackTrace();
        }catch (NotBoundException e){
            System.err.println("ServerCallback not found on registry");
        }
    }

    public ConcurrentHashMap<String, String> login(){
        String user, passwd;
        Scanner in = new Scanner(System.in);
        System.out.println("Insert your username");
        user = in.nextLine();
        System.out.println("Insert your password");
        passwd = in.nextLine();

        if (!clientChannel.isConnected()){
            clientChannel = connectToServer();
        }

        String commandToSend = "login" + " " + user + " " + passwd;
        sendCommand(commandToSend);


        //TODO trovare un modo per non mandare le password in chiaro

        //controllo il risultato della login
        try {
            ByteBuffer response = ByteBuffer.allocate(24);
            clientChannel.read(response);

            response.flip();

            int responseCode = response.getInt(); //codice in risposta all'operazione di login
            System.out.println(responseCode);
            if (responseCode == 200){
                //TODO farsi inviare la lista degli utenti registrati e il loro stato dal server
                loggedIn = 1;
                /*
                provare deserializzando la concurrenthashmap serializzata dal server,
                altrimenti provare con un arraylist di oggetti con due attributi, String name e String stato (potrebbe essere piu' semplice da serializzare ma piu' difficile da
                rendere sincronizzata)
                 */
                try (FileInputStream fis = new FileInputStream("usersHashmap.ser");
                     ObjectInputStream usersStatusOIS = new ObjectInputStream(fis)){
                    return (ConcurrentHashMap<String, String>) usersStatusOIS.readObject();
                } catch (ClassNotFoundException e){
                    e.printStackTrace();
                }

            } else if (responseCode == 404){
                System.out.println("error, user " + user + " not found");
            } else if (responseCode == 400){
                System.out.println("wrong password");
            }

        }catch (IOException e){
            System.err.println("Errore durante il login");
        }
        return null;
    }

    public void register () {

        RegistrationInterface serverObject;
        Remote remoteObject;

        //REGISTRATION
        try {
            Registry r = LocateRegistry.getRegistry(3000);
            remoteObject = r.lookup("REGISTER");
            serverObject = (RegistrationInterface) remoteObject;

            Scanner in = new Scanner(System.in);
            System.out.println("Inserire nome utente da registrare: ");
            String user = in.nextLine();

            System.out.println("Inserire una password: ");
            String password = in.nextLine();

            //register a new user
            //se l'utente e' gia' registrato entro in un ciclo while dove mi fara' inserire un altro nome utente
            while (!registerToServer(serverObject, user, password)) {
                System.out.println("Nome utente gia' presente");

                System.out.println("Inserire un nuovo nome utente da registrare: ");
                user = in.nextLine();

                System.out.println("Inserire una password: ");
                password = in.nextLine();
            }


        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (NotBoundException e) {
            e.printStackTrace();
        }

        //CONTACT SERVER
        clientChannel = connectToServer();
    }

    //method to register a new user and verify that it does not already exist
    public boolean registerToServer (RegistrationInterface serverObject, String user, String password) throws RemoteException {
        String registrationResult = null;
        //register a new user
        try {
            registrationResult = serverObject.register(user, password);
        } catch (IllegalArgumentException e) {
            return false;
        }

        if (registrationResult != null){
            System.out.println("Registration was successful");
        } else {
            return false;
        }

        return true; //non necessario
    }

    public SocketChannel connectToServer(){
        try {
            //connessione del client al server, immediatamente dopo la registrazione
            SocketChannel clientChannel;
            System.out.println("connessione al server");
            clientChannel = SocketChannel.open();

            clientChannel.configureBlocking(true);
            clientChannel.connect(new InetSocketAddress(DEFAULT_PORT));
            System.out.println("connesso al server");
            return clientChannel;

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void sendCommand(String command){
        try {

            //alloco spazio sul buffer per la stringa

            Charset charset = Charset.defaultCharset();
            CharBuffer Cbcs = CharBuffer.wrap(command);
            ByteBuffer byteCommandToSend = charset.encode(Cbcs);

            byteCommandToSend.compact();
            byteCommandToSend.flip();

            while (byteCommandToSend.hasRemaining()){
                clientChannel.write(byteCommandToSend);
            }


        } catch (IOException e){
            System.err.println("Errore durante la scrittura nel canale");
        }
    }


}