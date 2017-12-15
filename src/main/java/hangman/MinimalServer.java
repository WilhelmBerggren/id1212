/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hangman;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

/**
 *
 * @author Wilhelm
 */
public class MinimalServer {
    public static void main(String[] args) throws IOException {
        new MinimalServer().serve();
    }
    
    private void serve() {
        try {
            ServerSocket listeningSocket = new ServerSocket(8080);
            while(true) {
                Socket socket = listeningSocket.accept();
                Client client = new Client(socket);
                new Thread(client).start();
            }
        }
        catch (Exception e) {
            System.out.println(e);
        }
    }
    
    class Client implements Runnable {
        Socket socket;
        Boolean connected;
        private BufferedReader input;
        private PrintWriter output;
        
        public Client(Socket socket) {
            System.out.println("Client created");
            this.socket = socket;
            connected = true;
        }
        
        @Override
        public void run() {
            try {
                input = new BufferedReader(
                        new InputStreamReader(
                                socket.getInputStream()));
                output = new PrintWriter(socket.getOutputStream(), true);
                output.write(55);
                output.write(55);
                output.write(55);
                while(connected) {
                    System.out.println("This was received: "+input.readLine());
                    output.println("This was received: "+input.readLine());
                }
            }
            catch (Exception e) {
                System.out.println(e);
            }
        }
    }
}
