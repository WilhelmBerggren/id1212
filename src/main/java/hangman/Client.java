/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hangman;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.Scanner;

/**
 *
 * @author Wilhelm
 */
public class Client implements Runnable {
    private final SynchronizedOut output = new SynchronizedOut();
    private boolean receivingCmds = false;
    private final Scanner console = new Scanner(System.in);
    private Boolean connected = false;
    private Server server;
    
    public static void main(String[] args) throws Exception {
        new Client().start();
    }
    private InetSocketAddress serverAddress;

    private void start() {
        if(receivingCmds) {
            return;
        }
        receivingCmds = true;
        server = new Server();
        new Thread(this).start();
    }

    @Override
    public void run() {
        while (receivingCmds) {
            try {
                String msg = console.nextLine();
                String[] args = msg.split(" ");
                switch (args[0]) {
                    case "quit":
                        receivingCmds = false;
                        server.disconnect();
                        break;
                    case "connect":
                        server.connect(args[1]);
                        break;
                    default:
                        server.send(msg);
                }
            }
            catch (Exception e) { 
                output.println("Error: "+e);
            }
        }
    }
    
    public void connect(String host, int port) {
        serverAddress = new InetSocketAddress(host,port);
        new Thread(this).start();
    }
    
    public void disconnect() throws IOException {
        connected = false;
    }
    
    private class Server implements Runnable {
        
        private SocketChannel socketChannel;
        private InetSocketAddress serverAddress;
        private Selector selector;
        private final ByteBuffer fromServer = ByteBuffer.allocateDirect(1024);
        private final Queue<ByteBuffer> toServer = new ArrayDeque<>();
        private volatile boolean timeToSend = false;

        @Override
        public void run() {
            try {
                socketChannel = SocketChannel.open();
                socketChannel.configureBlocking(false);
                socketChannel.connect(serverAddress);
                connected = true;
                
                selector = Selector.open();
                socketChannel.register(selector, SelectionKey.OP_CONNECT);
                selector.select();
                
                while (connected || !toServer.isEmpty()) {
                    if (timeToSend) {
                        socketChannel.keyFor(selector).interestOps(SelectionKey.OP_WRITE);
                    }
                    
                    for (SelectionKey key : selector.selectedKeys()) {
                        selector.selectedKeys().remove(key);
                        if (!key.isValid()) {
                            continue;
                        }
                        if (key.isConnectable()) {
                            socketChannel.finishConnect();
                            key.interestOps(SelectionKey.OP_READ);
                            output.println("Connected");
                        }
                        else if (key.isReadable()) {
                            fromServer.clear();
                            int bytes = socketChannel.read(fromServer);
                            if(bytes == -1) {
                                throw new IOException("Lost Connection");
                            }
                            fromServer.flip();
                            output.println(new String(fromServer.array()));

                        }

                    }
                }
            }
            catch (Exception e) {
                System.err.println("Lost connection "+e);
            }
        }

        private void disconnect() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        private void connect(String arg) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        private void send(String msg) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
        
    }
    
    private class Listener {
        public void receive(String msg) {
            output.println(msg);
        }
        public void connected() {
            output.println("Connected");
        }
        public void disconnected() {
            output.println("Disconnected");
        }
    }
    
    private class SynchronizedOut {
        synchronized void print(String out) {
            System.out.print(out);
        }
        synchronized void println(String out) {
            System.out.println(out);
        }
    }
}