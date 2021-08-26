import RMICallbacksInterface.RMICallbackClient;
import RMICallbacksInterface.RMICallbackServer;
import registrationInterfaceRMI.RegistrationInterface;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RemoteObject;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

public class MainClient extends RemoteObject implements RMICallbackClient {



    //threadpool for manage the chat
    private ThreadPoolExecutor executorChat;

    //hashmap for save the Future of a task, that permit to stop a single thread in the threadpool
    private HashMap<String, Future<?>> threadBinding;
    //HashMap with association <nameProject, MIPAddress>
    private HashMap<String, String> IPBinding;
    //HashMap with association <nameProject, messageHistory>
    private HashMap<String, ArrayList<String>> messageHistoryProjects;
    //HashMap with association <nameProject, lastReadMessage> for remember the last message read oh the project chat's
    private HashMap<String, Integer> lastReadMessageCounters;


    private String user;
    private String nameProject;
    private int loggedIn = 0;
    private RMICallbackClient stub = null;
    private RMICallbackClient ROC;
    private ArrayList<String> listUsers;
    private ConcurrentHashMap<String, String> listOnlineUsers; //e.g.: Dario, Online
    public static int DEFAULT_PORT = 5000;
    public static int DEFAULT_PORT_CALLBACK = 3000;
    SocketChannel clientChannel;
    RMICallbackServer server;

    //CONSTRUCTOR
    public MainClient() {
        listUsers = new ArrayList<>();
        executorChat = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        threadBinding = new HashMap<>();
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
     * getCardHistory: richiede la storia di una card (cronologia degli spostamenti)
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

        MainClient mainClient = new MainClient();
        mainClient.beforeLoginCommand();

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

                //receive the Hashmap with the association Project-MIPAddress
                //TODO trovare una soluzione se non c'e' nulla da ricevere
                try (ObjectInputStream objectInputStream = new ObjectInputStream(clientChannel.socket().getInputStream())){
                    if (objectInputStream.available()!=0){
                        IPBinding = (HashMap<String, String>) objectInputStream.readObject();
                    }
                } catch (IOException e){
                    e.printStackTrace();
                } catch (ClassNotFoundException e){
                    e.printStackTrace();
                }
                lastReadMessageCounters = new HashMap<>();
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
        Scanner scanner = new Scanner(System.in);
        int command = scanner.nextInt();
        String commandToSend;
        String responseString;
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
                }
                System.out.println(countOnlineUsers + " Users online");
                operationTerminated();
                break;

            case 4:
                //listProjects
                ArrayList<String> listUserProject = new ArrayList<>();
                commandToSend = "listProjects";
                sendCommand(commandToSend);

                try(ObjectInputStream objectInputStream = new ObjectInputStream(clientChannel.socket().getInputStream())) {
                    listUserProject = (ArrayList<String>) objectInputStream.readObject();
                    System.out.println(listUserProject);
                }catch (IOException e){
                    e.printStackTrace();
                    System.err.println("Errore durante la deserializzazione in 'listProjects'");
                }catch (ClassNotFoundException e){
                    e.printStackTrace();
                }
                operationTerminated();
                break;


            case 5:
                //createProject
                System.out.println("Inserisci il nome del nuovo progetto");
                nameProject = scanner.next();

                commandToSend = "createProject " + nameProject;
                sendCommand(commandToSend);


                responseString = StandardCharsets.UTF_8.decode(receiveResponse()).toString();

                if (!responseString.equals("OK")){
                    System.err.println("Progetto aggiunto correttamente");

                    //receive the MulticastIP of this project and bind it
                    try (ObjectInputStream objectInputStream = new ObjectInputStream(clientChannel.socket().getInputStream())){
                        IPBinding = (HashMap<String, String>) objectInputStream.readObject();
                        ProjectChatSniffer tmp = new ProjectChatSniffer(IPBinding.get(nameProject), nameProject, this);
                        threadBinding.putIfAbsent(nameProject, executorChat.submit(tmp));

                    }catch (ClassNotFoundException | IOException e){
                        e.printStackTrace();
                    }
                    operationTerminated();
                    break;
                } else {
                    System.out.println("Progetto gia' esistente");
                    operationTerminated();
                }
                break;

            case 6:
                //addMember
                System.out.println("Inserisci il nome del progetto");
                nameProject = scanner.next();
                System.out.println("Inserisci il nome del nuovo membro");
                String newMember = scanner.next();

