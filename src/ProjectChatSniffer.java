import java.io.IOException;
import java.net.*;
import java.nio.channels.DatagramChannel;
import java.nio.channels.Selector;

public class ProjectChatSniffer implements Runnable{

    String MIPAddress;
    String nameProject;
    InetAddress inetAddress;
    MulticastSocket multicastSocket;
    MainClient mainClient;


    public ProjectChatSniffer(String MIPAddress, String nameProject, MainClient mainClient) {
        this.MIPAddress = MIPAddress;
        this.nameProject = nameProject;
        this.mainClient = mainClient;
    }

    @Override
    public void run() {

        try {
            inetAddress = InetAddress.getByName(MIPAddress);
            multicastSocket = new MulticastSocket();
            multicastSocket.setSoTimeout(5000);
            //join in the multicastGroup
            multicastSocket.joinGroup(inetAddress);

            while (true){
                ///prepare the datagram packet for receive the message
                byte[] bufferMessage = new byte[8192];
                DatagramPacket messageDP = new DatagramPacket(bufferMessage, bufferMessage.length);

                //receive the packet
                try {
                    multicastSocket.receive(messageDP);
                }catch (SocketException e){
                    if (Thread.currentThread().isInterrupted()){
                        multicastSocket.leaveGroup(inetAddress);
                        multicastSocket.close();
                    }
                }


                //add the new message on the chatHistory of this project
                String message = new String(messageDP.getData());
                mainClient.addMessage(nameProject, message);

            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
