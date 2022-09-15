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

    private String [][] playerGame = new String[10][10];
    private String [][] enemyGame = new String[10][10];
    BattleShip[] battleShips = new BattleShip[5];

    //create player game
    //create enemy game
    //create battleship class

    public BattleshipPlayer(){
        Random rand = new Random();
        initalPort = rand.nextInt(5001,6000);
        battleShips[0] = new BattleShip(5, "Aircraft Carrier");
        battleShips[1] = new BattleShip(4, "Battleship");
        battleShips[2] = new BattleShip(3, "Cruiser");
        battleShips[3] = new BattleShip(3, "Submarine");
        battleShips[4] = new BattleShip(2, "Patrol Boat");
        createGame(playerGame, enemyGame);
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
            //GAME LOOP
            playGame(reciever, in);
            out.close();
            in.close();



        }
        catch(Exception e){
            System.out.println(e);
            e.printStackTrace();
        }
    }

    private void playGame(boolean reciever, BufferedReader in){
        String input = "";
        printGames();
        try{
            while (!input.equals("GAME OVER"))
                if(!reciever){
                    //Receiving message
                    input = in.readLine();
                    reciever = false;
                }
                else{
                    //Sending message
                    //GAME LOOP

                }
        }
        catch(Exception e){

        }
    }

    private void createGame(String[][] playerGame, String[][] enemyGame){
        Random rand = new Random();
        for(int x = 0; x < 10; x ++){
            for(int y = 0; y < 10; y++){
                enemyGame[x][y] = "X";
            }
        }

        int x = rand.nextInt(10);
        int y = rand.nextInt(10);
        for(int place = 0; place < 5; place ++){
            if(playerGame[x][y] == null){
                int size = battleShips[place]. getSize();
                boolean flip = rand.nextBoolean();
                //check if there is room on the x axis if boolean is true checking to make sure that x will not exceed 10 or go under 0
                //if there is enough free spaces, place, otherwise, check if there is room on the y axis.
                //if there is not, decrement place by 1, try again
                //if placed then continue
            }
        }
    }

    private void printGames(){
        System.out.print("YOUR GAME");
        for(int x = 0; x < 10; x ++){
            System.out.print("\n");
            for(int y = 0; y < 10; y++){
                System.out.print("["+playerGame[x][y]+ "]");
            }
        }
        System.out.print("\n\nENEMY COMMANDERS GAME");
        for(int x = 0; x < 10; x ++){
            System.out.print("\n");
            for(int y = 0; y < 10; y++){
                System.out.print("["+enemyGame[x][y]+ "]");
            }
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
                    Thread.sleep(10); //should be 30000
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

    private class BattleShip{

        //hitpoints, set on battleship creation
        private int size;
        private String [] coords;
        private String name;

        public BattleShip(int size, String name){
            this.size = size;
            this.name = name;
        }

        public int getSize(){
            return size;
        }

        public void setCoords(int[][] XY){
            //the coords representing where the ship is.
        }
    }


}
