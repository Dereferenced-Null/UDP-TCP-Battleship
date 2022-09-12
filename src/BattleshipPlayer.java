import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Random;

public class BattleshipPlayer {

    //create player game
    //create enemy game
    //create battleship class

    public static void main(String [] args) {
        BattleshipPlayer client = new BattleshipPlayer();
        client.run();
    }

    public void run(){
        Random rand = new Random();
        String ping = "Josh";
        int initalPort = rand.nextInt(5001,6000);
        try{
            DatagramSocket socket = new DatagramSocket(5000);
            InetAddress address = InetAddress.getByName("192.168.0.255");
            socket.setBroadcast(true);
            Listener listener = new Listener(socket);
            listener.start();
            for(int targetPort = 4000; targetPort < 5000; targetPort ++){
                DatagramPacket packet = new DatagramPacket(ping.getBytes(), ping.length(), address, 5000);
                socket.send(packet);
            }
            socket.close();
        }
        catch(Exception e){
            System.out.println(e);
        }

    }

    private class Listener extends Thread{

        DatagramSocket socket;
        Listener(DatagramSocket socket){
            this.socket = socket;
        }

        public void run(){
            try{
                byte[] buffer = new byte[1024];
                DatagramPacket dataPacket = new DatagramPacket(buffer,1024);
                socket.receive(dataPacket);
                System.out.println(new String(buffer, 0, dataPacket.getLength()));
            }
            catch(Exception e){
                System.out.println(e);
            }
        }
    }

}
