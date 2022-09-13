import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.util.Random;

public class BattleshipPlayer extends Thread{

    //note: THIS SHOULD NEVER BE WRITTEN TO BY ANY NON LISTENER CLASS WITHOUT
    // A SEMAPHORE AS THIS WILL RESULT IN A RACE CONDITION
    protected int port;
    protected InetAddress host = null;
    protected ServerSocket serverSocket = null;
    protected Socket socket = null;
    protected boolean reciever = false;
    protected boolean done = false;
    private int initalPort;

    //create player game
    //create enemy game
    //create battleship class

    public BattleshipPlayer(){
        Random rand = new Random();
        initalPort = rand.nextInt(5001,6000);
    }

    public static void main(String [] args) {
        BattleshipPlayer player = new BattleshipPlayer();
        player.run();
    }

    public void run(){
        try{
            Socket clientSocket = null;
            DatagramSocket socket = new DatagramSocket(initalPort);
            socket.setBroadcast(true);
            Sender sender = new Sender(socket);
            Listener listener = new Listener(socket, sender);
            sender.start();
            listener.start();
            ServerSocket serverSocket = new ServerSocket(initalPort);
            serverSocket.setSoTimeout(10);
            while(done == false && clientSocket == null){
                try{
                    clientSocket = serverSocket.accept();
                }
                catch (SocketTimeoutException e)
                {
                    continue;
                }
            }
            listener.interrupt();
            sender.interrupt();
            socket.close();
            System.out.println(port + ":" + reciever);
            if(reciever){
                clientSocket = new Socket(host, port);
            }
            sessionHandler(clientSocket);
            System.out.println("Here");

        }
        catch(Exception e){
            System.out.println(e);
            e.printStackTrace();
        }
    }

    private void sessionHandler(Socket clientSocket){
        try{
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            String input = "";
            if(!reciever){
                input = in.readLine();
            }
            else{
                out.println("Start");
            }
            //GAME LOOP
            System.out.println(input);

        }
        catch(Exception e){
            System.out.println(e);
            e.printStackTrace();
        }
    }

    private class Sender extends Thread{

        DatagramSocket socket;

        public Sender(DatagramSocket socket){
            this.socket = socket;
        }

        public void run(){
            String message = "NEW PLAYER:" + initalPort;
            System.out.println(initalPort);
            try{
                InetAddress address = InetAddress.getByName("255.255.255.255");
                socket.setBroadcast(true);
                while(port == 0){
                    Thread.sleep(30000);
                    for(int targetPort = 5000; targetPort < 6001; targetPort ++){
                        DatagramPacket packet = new DatagramPacket(message.getBytes(), message.length(), address, targetPort);
                        socket.send(packet);
                    }
                }
            }
            catch(InterruptedException e){
                return;
            }
            catch(SocketException e){
                return;
            }
            catch(Exception e){
               return;
            }
        }
    }

    private class Listener extends Thread{

        DatagramSocket socket;
        Sender sender;
        Listener(DatagramSocket socket, Sender sender){
            this.sender = sender;
            this.socket = socket;
        }

        public void run(){
            try{
                byte[] buffer = new byte[1024];
                DatagramPacket dataPacket = new DatagramPacket(buffer,1024);
                String output;
                while(!done){
                    socket.receive(dataPacket);
                    if(dataPacket.getPort() == initalPort){
                        continue;
                    }
                    output = new String(buffer, 0, dataPacket.getLength());
                    String str[] = output.split(":");
                    if(str[0].equals("NEW PLAYER")){
                        port = Integer.parseInt(str[1]);
                        host = dataPacket.getAddress();
                        done = true;
                        reciever = true;
                        sender.interrupt();
                    }
                }
            }
            catch(SocketException e){
                return;
            }
            catch(Exception e){
                System.out.println(e);
            }
        }
    }

}
