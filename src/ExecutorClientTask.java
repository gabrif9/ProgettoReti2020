//questa classe si occupera' di gestire i comandi dei vari client

import java.nio.channels.SocketChannel;

public class ExecutorClientTask implements Runnable{

    private String user;
    private SocketChannel clientChannel;
    private String[] command;

    public ExecutorClientTask(String user, SocketChannel clientChannel, String[] command){
        this.clientChannel = clientChannel;
        this.user = user;
        this.command = command;
    }

    @Override
    public void run() {

    }
}