                commandToSend = "addMember " + nameProject + " " + newMember;
                sendCommand(commandToSend);

                responseString = StandardCharsets.UTF_8.decode(receiveResponse()).toString();

                //analyze the response
                if (responseString.equals("OK")){
                    System.out.println("Utente aggiunto correttamente al progetto");
                    operationTerminated();
                    break;
                } else if(responseString.equals("User already member")){
                    System.err.println("L'utente 'e gia' membro del progetto");
                    operationTerminated();
                    break;
                } else if (responseString.equals("project does not exist")){
                    System.err.println("Il progetto non esiste");
                    operationTerminated();
                    break;
                } else if (responseString.equals("User does not exist")){
                    System.err.println("l'utente non e' registrato");
                    operationTerminated();
                    break;
                }
                break;

            case 7:
                //showMember
                System.out.println("Inserisci il nome del progetto");
                String name = scanner.next();

                commandToSend = "showMember " + name;
                sendCommand(commandToSend);

                ArrayList<String> members;
                String result = StandardCharsets.UTF_8.decode(receiveResponse()).toString();
                System.out.println(result);
                if (result.equals("OK")){
                    try(ObjectInputStream objectInputStream = new ObjectInputStream(clientChannel.socket().getInputStream())) {
                        members = (ArrayList<String>) objectInputStream.readObject();
                        System.out.println(members);
                        operationTerminated();
                    } catch (IOException e){
                        e.printStackTrace();
                    } catch (ClassNotFoundException e){
                        e.printStackTrace();
                    }
                } else if (result.equals("Projects not found")){
                    System.err.println("Progetto non trovato");
                } else if (result.equals("This user does not belong to the project")){
                    System.err.println("Non appartieni ai mebri del progetto, solo i membri del progetto possono visionare la lista");
                }
                operationTerminated();
                break;

            case 8:
                //showCards
                System.out.println("Inserisci il nome del progetto");
                nameProject = scanner.next();

                commandToSend ="showCards " + nameProject;
                sendCommand(commandToSend);

                responseString = StandardCharsets.UTF_8.decode(receiveResponse()).toString();
                ArrayList<String> cards;
                if (responseString.equals("OK")){
                    try (ObjectInputStream objectInputStream = new ObjectInputStream(clientChannel.socket().getInputStream())){
                        cards = (ArrayList<String>) objectInputStream.readObject();
                        System.out.println(cards);
                        operationTerminated();
                    }catch (ClassNotFoundException e){
                        e.printStackTrace();
                    }catch (IOException e){
                        System.err.println("Errore durante la deserializzazione");
                    }
                } else if (responseString.equals("This user does not belong to this project")) {
                    System.err.println("Non fai parte del progetto");
                } else if (responseString.equals("Project does not exist")){
                    System.err.println("Il progetto non esiste");
                }
                operationTerminated();
                break;


            case 9:
                //showCard
                System.out.println("Inserisci il nome del progetto");
                nameProject = scanner.next();
                System.out.println("Inserisci il nome della card");
                String cardName = scanner.next();

                commandToSend = "showCard " + nameProject + " " + cardName;
                sendCommand(commandToSend);

                responseString = StandardCharsets.UTF_8.decode(receiveResponse()).toString();
                Card cardReceived = null;

                if (responseString.equals("OK")){
                    try (ObjectInputStream objectInputStream = new ObjectInputStream(clientChannel.socket().getInputStream())){

                        cardReceived = (Card) objectInputStream.readObject();
                        System.out.println("Nome card: " + cardReceived.getName());
                        System.out.println("Descrizione: " + cardReceived.getDescription());
                        System.out.println("Posizione: " + cardReceived.getCardPosition());

                    }catch (IOException e){
                        e.printStackTrace();
                    }catch (ClassNotFoundException e){
                        e.printStackTrace();
                    }
                } else if (responseString.equals("Card not found")){
                    System.err.println("Carta non trovata");
                } else if (responseString.equals("This user does not belong to this project")){
                    System.err.println("Non fai parte del progetto");
                } else if (responseString.equals("Project does not exist")){
                    System.err.println("Il progetto non esiste");
                }
                operationTerminated();

            case 10:
                //addCard
                System.out.println("Inserisci il nome del progetto");
                nameProject = scanner.next();
                System.out.println("Inserisci il nome della card");
                String cardName2 = scanner.next();
                System.out.println("Inserire una descrizione");
                String description = scanner.next();

