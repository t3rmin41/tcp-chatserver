package com.simple.tcp.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SocketClient implements Runnable {
    
    private static Logger log = LoggerFactory.getLogger(SocketClient.class);
    
    private Long id;
    private Long remoteId;
    private Socket localSocket;
    private BufferedReader localInput;
    private PrintWriter localOutput;
    private PrintWriter remoteOutput;
        
    public SocketClient(Long id, Socket localSocket) {
        this.id = id;
        this.localSocket = localSocket;
    }
    
    public void setRemoteId(Long rid) {
        this.remoteId = rid;
    }

    public Socket getLocalSocket() {
        return this.localSocket;
    }
    
    public PrintWriter getRemoteOutput() {
        return this.remoteOutput;
    }
    
    public void setRemoteOutput(PrintWriter output) {
        this.remoteOutput = output;
    }
    
    private void initLocal() {
        try {
            localInput = new BufferedReader(new InputStreamReader(this.localSocket.getInputStream()));
            localOutput = new PrintWriter(new BufferedWriter(new OutputStreamWriter(localSocket.getOutputStream())), true);
            remoteId = id; //Initially, set remote socket ID to local socket ID
            remoteOutput = localOutput;
        } catch (IOException e) {
            e.printStackTrace();
        }
        log.info("Connection accepted: " + localSocket + " in thread ID = " + id);
    }
    
    public void connectRemoteAndLocal(Set<Long> availableSocketId) throws IOException {
        localOutput.println("Available sockets:");
        for (Long clientid : availableSocketId) {
            if (clientid != this.id) {
                localOutput.println("ID = " + clientid);
            }
        }
        localOutput.println("Insert socket ID to communicate with and press ENTER");
        
        String remoteSocketId = localInput.readLine(); // wait for remote socket ID input
        
        initRemote(remoteSocketId);
        
        remoteOutput.println("Client with ID : " + id + " requested communication with you. Starting communication");
        localOutput.println("Starting communication with ID : " + remoteId);
    }
    
    private void initRemote(String remoteSocketId) {
        try {
            remoteId = Long.parseLong(remoteSocketId);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        try {
            remoteOutput = new PrintWriter(new BufferedWriter(
                    new OutputStreamWriter(
                            MultiClientServer.getSocketById(remoteId).getOutputStream()
                    )
                ), true);

            SocketClient remoteClient =  MultiClientServer.getSocketClientById(remoteId);

            // on the remote connection, set remote output to this local output so that whatever typed on local, is observed on remote
            remoteClient.setRemoteOutput(localOutput);
            remoteClient.setRemoteId(id);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void cleanUp() throws IOException {
        MultiClientServer.removeClient(id);
        localOutput.close();
        localInput.close();
        localSocket.close();
    }
    
    @Override
    public void run() {
        try {
            initLocal();
            
            Set<Long> socketSet = MultiClientServer.getClientList();

            if (socketSet.size() == 1) {
                localOutput.println("You are the only client on server with ID = " + id);
                localOutput.println("Please wait for other participants or you may use echo server");
            } else if (socketSet.size() > 1) {
                localOutput.println("You have connected with ID = " + id);
                connectRemoteAndLocal(socketSet);
            }

            while(true) {
                String messageString = localInput.readLine(); // Read message line by line
                if (messageString == null) {
                    log.info("Client ID =  " + id + " closed connection with ID = " + remoteId);
                    break;
                }
                if ("END".equals(messageString)) {
                    log.info("Client ID = " + id + " requested connection close with ID = " + remoteId);
                    remoteOutput.println("ID = " + id + " requested communication end with you");
                    break;
                }
                remoteOutput.println("ID =  " + (id.equals(remoteId) ? id + " (you)" : id)  + " : " + messageString);
                log.info("ID = " + id + " transmitted to ID = " + remoteId + " message : " + messageString);
            }
            
            cleanUp();
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}