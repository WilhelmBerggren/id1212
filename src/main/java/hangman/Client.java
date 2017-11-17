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
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;

/**
 *
 * @author Wilhelm
 */
public class Client implements Runnable {

    private static String serverAddress;
    private final Socket socket;
    private final BufferedReader input;
    private final PrintWriter output;
    private boolean receivingCmds = false;
    private final Scanner console = new Scanner(System.in);
    private Boolean connected = false;
    private final int port = 8080;
    
    public static void main(String[] args) throws Exception {
        serverAddress = (args.length == 0) ? "localhost" : args[0];
        Client client = new Client(serverAddress);
        client.start();
    }
    
    public Client(String serverAddress) throws Exception {
        socket = new Socket(serverAddress, port);
        input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        output = new PrintWriter(socket.getOutputStream(), true);
    }

    private void start() {
        if(receivingCmds) {
            return;
        }
        receivingCmds = true;
        new Thread(this).start();
        new Thread(new Listener()).start();
    }

    @Override
    public void run() {
        if(!connected) {
            connect();
        }
        while (receivingCmds) {
            try {
                String msg = console.nextLine();
                CompletableFuture.runAsync(() -> output.println(msg));
            }
            catch (Exception e) { }
        }
    }
    
    public void connect() {
        CompletableFuture.runAsync(() -> {
            try {
                socket.connect(new InetSocketAddress(serverAddress, port));
                connected = true;
            }
            catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }
    
    private class Listener implements Runnable {

        @Override
        public void run() {
            while(true) {
                try {
                    String in = input.readLine();
                    if(in == null) 
                        break;
                    CompletableFuture.runAsync(() -> 
                        System.out.println(in)
                    );
                }
                catch (IOException e) {
                    CompletableFuture.runAsync(() -> System.out.println("Disconnected"));
                    break;
                }
            }
        }
        
    }
}