                commandToSend = "addCard " + nameProject + " " + cardName2 + " " + description;
                sendCommand(commandToSend);

                responseString = StandardCharsets.UTF_8.decode(receiveResponse()).toString();
                System.out.println(responseString);
                if (responseString.equals("OK")){
                    System.out.println("Carta aggiunta con successo");
                } else if (responseString.equals("Card already exist")){
                    System.err.println("La carta esiste gia'");
                } else if (responseString.equals("This user does not belong to this project")){
                    System.err.println("Non fai parte del progetto");
                } else if (responseString.equals("Project does not exist")){
                    System.err.println("Il progetto non esiste");
                }
                operationTerminated();
                break;

            case 11:
                //moveCard
                System.out.println("Inserisci il nome del progetto");
                nameProject = scanner.next();
                System.out.println("Inserisci il nome della card");
                String cardName3 = scanner.next();
                System.out.println("Inserire la lista di partenza");
                String srcList = scanner.next();
                System.out.println("Inserire la lista di destinazione");
                String destList = scanner.next();

                commandToSend = "moveCard " + nameProject + " " + cardName3 + " " + srcList + " " + destList;
                sendCommand(commandToSend);

                responseString = StandardCharsets.UTF_8.decode(receiveResponse()).toString();
                if (responseString.equals("OK")){
                    System.out.println("Carta spostata correttamente");
                }else if (responseString.equals("Wrong destination")){
                    System.err.println("Non puoi spostare una card da " + srcList + " a " + destList);
                }else if (responseString.equals("Card not found")){
                    System.err.println("Carta non trovata");
                } else if (responseString.equals("This user does not belong to this project")){
                    System.err.println("Non fai parte del progetto");
                } else if (responseString.equals("Project does not exist")){
                    System.err.println("Il progetto non esiste");
                }
                operationTerminated();
                break;

            case 12:
                //getCardHistory
                System.out.println("Inserisci il nome del progetto");
                nameProject = scanner.next();
                System.out.println("Inserisci il nome della card");
                String cardName4 = scanner.next();

                commandToSend = "getCardHistory " + nameProject + " " + cardName4;
                sendCommand(commandToSend);

                responseString = StandardCharsets.UTF_8.decode(receiveResponse()).toString();
                System.out.println("risultato response cardHistory " + responseString);
                if (responseString.equals("OK")){
                    //receive and deserialize the arrayList with the card history
                    try {
                        ObjectInputStream objectInputStream = new ObjectInputStream(clientChannel.socket().getInputStream());
                        ArrayList<String> cardHistory = (ArrayList<String>) objectInputStream.readObject();
                        System.out.println(cardHistory);
                    }catch (IOException e){
                        e.printStackTrace();
                    }catch (ClassNotFoundException e){
                        e.printStackTrace();
                    }

                }else if (responseString.equals("Card not found")){
                    System.err.println("Carta non trovata");
                } else if (responseString.equals("This user does not belong to this project")){
                    System.err.println("Non fai parte del progetto");
                } else if (responseString.equals("Project does not exist")){
                    System.err.println("Il progetto non esiste");
                }
                operationTerminated();

            case 13:
                //sendChatMsg
                System.out.println("Inserisci il nome del progetto");
                nameProject = scanner.next();
                System.out.println("Inserisci il messaggio");
                String message = user + " ha detto :" + scanner.nextLine();

                try {
                    //get the multicast ip from the hashmap
                    InetAddress multicastAddress = InetAddress.getByName(IPBinding.get(nameProject));

                    //create datagram socket
                    DatagramSocket datagramSocket = new DatagramSocket(4656);

                    //create the message and put the message inside the datagramPacket

                    byte [] messageData;
                    messageData = message.getBytes();
                    DatagramPacket messageDP = new DatagramPacket(messageData, messageData.length, multicastAddress, 4656);
                    datagramSocket.send(messageDP);
                    datagramSocket.close();
                    System.out.println("Messaggio inviato");
                    operationTerminated();
                } catch (IOException e) {
                    e.printStackTrace();
                }


            case 14:
                //readChat
                System.out.println("Inserisci il nome del progetto");
                nameProject = scanner.next();

                int lastReadMessage = lastReadMessageCounters.get(nameProject);

