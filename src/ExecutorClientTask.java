//questa classe si occupera' di gestire i comandi dei vari client

import java.nio.channels.SocketChannel;

public class ExecutorClientTask implements Runnable{

    private String user;
    private SocketChannel clientChannel;
    private String[] command;

    public ExecutorClientTask(MainServer mainServer, String user, String[] command){
        this.clientChannel = clientChannel;
        this.user = user;
        this.command = command;
    }

    @Override
    public void run() {
        switch (command[0].trim()){
            case "listProjects": //no arguments


                break;

            case "createProject":


                break;

            case "addMember":


                break;

            case "showMember":


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
}
