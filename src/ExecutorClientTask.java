//questa classe si occupera' di gestire i comandi dei vari client
/*
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;

public class ExecutorClientTask implements Runnable{

    private String nameProject;
    private final MainServer mainServer;
    private final String user;
    private final String[] command;
    private Project project = null;
    private final Result result;

    public ExecutorClientTask(MainServer mainServer, String user, String[] command, Result result){
        this.user = user;
        this.command = command;
        this.mainServer = mainServer;
        this.result = result;
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
                    result.setResult("OK");
                    HashMap<String, String> IPBinding = mainServer.getIPBinding(user);
                    result.addSerializedObject("IPBinding", serializeObject(IPBinding));
                } else {
                    result.setResult("Project already exist");
                }
                mainServer.setAttachmentOnKey(user, result);
                break;

            case "addMember":

                //get the project name
                nameProject = command[1].trim();
                //check if the project exist
                int resultOperation = mainServer.searchRegisteredMember(nameProject, command[2]);
                if (resultOperation == 200){
                    //controllare se newMember e' registrato e non e' membro del progetto
                    result.setResult("OK");
                } else if (resultOperation == 400){
                    result.setResult("User already member");
                } else if (resultOperation == 404){
                    result.setResult("project does not exist");
                } else if (resultOperation == 405){
                    result.setResult("User does not exist");
                }
                mainServer.setAttachmentOnKey(user, result);
                break;

            case "showMember":
                nameProject = command[1].trim();

                //check if the project exist
                if ((project = mainServer.checkProject(nameProject))!= null) {
                    synchronized (project) {
                        if (project.searchMember(user)) {
                            ArrayList<String> members = project.getMembers();
                            result.setResult("OK");
                            result.addSerializedObject("members", serializeObject(members));
                        } else {
                            result.setResult("This user does not belong to this project");
                        }
                    }
                } else result.setResult("Project not found");
                mainServer.setAttachmentOnKey(user, result);
                break;

            case "showCards":
                nameProject = command[1].trim();

                //check if the project exist
                if ((project = mainServer.checkProject(nameProject))!= null){
                    synchronized (project){
                        if (project.searchMember(user)){
                            ArrayList<String> cardsList = project.getCards();
                            result.setResult("OK");
                            result.addSerializedObject("cardsList", serializeObject(cardsList));
                        } else {
                            result.setResult("This user does not belong to the project");
                        }
                    }
                } else result.setResult("Project not found");
                mainServer.setAttachmentOnKey(user, result);
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
                            result.setResult("OK");
                            result.addSerializedObject("card", serializeObject(card));
                        } else {
                            result.setResult("Card not found");
                        }
                    } else {
                        result.setResult("This user does not belong to the project");
                    }
                } else {
                    result.setResult("Project not found");
                }
                mainServer.setAttachmentOnKey(user, result);
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
                int resultOperationInt = mainServer.addCard(cardName2, nameProject, cardDescription, user);
                if (resultOperationInt == 200){
                    result.setResult("OK");
                } else if (resultOperationInt == 400){
                    result.setResult("Card already exist");
                } else if (resultOperationInt == 404) {
                    result.setResult("This user does not belong to this project");
                } else if (resultOperationInt == 408) {
                    result.setResult("Project not found");
                }
                mainServer.setAttachmentOnKey(user, result);
                break;

            case "moveCard":
                //controllare se il progetto esiste, se l'utente e' membro e se la card e' nella lista di partenza, in tal caso spostare la card
                nameProject = command[1];
                String cardName3 = command[2];
                String srcList = command[3];
                String destList = command[4];
                Boolean resultMoveOperation;

                //check if the project exist and user belong to the project
                if ((project = mainServer.checkProject(nameProject))!= null) {
                    synchronized (project){
                        if (project.searchMember(user)) {
                            resultMoveOperation = project.moveCard(cardName3, srcList, destList);
                            if (resultMoveOperation){
                                result.setResult("OK");
                            } else {
                                result.setResult("Wrong list");
                            }

                        } else {
                            result.setResult("This user does not belong to this project");
                        }
                    }
                } else {
                    result.setResult("Project not found");
                }
                mainServer.setAttachmentOnKey(user, result);
                break;

            case "getCardHistory":
                nameProject = command[1];
                String cardName4 = command[2];

                //check if the project exist and user belong to the project
                if ((project = mainServer.checkProject(nameProject))!= null) {
                    synchronized (project) {
                        if (project.searchMember(user)) {
                            try {
                                System.out.println("nome carta = " + cardName4);
                                Card cardTmp = project.getCardToSend(cardName4);
                                System.out.println("Sono dentro il blocco try di getCardHistory");
                                result.setResult("OK");
                                result.addSerializedObject("cardHistory", serializeObject(cardTmp.getCardHistory()));
                            } catch (IllegalArgumentException e) {
                                result.setResult("Card not found");
                            }
                        } else result.setResult("This user does not belong to this project");
                    }
                }else {
                    result.setResult("Project not found");
                }
                mainServer.setAttachmentOnKey(user, result);
                break;
        }
    }

    public byte[] serializeObject(Object obj){
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
            objectOutputStream.writeObject(obj);

            return byteArrayOutputStream.toByteArray();

        }catch (IOException e){
            e.printStackTrace();
        }
        return null;
    }
}*/