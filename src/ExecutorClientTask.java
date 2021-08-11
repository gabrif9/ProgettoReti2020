//questa classe si occupera' di gestire i comandi dei vari client

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class ExecutorClientTask implements Runnable{

    private MainServer mainServer;
    private String user;
    private SocketChannel clientChannel;
    private String[] command;

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
                String nameProject = command[1].trim();
                Project project = null;

                //check if the new project name already exist
                if ((project = mainServer.checkProject(nameProject))!=null){
                    sendResult("Error project already exist");
                    break;
                }
                project.addMember(user); //add the member that has request the creation of this project
                mainServer.addProject(project);
                sendResult("OK");
                break;

            case "addMember":

                //get the project name
                String nameProject1 = command[1].trim();

                //check if the project exist
                int result = mainServer.searchRegisteredMember(nameProject1, user);
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
                String nameProject2 = command[1].trim();
                Project project1 = null;

                //check if the project exist
                if ((project1 = mainServer.checkProject(nameProject2))!= null){
                    if (project1.searchMember(user)){
                        ArrayList<String> members = project1.getMembers();
                        sendResult("OK");
                        sendSerializedObject(members);
                    } else {
                        sendResult("This user does not belong to the project");
                    }
                } else sendResult("Project not found");
                break;

            case "showCards":
                String nameProject3 = command[1].trim();
                Project project2 = null;

                //check if the project exist
                if ((project2 = mainServer.checkProject(nameProject3))!= null){
                    if (project2.searchMember(user)){
                        ArrayList<String> cardsList = project2.getCards();
                        sendResult("OK");
                        sendSerializedObject(cardsList);
                    } else {
                        sendResult("This user does not belong to this project");
                    }
                } else sendResult("Project does not exist");
                break;

            case "showCard":


                break;

            case "addCard":


                break;

            case "moveCard":


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
