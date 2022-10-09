// Programmer: Luke Haigh / c3303309
// Course: SENG 4500
// Last Modified: 10/10/2022
// Program Description: Battleship player program for playing battleship over a network. Uses a threaded implementation to
// create a UDP port connection which facilitates a TCP connection on which the game is played using battleships represented
// by a battleship class on matrices representing each player's game.

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.util.Random;

public class BattleshipPlayer extends Thread{
    protected boolean reciever = false;
    protected boolean done = false;
    protected boolean GAMEOVER = false;
    protected int broadcastPort;
    private String broadcastAddress;
    protected InetAddress host;
    protected int playerPort;
    protected int port;
    private final String [][] playerGame = new String[10][10];
    private final String [][] enemyGame = new String[10][10];
    Battleship[] battleships = new Battleship[5];

    //Constructor, creates the battleship player, players games and ships
    public BattleshipPlayer(String broadcastAddress, String broadcastPort){
        Random rand = new Random();
        this.port = rand.nextInt(5001, 6000);
        this.broadcastPort = Integer.parseInt(broadcastPort);
        this.broadcastAddress = broadcastAddress;
        battleships[0] = new Battleship(5, "AIRCRAFT CARRIER");
        battleships[1] = new Battleship(4, "BATTLESHIP");
        battleships[2] = new Battleship(3, "CRUISER");
        battleships[3] = new Battleship(3, "SUBMARINE");
        battleships[4] = new Battleship(2, "PATROL BOAT");
        createGame(playerGame, enemyGame);
    }

    public static void main(String [] args) {
        BattleshipPlayer player = new BattleshipPlayer(args[0], args[1]);
        player.run();
    }

