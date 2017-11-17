package hangman;


import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Random;

/**
 *
 * @author Wilhelm
 */
public class Server {
    
    public static void main(String[] args) {
        Server server = new Server();
        server.serve();
    }
    
    private void serve() {
        try {
            ServerSocket listeningSocket = new ServerSocket(8080);
            Words words = new Words("words.txt");
            while(true) {
                Socket clientSocket = listeningSocket.accept();
                GameInstance game = new GameInstance(this, clientSocket, words);
                Thread handlerThread = new Thread(game);
                System.out.println("Starting new game");
                handlerThread.start();
            }
        }
        catch (IOException e) {
            System.out.println("Server failure "+e);
        }
    }
}

class Words {
    private final ArrayList<String> words = new ArrayList<String>();

    Words(String path) {
        try {
            BufferedReader br = new BufferedReader(
                                    new InputStreamReader(
                                        new FileInputStream(path)));
            String line;
            while((line = br.readLine()) != null) {
                words.add(line);
            }
            br.close();
        }
        catch (FileNotFoundException e) {
            System.out.println("No words file found");
        }
        catch (IOException f) {
            System.out.println("Error reading line in words file");
        }
    }

    String getWord() {
        Random rand = new Random();
        int index = rand.nextInt(words.size());
        return words.get(index);
    }
}

class GameInstance implements Runnable {

    private boolean connected;
    private final Socket clientSocket;
    private BufferedReader input;
    private PrintWriter output;
    private final Words words;
    private Game game;
    int score = 0;

    GameInstance(Server server, Socket clientSocket, Words words) {
        this.clientSocket = clientSocket;
        this.words = words;
        connected = true;
    }

    @Override
    public void run() {
        try {
            input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            output = new PrintWriter(clientSocket.getOutputStream(), true);
            output.println("Connected");
            this.game = new Game(words);
            output.println(this.game.status());
            String[] guess;
            while (connected) {
                guess = input.readLine().split(" ");
                if(guess.length == 1) {
                    if(guess[0].equals("quit")) {
                        try {
                            clientSocket.close();
                        }
                        catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    else {
                        output.println("Error parsing command");
                    }
                }
                else if(guess.length == 2 && guess[0].equals("guess")) {
                    game.makeGuess(guess[1]);
                    output.println(game.status());
                    if(game.finished) {
                        if(game.lost)   { score--; }
                        else            { score++; }
                        
                        this.game = new Game(words);
                        output.println(game.status() + " Score: "+score);
                    }
                }
                else {
                    output.println("Error parsing command");
                }
                
            }
        }
        catch (IOException e) {
            System.out.println("Disconnected: "+e);
        }
        finally {
            try {
                clientSocket.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            connected = false;
            System.out.println("User disconnected");
        }
    }
}

class Game {
    private final Words words;
    private final String word;
    private String hiddenWord;
    private int lives;
    boolean finished = false;
    boolean lost = false;
    private final ArrayList<String> guesses;
    private final ArrayList<String> help;
    
    Game(Words words) {
        this.guesses = new ArrayList<>();
        this.help = new ArrayList<>();
        this.words = words;
        this.word = words.getWord();
        this.hiddenWord = word.replaceAll(".","_");
        this.lives = word.length();
    }
    
    void makeGuess(String input) {
        if(input.equals(word)) { 
            finished = true;
            return; 
        }
        boolean wrongGuess = false;
        if(input.length() == 1) { 
            char guess = input.charAt(0); 
            char[] updatedHiddenWord = hiddenWord.toCharArray();
            int change = 0;
            for(int i = 0; i < word.length(); i++) {
                char c;
                if((c = word.charAt(i)) == guess) {
                    updatedHiddenWord[i] = c;
                    change++;
                }
            }
            if(change == 0) {
                wrongGuess = true;
            }
            hiddenWord = new String(updatedHiddenWord);
        }
        else {
            wrongGuess = true;
        }
        if(wrongGuess) {
            lives--;
            if(lives == 0) {
                lost = true;
                finished = true;
            }
            guesses.add(suggestion());
            guesses.add(input);
        }
    }
    
    String suggestion() {
        Random rand = new Random();
        char c;
        String notTheWord;
        while(true) {
            c = (char) (rand.nextInt('z' - 'a') + 'a');
            if(word.contains(c+"") && !guesses.contains(c+""))
                return c+"";
            notTheWord = words.getWord();
            if(!word.equals(notTheWord) && word.length() == notTheWord.length())
                return notTheWord;
        }
    }
    
    String status() {
        if(lives == 0) {
            return "You lose";
        }
        if(hiddenWord.equals(word)) {
            return "You win";
        }
        return hiddenWord+ " Lives: "+lives+" Not in word: "+guesses.toString();
    }
}