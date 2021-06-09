//questa classe si occupera' di gestire i comandi dei vari client

import java.nio.channels.SocketChannel;

public class ExecutorClientTask implements Runnable{

    private Project project;
    private String user;
    private SocketChannel clientChannel;

    public ExecutorClientTask(Project project, String user, SocketChannel clientChannel){
        this.clientChannel = clientChannel;
        this.project = project;
        this.user = user;
    }

    @Override
    public void run() {

    }
}