    public void run(){
        try{
            Socket clientSocket = null;
            DatagramSocket socket = new DatagramSocket(broadcastPort);
            socket.setBroadcast(true);
            Sender sender = new Sender(socket);
            Listener listener = new Listener(socket, sender);
            sender.start();
            listener.start();
            ServerSocket serverSocket = new ServerSocket(port);
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
            System.out.println(playerPort + ":" + reciever);
            if(reciever){
                clientSocket = new Socket(host, playerPort);
            }
            sessionHandler(clientSocket);
            sleep(1000);
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
            printGames();
            System.out.println("GAME OVER");
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
                    if(!first){
                        //handles incoming messages after the first fire message
                        input2 = in.readLine();
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
                            GAMEOVER = true;
                            continue;
                        }
                    }
                    //print both games
                    //gives the player something to look at while they wait for their turn
                    printGames();
                    System.out.println("WAITING FOR OTHER PLAYERS TURN");
                    //Handles firing
                    input = in.readLine();
                    if(input.matches("FIRE:[A-Z]\\d*")){
                        String [] str = input.split(":");
                        String output = calculateHit(str[1]);
                        if(output.matches("GAME OVER:[A-Z]\\d*:.*")){
                            String str2[] = output.split(":");
                            String str3[] = str2[1].split("", 2);
                            enemyGame[Integer.parseInt(letterToNumber(str3[0])) - 1][Integer.parseInt(str3[1]) - 1] = "X";
                            GAMEOVER = true;
                            continue;
                        }
                        out.println(output);
                    }
                    reciever = false;
                    first = false;
                }
                else{
                    //Sending message
                    printGames();
                    System.out.println("YOUR TURN");
                    while(!sent){
                        System.out.print("FIRE:");
                        sInput = systemIn.readLine();
                        //Input error checking
                        if(sInput.matches("[A-Z]\\d*")){
                            String str[] = sInput.split("", 2);
                            if(!str[0].matches("[ABCDEFGHIJ]") || !str[1].matches("\\d+")){
                                System.out.println("INVALID INPUT");
                            }
                            else if(Integer.parseInt(letterToNumber(str[0])) > 10 || Integer.parseInt(str[1]) > 10){
                                System.out.println("INVALID GRID SPACE");
                            }
                            else if(enemyGame[Integer.parseInt(letterToNumber(str[0])) - 1][Integer.parseInt(str[1]) - 1].matches("X")){
                                System.out.println("WE ALREADY HIT A SHIP THERE COMMANDER");
                            }
                            else{
                                out.println("FIRE:" + sInput);
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
        catch(NullPointerException n){
            //this means that the other socket is closing indicating the game is over
            GAMEOVER = true;
        }
        //general exception
        catch(Exception e){
            System.out.println(e);
            e.printStackTrace();
        }
    }

    //Creates both player 1 and player 2's games locally and places local player's battleships
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

    //prints the matrices representing the states of both players games
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

    //checks if the coord selected by the other player is occupied or not and determines if any or all ships are sunk
    private String calculateHit(String coords){
        String output;
        String str[] = coords.split("",2);
        int coord1 = Integer.parseInt(letterToNumber(str[0])) - 1;
        int coord2 = Integer.parseInt(str[1]) - 1;
        if(playerGame[coord1][coord2].matches("~") || playerGame[coord1][coord2].matches("0")){
            playerGame[coord1][coord2] = "0";
            return "MISS:" + coords;
        }
        else{
            Battleship hitShip = null;
            for(Battleship battleship: battleships){
                if(battleship.hasCoord(coord1 +":"+coord2)){
                    hitShip = battleship;
                }
            }
            output = hitShip.recordHit(coord1 +":"+ coord2);
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
                    playerGame[coord1][coord2] = "X";
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

    //converts a number to ASCII letter for the Coord system
    private String numberToLetter(String number){
        int num = Integer.parseInt(number);
        num += 64;
        return String.valueOf((char)num);
    }

    //converts an ASCII letter to a number for the Coord system
    private String letterToNumber(String letter){
        char[] letters = letter.toCharArray();
        int num = (int)letters[0];
        num -=64;
        return Integer.toString(num);
    }

    //Message sender thread, handles all sent messages
    private class Sender extends Thread{

        DatagramSocket socket;

        public Sender(DatagramSocket socket){
            this.socket = socket;
        }

        //sends a message every 30 seconds until interrupted
        public void run(){
            String message = "NEW PLAYER:" + port;
            try{
                InetAddress address = InetAddress.getByName(broadcastAddress);
                socket.setBroadcast(true);
                while(true){
                    Thread.sleep(30000);
                    DatagramPacket packet = new DatagramPacket(message.getBytes(), message.length(), address, broadcastPort);
                    socket.send(packet);
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

    //Message Listening Thread, recieves all messages
    private class Listener extends Thread{

        DatagramSocket socket;
        //sender thread
        Sender sender;
        //Constructor
        Listener(DatagramSocket socket, Sender sender){
            this.sender = sender;
            this.socket = socket;
        }

        //Listens on the selected port until a valid new player packet is received
        //and then extracts port and IP to allow for TCP connection, interrupts sender
        //and terminates
        public void run(){
            try{
                byte[] buffer = new byte[1024];
                DatagramPacket dataPacket = new DatagramPacket(buffer,1024);
                String output;
                while(!done){
                    socket.receive(dataPacket);
                    output = new String(buffer, 0, dataPacket.getLength());
                    String str[] = output.split(":");
                    if(str[1].matches(Integer.toString(port))){
                        continue;
                    }
                    if(str[0].equals("NEW PLAYER")){
                        playerPort = Integer.parseInt(str[1]);
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

    //Battleship class for storing ship coords and hit information
    private class Battleship{

        private int size;
        //location of battleship
        private String [] coords;
        private String name;
        private boolean sunk = false;

        //Constructor
        public Battleship(int size, String name){
            this.size = size;
            this.name = name;
        }

        //checks if this ship is present in the parameter's Coord
        public boolean hasCoord(String coordinate){
            for(String coord: coords){
                if(coord.matches(coordinate)){
                    return true;
                }
            }
            return false;
        }

        //Records a hit to the battleship
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

        //Getters and Setters
        public boolean getSunk(){
            return sunk;
        }

        public int getSize(){
            return size;
        }

        public void setCoords(String[] coords){
            this.coords = coords;
        }
    }
}
