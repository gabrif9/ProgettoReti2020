import java.io.IOException;
import java.net.*;

public class ProjectChatSniffer implements Runnable{

    String MIPAddress;
    String nameProject;
    InetAddress inetAddress;
    MulticastSocket multicastSocket;
    MainClient mainClient;
    private static final int MULTICAST_PORT = 6789;


    public ProjectChatSniffer(String MIPAddress, String nameProject, MainClient mainClient) {
        this.MIPAddress = MIPAddress;
        this.nameProject = nameProject;
        this.mainClient = mainClient;
    }

    @Override
    public void run() {

        try {
            inetAddress = InetAddress.getByName(MIPAddress);
            multicastSocket = new MulticastSocket(MULTICAST_PORT);
            multicastSocket.setSoTimeout(4000);
            multicastSocket.setLoopbackMode(false);
            //join in the multicastGroup
            multicastSocket.joinGroup(inetAddress);
            System.out.println("in ascolto sull'indirizzo: " + MIPAddress);

            while (true){
                ///prepare the datagram packet for receive the message
                byte[] bufferMessage = new byte[8192];
                DatagramPacket messageDP = new DatagramPacket(bufferMessage, bufferMessage.length);

                //receive the packet
                try {
                    //System.out.println("Cerco messaggi su: " + MIPAddress);
                    multicastSocket.receive(messageDP);
                    System.out.println("Message received");
                    //add the new message on the chatHistory of this project
                    String message = new String(messageDP.getData(), 0, messageDP.getLength());
                    mainClient.addMessage(nameProject, message);
                }catch (SocketTimeoutException e){
                    if (Thread.currentThread().isInterrupted()){
                        multicastSocket.leaveGroup(inetAddress);
                        multicastSocket.close();
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
