import registrationInterfaceRMI.RegistrationInterface;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.IntBuffer;
import java.nio.channels.NotYetConnectedException;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
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
    SocketChannel clientChannel;

    //CONSTRUCTOR
    public MainClient() {
        listOnlineUsers = new ConcurrentHashMap<>();
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
        int command = commandNumber.nextInt();
        switch (command) {
            case 1:
                //stranamente funziona
                register(); //metodo che serve per registrare l'utente e che connette il client al server tramite tcp
                beforeLoginCommand();

            case 2:
                //login
                while ( (listOnlineUsers = login()) == null);//return the list of user and their current state, null if the user doesn't exist


        }
    }

    public void afterLoginCommand () {
        System.out.println(commandTableRegisteredUser);
        Scanner commandNumber = new Scanner(System.in);
        int command = commandNumber.nextInt();
        switch (command) {
            case 1:
                //logout
            case 2:
                //listUsers
            case 3:
                //listOnlineUsers
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

        }
    }

    public ConcurrentHashMap<String, String> login(){
        String user, passwd;
        Scanner in = new Scanner(System.in);
        System.out.println("Insert your username");
        user = in.nextLine();
        System.out.println("Insert your password");
        passwd = in.nextLine();

        //TODO trovare un modo per non mandare le password in chiaro
        try {
            String commandToSend = "login" + " " + user + " " + passwd;

            //alloco spazio sul buffer per la stringa

            Charset charset = Charset.defaultCharset();
            CharBuffer Cbcs = CharBuffer.wrap(commandToSend);
            ByteBuffer byteCommandToSend = charset.encode(Cbcs);

            byteCommandToSend.compact();
            byteCommandToSend.flip();

            clientChannel.write(byteCommandToSend);
        } catch (IOException e){
            System.err.println("Errore durante la scrittura nel canale");
        }

        //controllo il risultato della login
        try {
            ByteBuffer response = ByteBuffer.allocate(12);
            while (clientChannel.read(response) != -1){
                clientChannel.read(response);
            }

            response.flip();

            int responseCode = response.getInt(); //codice in risposta all'operazione di login

            if (responseCode == 200){
                //TODO farsi inviare la lista degli utenti registrati e il loro stato dal server
                /*
                provare deserializzando la concurrenthashmap serializzata dal server,
                altrimenti provare con un arraylist di oggetti con due attributi, String name e String stato (potrebbe essere piu' semplice da serializzare ma piu' difficile da
                rendere sincronizzata)
                 */
            } else {
                System.out.println("errore, utente " + user + " non esistente");
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
        try {
            //connessione del client al server, immediatamente dopo la registrazione
            System.out.println("connessione al server");
            clientChannel = SocketChannel.open();

            clientChannel.configureBlocking(true);
            clientChannel.connect(new InetSocketAddress(DEFAULT_PORT));
            System.out.println("connesso al server");

        } catch (IOException e) {
            e.printStackTrace();
        }
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
}