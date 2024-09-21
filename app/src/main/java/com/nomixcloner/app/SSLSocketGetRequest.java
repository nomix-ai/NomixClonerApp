package com.nomixcloner.app;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocket;

public class SSLSocketGetRequest {

    public static String makeGetRequest(String host, int port, String path, SocketFactory socketFactory) {
        SSLSocket socket = null;
        try {
            socket = (SSLSocket) socketFactory.createSocket(host, port);
            socket.startHandshake();
            OutputStream output = socket.getOutputStream();
            PrintWriter writer = new PrintWriter(output, true);
            writer.println("GET " + path + " HTTP/1.1");
            writer.println("Host: " + host);
            writer.println("Connection: close");
            writer.println();

            // Read the response
            InputStream input = socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            String line;
            StringBuilder stringBuilder = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }

            return stringBuilder.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            // Close the socket
            if (socket != null) {
                try {
                    socket.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
