package com.my.p2p;
import java.net.*;

public class P2PClient {
    
    private InetAddress address;
    private int port;
    private DatagramSocket socket;
    
    public P2PClient(String host, int port) throws P2PClientCreateException {
        try
        {
            address = InetAddress.getByName(host);
            this.port = port;
            socket = new DatagramSocket();
        } catch (SocketException | UnknownHostException e) {
            throw new P2PClientCreateException();
        }
    }
    
    private 
    
    public class P2PClientCreateException extends Exception {
        
    }
}
