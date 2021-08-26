
import RMICallbacksInterface.RMICallbackServer;
import RMICallbacksInterface.RMICallbackServerImpl;
import registrationInterfaceRMI.RegistrationInterface;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;


public class MainServer extends RemoteServer implements RegistrationInterface {


    private MIPManager mipManager;
    private static MainServer mainServer;
    private static RMICallbackServerImpl ROS;
    private static ThreadPoolExecutor executor;
    private static List<Project> projectList; //syncrhonized list
    private static ConcurrentHashMap<String, ArrayList<Project>> projectMemberBinding; //hashmap that bind a member with his project
    private static ConcurrentHashMap<String, String> registeredUsersData; //hashmap with username and psw
    private static ConcurrentHashMap<String, String> usersStatus;
    private static HashMap<SocketChannel, String> channelBinding; //hashmap that bind a channel with a user
    public static int DEFAULT_PORT = 5000;
    public static int DEFAULT_PORT_RMI = 3000;


    public MainServer(){
        usersStatus = new ConcurrentHashMap<String, String>();
        channelBinding = new HashMap<SocketChannel, String>();
        projectMemberBinding = new ConcurrentHashMap<String, ArrayList<Project>>();
        executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        projectList = Collections.synchronizedList(new ArrayList<Project>());
        registeredUsersData = new ConcurrentHashMap<>();
        mipManager = new MIPManager();
        try {
           ROS = new RMICallbackServerImpl();
        }catch (RemoteException e){
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {

        try {
            mainServer = new MainServer();

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
                            int byteRead;

                            while ((byteRead = client.read(buffer))>0){
                                System.out.println("byte letti= " + byteRead);
                            }
                            buffer.flip();

                            Charset charset = StandardCharsets.UTF_8;
                            CharBuffer charBuffer = charset.decode(buffer);

                            String commandArray = new String(charBuffer.array());
                            String[] arrayStringCommand = commandArray.split(" ");
                            String command = arrayStringCommand[0].trim();
                            //System.out.println(command);


                            switch (command){

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
                                           //send the userslist and their status
                                            sendSerializedObject(client, usersStatus);
                                           //add the clientChannel to the hashmap with the user associated
                                            channelBinding.put(client, arrayStringCommand[1].trim());

                                            //arraylist with all the ip multicast of the projects with the user as a member
                                            HashMap<String, String> IPBinding = new HashMap<>();

                                            //send the ip multicast of the projects of this member if the member is in some projects
                                            if (projectMemberBinding.containsKey(arrayStringCommand[1])) {

                                                //get the arraylist of projects with the user as a member
                                                ArrayList<Project> tmpProjectList = projectMemberBinding.get(arrayStringCommand[1]);

                                                for (Project project : tmpProjectList) {
                                                    IPBinding.putIfAbsent(project.getProjectName(), project.getMIPAddress());
                                                }

                                                sendSerializedObject(client, IPBinding);
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

                                case "logout": //no arguments
                                    //disconnetto l'utente, e aggiorno lo stato tramite callback
                                    String user = channelBinding.get(client);
                                    channelBinding.remove(client);
                                    client.close();
                                    ROS.update(user, "Offline");
                                    break;


                                case "listProjects": //noArguments
                                    //create a new Arraylist where i'll add all the project with the user as member
                                    ArrayList<String> listUserProject = new ArrayList<>();

                                    //get username
                                    String user2 = channelBinding.get(client);

                                    //search for projects of which the user is a member
                                    for (Project project : projectList){
                                        if (project.searchMember(user2)){
                                            listUserProject.add(project.getProjectName());
                                        }
                                    }

                                    sendSerializedObject(client, listUserProject);
                                    break;

                                case "cancelProject":
                                    String user5 = channelBinding.get(client);
                                    String nameProject3 = arrayStringCommand[1];
                                    Project project2 = mainServer.checkProject(nameProject3);

                                    if (project2!=null) {
                                        if (project2.searchMember(user5)) {
                                            if (project2.checkCard()){
                                                for (String member : project2.getMembers()){
                                                    ROS.doCallbacksProjectremoved(member, nameProject3);
                                                }
                                                projectList.remove(project2);
                                                //inviare una risposta al client
                                                mainServer.sendResult("Project deleted", client);
                                            } else {
                                                mainServer.sendResult("Cannot delete the project, the cards are not all in the toDoList", client);
                                                }
                                            } else {
                                            mainServer.sendResult("You are not part of the project", client);
                                        }
                                    } else {
                                        mainServer.sendResult("project not found", client);
                                    }
                                    break;


                                default:
                                    String userToSend = channelBinding.get(client);
                                    executor.execute(new ExecutorClientTask(mainServer, userToSend, arrayStringCommand, client));
                            }
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
                usersStatus.putIfAbsent(user, "online");
                try {
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


    /**
     *
     * @param projectName project where the user will have to be added
     * @param user user to add to this project
     * @return 400 if the user is a member of the project
     * @return 200 if the user has been added to this project
     * @return 404 if the project does not exist
     * @return 405 if the user does not exist
     */
    public int searchRegisteredMember(String projectName, String user){
        if (registeredUsersData.containsKey(user)){
            for (Project project : projectList){
                synchronized (project){
                    System.out.println(projectName);
                    if (project.getProjectName().trim().equals(projectName)){
                        try {
                            //add the member tho the project and advise that member that has been added to the project through a callback
                            project.addMember(user);
                            try {
                                ROS.updateProject(user, projectName, project.getMIPAddress());
                            }catch (RemoteException e){
                                e.printStackTrace();
                            }
                        }catch (IllegalArgumentException e){
                            return 400; //if the user is already a member of this project
                        }
                        return 200; //if the user has been added to this project
                    }
                }
            }
            return 404; //if the project does not exist
        }
        return 405; //if the user does not exist
    }


    /**
     *
     * @param nameProject
     * @return the project whit nameProject as name or null if the project does not exist
     */
    public Project checkProject(String nameProject){
        //verify if the project exist
        synchronized (projectList){
            for (Project project : projectList){
                if (project.getProjectName().equals(nameProject)){
                    return project;
                }
            }
        }
        return null;
    }

    public static ConcurrentHashMap<String, String> getUsersStatus() {
        return usersStatus;
    }

    public static HashMap<SocketChannel, String> getChannelBinding() {
        return channelBinding;
    }

    public void bindChannel(String user, SocketChannel client){
        channelBinding.putIfAbsent(client, user);
    }

    public boolean addProject(Project project, String user){
        //add a multicastIP to the project
        synchronized (projectList){
            for (Project projectTmp : projectList){
                if (projectTmp.getProjectName().equals(project.getProjectName())){
                    return false;
                }
            }

            project.setMIPAddress(mipManager.createRandomIP());

            //update the Arraylist with association project-MIPAddress
            synchronized (projectMemberBinding){
                if (projectMemberBinding.containsKey(user)){
                    projectMemberBinding.get(user).add(project);
                } else {
                    ArrayList<Project> tmpArrayList = new ArrayList<>();
                    tmpArrayList.add(project);
                    projectMemberBinding.put(user, tmpArrayList);
                }
            }
            projectList.add(project);

            //update the user
            try {
                ROS.updateProject(user, project.getProjectName(), project.getMIPAddress());
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            return true;
        }
    }

    public HashMap<String, String> sendIPBinding(String user){
        HashMap<String, String> IPBinding = new HashMap<>();
        //recuperare l'arraylist dei progetti associati all'utente
        //per ogni progetto recuperare l'ipmulticast ed inserirlo nella hashmpa insieme al nome del progetto
        synchronized (projectMemberBinding){
            for (Project project : projectMemberBinding.get(user)){
                IPBinding.put(project.getProjectName(), project.getMIPAddress());
            }
        }
        return IPBinding;
    }

    //send a serialized object
    public static void sendSerializedObject(SocketChannel clientChannel, Object obj){
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
            objectOutputStream.writeObject(obj);
            byte[] bytesObjectSerialized = byteArrayOutputStream.toByteArray();


            //send the obj serialized
            ByteBuffer byteBuffer = ByteBuffer.allocate(bytesObjectSerialized.length);
            System.out.println("lunghezza buffer con oggetto serializzato = " + bytesObjectSerialized.length);
            byteBuffer.put(bytesObjectSerialized);
            byteBuffer.flip();
            while (byteBuffer.hasRemaining()){
                clientChannel.write(byteBuffer);
            }
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    public boolean addCard(String cardName, String nameProject, String description){
        for (Project project : projectList){
            synchronized (project){
                if (project.getProjectName().equals(nameProject)){
                    //if the card does not already exist
                    try {
                        project.addCard(cardName, description);
                        return true;
                    }catch (IllegalArgumentException e){
                        return false;
                    }
                }
            }
        }
        return false;
    }

    public String moveCard(String nameProject, String cardName,  String srcList, String destList) {
        for (Project project : projectList) {
            synchronized (project) {
                if (project.getProjectName().equals(nameProject)) {
                    //if the card does not already exist
                    try {
                        if (project.moveCard(cardName, srcList, destList)) {
                            return "OK";
                        }
                    } catch (IllegalArgumentException e) {
                        return "Wrong destination";
                    }
                }
            }
        }
        return "Card not found";
    }

    public void sendResult(String result, SocketChannel clientChannel){
        try {

            //alloco spazio sul buffer per la stringa

            Charset charset = Charset.defaultCharset();
            CharBuffer Cbcs = CharBuffer.wrap(result);
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