                synchronized (messageHistoryProjects){
                    if (lastReadMessage + 1 != messageHistoryProjects.get(nameProject).size()){
                        for (int i = lastReadMessage + 1; i <= messageHistoryProjects.get(nameProject).size(); i++){
                            System.out.println("> " + messageHistoryProjects.get(nameProject).get(i));
                        }
                        lastReadMessage = messageHistoryProjects.get(nameProject).size() - 1;
                    } else System.out.println("Non e' presente nessun nuovo messaggio");
                }
                lastReadMessageCounters.put(nameProject, lastReadMessage);
                operationTerminated();





            case 15:
                //cancelProject
                System.out.println("Inserisci il nome del progetto");
                nameProject = scanner.next();

                commandToSend = "cancelProject " + nameProject;
                sendCommand(commandToSend);

                result = StandardCharsets.UTF_8.decode(receiveResponse()).toString();
                if (result.equals("Project deleted")){
                    //TODO avvertire gli altri client che un progetto e' stato eliminato quindi terminare il thread che si occupava della chat

                }

                System.out.println(result);
                operationTerminated();
                break;


            default:
                System.err.println("command not found");
                afterLoginCommand();

        }
    }


    //metodo invocato al termine di un'operazione per permettere all'utente di tornare al menu' principale
    public void operationTerminated(){
        Scanner scanner = new Scanner(System.in);
        System.out.println("Press 0 than enter to show the menu'");
        int commandExit = scanner.nextInt();

        if (commandExit == 0){
            afterLoginCommand();
        } else {
            System.err.println("Please press 0 than enter");
            operationTerminated();
        }

    }


    //Callback operation
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
            server = (RMICallbackServer) registry.lookup(name);

            ROC = this;
            //se lo stub e' null il client non e' mai stato avviato, altrimenti significa che lo stub e' gia' registrato sul registry
            if (stub==null){
                stub = (RMICallbackClient) UnicastRemoteObject.exportObject(ROC, 0);
            }
            server.registerForCallback(stub);
            server.registerForcallbackUpdateProjects(user, stub);
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
            server.unRegisterForcallbackupdateProjects(user, stub);
        }catch (RemoteException e){
            e.printStackTrace();
        }catch (NotBoundException e){
            System.err.println("ServerCallback not found on registry");
        }
    }

    public void addedToNewProjectevent(String nameProject, String MIPAddress) throws RemoteException{
        IPBinding.putIfAbsent(nameProject, MIPAddress);

        //Start a thread sniffer
        ProjectChatSniffer tmp = new ProjectChatSniffer(IPBinding.get(nameProject), nameProject, this);
        threadBinding.putIfAbsent(nameProject, executorChat.submit(tmp));

    }

    public void projectRemoved(String nameProject) throws RemoteException{
        IPBinding.remove(nameProject);
        threadBinding.get(nameProject).cancel(true);
        lastReadMessageCounters.remove(nameProject);
    }


    public ConcurrentHashMap<String, String> login(){
        String passwd;
        Scanner in = new Scanner(System.in);
        System.out.println("Insert your username");
        user = in.nextLine();
        System.out.println("Insert your password");
        passwd = in.nextLine();

        if (clientChannel == null){
            clientChannel = connectToServer();
        }

        String commandToSend = "login " + user + " " + passwd;
        sendCommand(commandToSend);



        //controllo il risultato della login
        try {
            ByteBuffer response = ByteBuffer.allocate(24);
            clientChannel.read(response);

            response.flip();

            int responseCode = response.getInt(); //codice in risposta all'operazione di login
            System.out.println(responseCode);
            if (responseCode == 200){

                loggedIn = 1;
                try {
                    ObjectInputStream objectInputStream = new ObjectInputStream(clientChannel.socket().getInputStream());
                    return (ConcurrentHashMap<String, String>) objectInputStream.readObject();
                }catch (IOException e){
                    System.err.println("Errore durante la deserializzazione in 'login'");
                }catch (ClassNotFoundException e){
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
            if (!registerToServer(serverObject, user, password)) {
                System.out.println("Nome utente gia' presente");
                beforeLoginCommand();
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

        return true;
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

    public ByteBuffer receiveResponse(){
        //receive the response from server
        ByteBuffer response = ByteBuffer.allocate(1024);
        try {
            clientChannel.read(response);
            response.flip();
        }catch (IOException e){
            e.printStackTrace();
        }
        return response;
    }

    //methods for manage chat message
    public void addMessage(String project, String message){
        messageHistoryProjects.get(project).add(message);
    }


}