package com.simple.tcp.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MultiClientServer {
    private static Logger log = LoggerFactory.getLogger(MultiClientServer.class);
    
    private static final Map<Long, SocketClient> connectionMap = new HashMap<Long, SocketClient>();
    
    private static final int PORT = 8888;
    
    private static Long id = 0L;
    
    public static void main(String[] args) throws IOException {
        InetAddress host = null;
        try {
            host = InetAddress.getByAddress(new byte[] {0x00, 0x00, 0x00, 0x00});
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        ServerSocket serverSocket = new ServerSocket(PORT, 0, host); // creates physical listening socket on machine
        log.info("Started server listening on " + PORT + " at " + host.getCanonicalHostName());
        try {
            while(true) {
                synchronized(id) {
                    Socket socket = serverSocket.accept(); // creates actual new socket from ServerSocket
                    connectionMap.put(id, new SocketClient(id, socket));
                    Thread socketThread = new Thread(connectionMap.get(id));
                    socketThread.start();
                    id++;
                }
            }// end of synchronized block
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            serverSocket.close();
        }
    }
    
    public static Set<Long> getClientList() {
        return connectionMap.keySet();
    }
    
    public static Socket getSocketById(Long id) {
        return connectionMap.get(id).getLocalSocket();
    }
    
    public static SocketClient getSocketClientById(Long id) {
        return connectionMap.get(id);
    }
    
    public static void removeClient(Long id) {
            connectionMap.remove(id);
    }
}