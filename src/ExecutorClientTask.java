//questa classe si occupera' di gestire i comandi dei vari client

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;

public class ExecutorClientTask implements Runnable{

    private String nameProject;
    private MainServer mainServer;
    private String user;
    private SocketChannel clientChannel;
    private String[] command;
    private Project project = null;

    public ExecutorClientTask(MainServer mainServer, String user, String[] command, SocketChannel clientChannel){
        this.clientChannel = clientChannel;
        this.user = user;
        this.command = command;
        this.mainServer = mainServer;
    }

    @Override
    public void run() {
        switch (command[0].trim()){
            case "createProject":
                //get the new project name
                nameProject = command[1].trim();

                project = new Project(nameProject);
                project.addMember(user);

                //check if the new project name already exist
                if (mainServer.addProject(project, user)){
                    sendResult("OK");
                    HashMap<String, String> IPBinding = mainServer.sendIPBinding(user);
                    sendSerializedObject(IPBinding);
                    break;
                } else {
                    sendResult("Project already exist");
                }
                break;

            case "addMember":

                //get the project name
                nameProject = command[1].trim();
                //check if the project exist
                int result = mainServer.searchRegisteredMember(nameProject, command[2]);
                if (result == 200){
                    //controllare se newMember e' registrato e non e' membro del progetto
                    sendResult("OK");
                } else if (result == 400){
                    sendResult("User already member");
                } else if (result == 404){
                    sendResult("project does not exist");
                } else if (result == 405){
                    sendResult("User does not exist");
                }
                break;

            case "showMember":
                nameProject = command[1].trim();

                //check if the project exist
                if ((project = mainServer.checkProject(nameProject))!= null){
                    if (project.searchMember(user)){
                        ArrayList<String> members = project.getMembers();
                        sendResult("OK");
                        sendSerializedObject(members);
                    } else {
                        sendResult("This user does not belong to the project");
                    }
                } else sendResult("Project not found");
                break;

            case "showCards":
                nameProject = command[1].trim();

                //check if the project exist
                if ((project = mainServer.checkProject(nameProject))!= null){
                    if (project.searchMember(user)){
                        ArrayList<String> cardsList = project.getCards();
                        sendResult("OK");
                        sendSerializedObject(cardsList);
                    } else {
                        sendResult("This user does not belong to this project");
                    }
                } else sendResult("Project does not exist");
                break;

            case "showCard":
                //verificare che il progetto esista, verificare che l'utente e' memebro di quel progetto, verificare che la card esista e restituirla
                nameProject = command[1].trim();
                String cardName = command[2].trim();
                Card card = null;

                //check if the project exist
                if ((project = mainServer.checkProject(nameProject))!= null){
                    if (project.searchMember(user)){
                        if ((card = project.searchCard(cardName))!=null){
                            sendResult("OK");
                            sendSerializedObject(card);
                        } else {
                            sendResult("Card not found");
                        }
                    } else {
                        sendResult("This user does not belong to this project");
                    }
                } else {
                    sendResult("Project does not exist");
                }
                break;

            case "addCard":
            //controllare se il progetto esiste, se l'utente e' membro del progetto e se la card esiste e se non esiste aggiungerla

                nameProject = command[1].trim();
                String cardName2 = command[2].trim();

                //obtain the description from the command array
                String cardDescription = command[3];

                //new card to add
                Card card2 = new Card(cardName2, cardDescription);

                //check if the project exist
                if ((project = mainServer.checkProject(nameProject))!= null){
                    if (project.searchMember(user)){
                        boolean resultOperation = mainServer.addCard(cardName2, nameProject, cardDescription);
                        if (resultOperation){
                            sendResult("OK");
                        } else sendResult("Card already exist");
                    } else {
                        sendResult("This user does not belong to this project");
                    }
                } else {
                    sendResult("Project does not exist");
                }
                break;

            case "moveCard":
                //controllare se il progetto esiste, se l'utente e' membro e se la card e' nella lista di partenza, in tal caso spostare la card
                nameProject = command[1];
                String cardName3 = command[2];
                String srcList = command[3];
                String destList = command[4];
                String resultMoveOperation;

                //check if the project exist and user belong to the project
                if ((project = mainServer.checkProject(nameProject))!= null) {
                    if (project.searchMember(user)) {
                        resultMoveOperation = mainServer.moveCard(nameProject, cardName3, srcList, destList);
                        sendResult(resultMoveOperation);
                    } else {
                        sendResult("This user does not belong to this project");
                    }
                } else {
                    sendResult("Project does not exist");
                }
                break;

            case "getCardHistory":
                nameProject = command[1];
                String cardName4 = command[2];

                //check if the project exist and user belong to the project
                if ((project = mainServer.checkProject(nameProject))!= null) {
                    if (project.searchMember(user)) {
                        try {
                            System.out.println("nome carta = " + cardName4);
                            Card cardTmp = project.getCard(cardName4);
                            System.out.println("Sono dentro il blocco try di getCardHistory");
                            sendResult("OK");
                            sendSerializedObject(cardTmp.getCardHistory());
                        } catch (IllegalArgumentException e){
                            sendResult("Card not found");
                        }
                    } else sendResult("This user does not belong to this project");
                }else {
                    sendResult("Project does not exist");
                }
                break;
        }
    }


    public void sendResult(String result){
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

    //send a serialized object
    public void sendSerializedObject(Object obj){
        try {
            //serialize the object
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
            objectOutputStream.writeObject(obj);
            objectOutputStream.flush();
            byte[] bytesObjectSerialized = byteArrayOutputStream.toByteArray();


            //send the obj serialized
            ByteBuffer byteBuffer = ByteBuffer.allocate(bytesObjectSerialized.length);
            byteBuffer.put(bytesObjectSerialized);
            byteBuffer.flip();
            while (byteBuffer.hasRemaining()){
                clientChannel.write(byteBuffer);
            }
        }catch (IOException e){
            e.printStackTrace();
        }
    }
}
