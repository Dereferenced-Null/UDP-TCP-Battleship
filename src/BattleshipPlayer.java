import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.util.Arrays;
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
    protected boolean GAMEOVER = false;
    private int initalPort;

    private String [][] playerGame = new String[10][10];
    private String [][] enemyGame = new String[10][10];
    Battleship[] battleships = new Battleship[5];

    //create player game
    //create enemy game
    //create battleship class

    public BattleshipPlayer(){
        Random rand = new Random();
        initalPort = rand.nextInt(5001,6000);
        battleships[0] = new Battleship(5, "AIRCRAFT CARRIER");
        battleships[1] = new Battleship(4, "BATTLESHIP");
        battleships[2] = new Battleship(3, "CRUISER");
        battleships[3] = new Battleship(3, "SUBMARINE");
        battleships[4] = new Battleship(2, "PATROL BOAT");
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
            while(!done && clientSocket == null){
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
            clientSocket.close();
            serverSocket.close();
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
            playGame(reciever, in, out);
            out.close();
            in.close();
        }
        catch(Exception e){
            System.out.println(e);
            e.printStackTrace();
        }
    }

    private void playGame(boolean reciever, BufferedReader in, PrintWriter out){
        InputStreamReader systemInput = new InputStreamReader(System.in);
        BufferedReader systemIn = new BufferedReader(systemInput);
        String input = "";
        String input2 = "";
        String sInput = "";
        //checks if a message has been sent out
        boolean sent = false;
        //Allows for single message first time
        boolean first = true;
        try{
            while (!GAMEOVER){
                if(reciever){
                    //Receiving message
                    //gives the player something to look at while they wait for the fire command
                    if(!first){
                        //handles incoming messages after the first fire message
                        input2 = in.readLine();
                        if(input2 == null){
                            System.out.println("GAME OVER");
                            GAMEOVER = true;
                            break;
                        }
                        if(input2.matches("MISS:[A-Z]\\d*")){
                            String str[] = input2.split(":");
                            String str2[] = str[1].split("", 2);
                            enemyGame[Integer.parseInt(letterToNumber(str2[0])) - 1][Integer.parseInt(str2[1]) - 1] = "0";
                        }
                        else if(input2.matches("HIT:[A-Z]\\d*")){
                            String str[] = input2.split(":");
                            String str2[] = str[1].split("", 2);
                            enemyGame[Integer.parseInt(letterToNumber(str2[0])) - 1][Integer.parseInt(str2[1]) - 1] = "X";
                        }
                        else if(input2.matches("SUNK:[A-Z]\\d*:.*")){
                            String str[] = input2.split(":");
                            String str2[] = str[1].split("", 2);
                            enemyGame[Integer.parseInt(letterToNumber(str2[0])) - 1][Integer.parseInt(str2[1]) - 1] = "X";
                            System.out.println("YOU SUNK MY "+ str[2]);
                        }
                        else if(input2.matches("GAME OVER:[A-Z]\\d*:.*")){
                            String str[] = input2.split(":");
                            String str2[] = str[1].split("", 2);
                            enemyGame[Integer.parseInt(letterToNumber(str2[0])) - 1][Integer.parseInt(str2[1]) - 1] = "X";
                            System.out.println("GAME OVER");
                            GAMEOVER = true;
                            break;
                        }
                    }
                    printGames();
                    System.out.println("WAITING FOR OTHER PLAYERS TURN");
                    //Handles firing
                    input = in.readLine();
                    if(input.matches("FIRE:[A-Z]\\d*")){
                        String [] str = input.split(":");
                        out.println(calculateHit(str[1]));
                    }
                    reciever = false;
                    first = false;
                }
                else{
                    //Sending message
                    printGames();
                    System.out.println("YOUR TURN");
                    while(!sent){
                        sInput = systemIn.readLine();
                        out.println(sInput);
                        if(sInput.matches("FIRE:[A-Z]\\d*")){
                            String str[] = sInput.split(":");
                            String str2[] = str[1].split("", 2);
                            if(!str2[0].matches("[ABCDEFGHIJ]") || !str2[1].matches("\\d+")){
                                System.out.println("INVALID INPUT");
                            }
                            else if(Integer.parseInt(letterToNumber(str2[0])) > 10 || Integer.parseInt(str2[1]) > 10){
                                System.out.println("INVALID GRID SPACE");
                            }
                            else if(enemyGame[Integer.parseInt(letterToNumber(str2[0])) - 1][Integer.parseInt(str2[1]) - 1].matches("X")){
                                System.out.println("WE ALREADY HIT A SHIP THERE COMMANDER");
                            }
                            else{
                                out.println(sInput);
                                sent = true;
                            }
                        }
                        else{
                            System.out.println("INVALID INPUT");
                        }
                    }
                    reciever = true;
                    first = false;
                    sent = false;
                }
            }
        }
        catch(Exception e){
            System.out.println(e);
            e.printStackTrace();
        }
    }

    private void createGame(String[][] playerGame, String[][] enemyGame){
        Random rand = new Random();
        for(int x = 0; x < 10; x ++){
            for(int y = 0; y < 10; y++){
                enemyGame[x][y] = "~";
            }
        }

        boolean failed = false;
        int xRand;
        int yRand;
        for(int place = 0; place < 5; place ++){
            xRand = rand.nextInt(10);
            yRand = rand.nextInt(10);
            if(playerGame[xRand][yRand] == null){
                int size = battleships[place].getSize();
                boolean flip = rand.nextBoolean();
                if(flip){
                    if(xRand + 5 < 10){
                        for(int xPos = xRand; xPos < xRand+size; xPos ++){
                            if(playerGame[xPos][yRand] != null && !failed){
                                place --;
                                failed = true;
                            }
                        }
                        if(!failed){
                            int c = 0;
                            String[] coords = new String[size];
                            for(int xPos = xRand; xPos < xRand+size; xPos ++){
                                playerGame[xPos][yRand] = Integer.toString(size);
                                coords[c] = xPos +":"+yRand;
                                c++;
                            }
                            battleships[place].setCoords(coords);
                        }
                    }
                    else{
                        place--;
                    }
                    failed = false;
                }
                else{
                    if(yRand + 5 < 10){
                        for(int yPos = yRand; yPos < yRand+size; yPos ++){
                            if(playerGame[xRand][yPos] != null && !failed){
                                place --;
                                failed = true;
                            }
                        }
                        if(!failed){
                            int c = 0;
                            String[] coords = new String[size];
                            for(int yPos = yRand; yPos < yRand+size; yPos ++){
                                playerGame[xRand][yPos] = Integer.toString(size);
                                coords[c] = xRand +":"+yPos;
                                c++;
                            }
                            battleships[place].setCoords(coords);
                        }
                    }
                    else{
                        place--;
                    }
                    failed = false;
                }
            }
            else{
                place --;
            }
        }
        for(int x = 0; x < 10; x ++){
            for(int y = 0; y < 10; y++){
                if(playerGame[x][y] == null){
                    playerGame[x][y] = "~";
                }
            }
        }
    }

    private void printGames(){
        System.out.print("YOUR GAME\n");
        System.out.print("  1  2  3  4  5  6  7  8  9 10");
        for(int x = 0; x < 10; x ++){
            System.out.print("\n");
            System.out.print(numberToLetter(String.valueOf(x + 1)));
            for(int y = 0; y < 10; y++){
                System.out.print("["+playerGame[x][y]+ "]");
            }
        }
        System.out.print("\n\nENEMY COMMANDERS GAME\n");
        System.out.print("  1  2  3  4  5  6  7  8  9 10");
        for(int x = 0; x < 10; x ++){
            System.out.print("\n");
            System.out.print(numberToLetter(String.valueOf(x + 1)));
            for(int y = 0; y < 10; y++){
                System.out.print("["+enemyGame[x][y]+ "]");
            }
        }
        System.out.println("\n");
    }

    private String calculateHit(String coords){
        String output;
        String str[] = coords.split("",2);
        int coord1 = Integer.parseInt(letterToNumber(str[0])) - 1;
        int coord2 = Integer.parseInt(str[1]) - 1;
        if(playerGame[coord1][coord2].matches("~")){
            return "MISS:" + coords;
        }
        else{
            Battleship hitShip = null;
            for(Battleship battleship: battleships){
                if(battleship.hasCoord(Integer.toString(coord1) +":"+ Integer.toString(coord2))){
                    hitShip = battleship;
                }
            }
            output = hitShip.recordHit(Integer.toString(coord1) +":"+ Integer.toString(coord2));
            if(output.matches("HIT:")){
                playerGame[coord1][coord2] = "X";
                return output + coords;
            }
            else {
                int counter = 0;
                for (Battleship battleship : battleships) {
                    if (battleship.getSunk()) {
                        counter++;
                    }
                }
                //all battleships are sunk
                if (counter == battleships.length) {
                    String [] str2 = output.split(":");
                    GAMEOVER = true;
                    return "GAME OVER:" + coords + str2[1];
                }
                //not all battleships are sunk
                else{
                    playerGame[coord1][coord2] = "X";
                    String [] str2 = output.split(":" );
                    return str2[0] +":"+ coords +":"+ str2[1];
                }
            }
        }

    }

    private String numberToLetter(String number){
        int num = Integer.parseInt(number);
        num += 64;
        return String.valueOf((char)num);
    }

    private String letterToNumber(String letter){
        char[] letters = letter.toCharArray();
        int num = (int)letters[0];
        num -=64;
        return Integer.toString(num);
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
                    Thread.sleep(1000); //should be 30000
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

    private class Battleship{

        //hitpoints, set on battleship creation
        private int size;
        //location of battleship
        private String [] coords;
        private String name;
        private boolean sunk = false;

        public Battleship(int size, String name){
            this.size = size;
            this.name = name;
        }

        public int getSize(){
            return size;
        }

        public void setCoords(String[] coords){
            this.coords = coords;
        }

        public boolean hasCoord(String coordinate){
            for(String coord: coords){
                if(coord.matches(coordinate)){
                    return true;
                }
            }
            return false;
        }

        public String recordHit(String coordinate){
            int counter = 0;
            for(int i = 0; i < size; i ++){
                if(coords[i].matches(coordinate)){
                    coords[i] = "-1:-1";
                }
                if(coords[i].matches("-1:-1")){
                    counter += 1;
                }
            }
            if(counter == size){
                sunk = true;
                return "SUNK:" + name;
            }
            else{
                return "HIT:";
            }
        }

        public boolean getSunk(){
            return sunk;
        }
    }


}
