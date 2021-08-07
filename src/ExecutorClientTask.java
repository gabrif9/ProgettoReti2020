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


                //check if the new project name already exist
                if (mainServer.checkProject(nameProject)){
                    sendResult("Error project already exist");
                    break;
                }

                //add the new project
                Project project = new Project(nameProject);
                project.addMember(user);
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

                //check if the project exist
                if (mainServer.checkProject(nameProject2)){
                    List<Project> projects = mainServer.getProjectList();
                    int i = 0;
                    int max = projects.size();
                    while (i<max && !projects.get(i).getProjectName().equals(nameProject2)){
                        i++;
                    }
                    ArrayList<String> members = projects.get(i).getMembers();
                    sendResult("OK");
                    sendSerializedObject(members);
                } else sendResult("Project not found");
                break;

            case "showCards":


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
