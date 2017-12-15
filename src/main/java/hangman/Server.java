package hangman;


import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

/**
 *
 * @author Wilhelm
 */
public class Server {
    private Selector selector;
    private ServerSocketChannel listeningSocketChannel;
    
    public static void main(String[] args) {
        Server server = new Server();
        server.serve();
    }
    
    private void serve() {
        try {
            selector = Selector.open();
            selector.select();
            
            listeningSocketChannel = ServerSocketChannel.open();
            listeningSocketChannel.configureBlocking(false);
            listeningSocketChannel.bind(new InetSocketAddress(8080));
            listeningSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
            
            Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
            
            while(iterator.hasNext()) {
                SelectionKey key = iterator.next();
                iterator.remove();
                
                if (!key.isValid()) {
                        continue;
                    }
                    if (key.isAcceptable()) {
                        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
                        SocketChannel clientChannel = serverSocketChannel.accept();
                        clientChannel.configureBlocking(false);
                        ClientHandler handler = new ClientHandler(this, clientChannel);
                        clientChannel.register(selector, SelectionKey.OP_WRITE, 
                                new Client(handler));
                    } else if (key.isReadable()) {
                        recvFromClient(key);
                    } else if (key.isWritable()) {
                        sendToClient(key);
                    }
            }
        }
        catch (Exception e) {
            System.out.println("Server failure "+e);
        }
    }

    private static class ClientHandler implements Runnable {

        private final Server server;
        private final SocketChannel clientChannel;

        ClientHandler(Server server, SocketChannel clientChannel) {
            this.server = server;
            this.clientChannel = clientChannel;
        }

        @Override
        public void run() {
            while()
        }
    }

    class Game {
        private final Words words = new Words("words.txt");
        private final String word;
        private String hiddenWord;
        private int lives;
        boolean finished = false;
        boolean lost = false;
        private final ArrayList<String> guesses;
        private final ArrayList<String> help;

        Game() {
            this.guesses = new ArrayList<>();
            this.help = new ArrayList<>();
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
                return "You lose. Word was "+word;
            }
            if(hiddenWord.equals(word)) {
                return "You win";
            }
            return hiddenWord+ " Lives: "+lives+" Not in word: "+guesses.toString();
        }


        private class Words {
            private ArrayList<String> words = new ArrayList<>();

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
                catch (Exception e) {
                    System.out.println("Error reading file "+e);
                }
            }

            String getWord() {
                Random rand = new Random();
                int index = rand.nextInt(words.size());
                String word = words.get(index);
                words.remove(index);
                return word;
            }
        }
    }
}