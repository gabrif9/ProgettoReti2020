import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Random;

public class MIPManager {

    private ArrayList<String> MIPAddress; //IP  address already assigned


    public MIPManager() {
        MIPAddress = new ArrayList<>();
    }

    //https://stackoverflow.com/questions/9236197/generate-random-ip-address/9236244
    public synchronized String createRandomIP() {

        //generate a random multicast ip address
        String IPAddress = randomNumber(239, 224) + "." + randomNumber(255, 0) + "." + randomNumber(255, 0) + "." + randomNumber(25, 0);
        try {
            if (InetAddress.getByName(IPAddress).isMulticastAddress()){
                if (MIPAddress.contains(IPAddress)){
                    return createRandomIP();
                }
                MIPAddress.add(IPAddress);
            }
        } catch (UnknownHostException e){
            return createRandomIP();
        }
    return IPAddress;
    }

    public int randomNumber(int high, int low) {
        return new Random().nextInt(high - low) + low;
    }

    public synchronized void removeIPAddress(String IPToRemove){
        if (!MIPAddress.contains(IPToRemove)) throw new IllegalArgumentException("IP not found");

        MIPAddress.remove(IPToRemove);
    }




}
