/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Queue;

/**
 *
 * @author Wilhelm
 */
public class Server {
    public static void main(String[] args) throws Exception {
        new Server().serve();
    }
    
    ServerSocketChannel serverChannel;
    Selector selector;
    SelectionKey serverKey;
    
    void serve() throws Exception {
        serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);
        serverKey = serverChannel.register(selector = Selector.open(), SelectionKey.OP_ACCEPT);
        serverChannel.bind(new InetSocketAddress("localhost", 8080));
        
        while(true) {
            selector.selectNow();
            
            for (SelectionKey key : selector.keys()) {
                if(!key.isValid()) continue;
                
                if(key.isAcceptable()) {
                    SocketChannel acceptedChannel = serverChannel.accept();
                    if(acceptedChannel == null) continue;
                    acceptedChannel.configureBlocking(false);
                    acceptedChannel.register(selector, SelectionKey.OP_READ,
                            new Client(key, acceptedChannel));
                    System.out.println("New client");
                }
                
                if(key.isReadable()) {
                    Client client = (Client) key.attachment();
                    client.read();
                }
            }
            
            selector.selectedKeys().clear();
        }
    }
}

class ClientHandler implements Runnable {
    Server server;
    SocketChannel clientChannel;
    SelectionKey key;
    ByteBuffer clientMsg = ByteBuffer.allocateDirect(1024);
    Queue<ByteBuffer> messages = new ArrayDeque<>();
    
    public ClientHandler(Server server, SocketChannel clientChannel) {
        this.server = server;
        this.clientChannel = clientChannel;
    }

    @Override
    public void run() {
        Iterator i = messages.iterator();
        while(i.hasNext()) {
            clientChannel.write(i.next());
            
        }
    }
    
    void getMsg() throws Exception {
        clientMsg.clear();
        int bytes = -1;
        bytes = clientChannel.read((ByteBuffer) clientMsg.clear());
        if (bytes == -1) disconnectClient();
        if (bytes < 1) return;
        
        messages.add(bytes);
    }
    
    ByteBuffer readMsgFromBuffer() throws Exception {
        clientMsg.flip();
        byte[] bytes = new byte[clientMsg.remaining()];
        clientMsg.get(bytes);
        return new String(bytes);
    }
    
    void disconnectClient() throws Exception {
        clientChannel.close();
    }
}

class Client {
    SelectionKey key;
    SocketChannel channel;
    ByteBuffer bf;
    
    Client(SelectionKey key, SocketChannel channel) throws Exception {
        this.key = key;
        this.channel = (SocketChannel) channel.configureBlocking(false);
        bf = ByteBuffer.allocate(1024);
    }
    
}