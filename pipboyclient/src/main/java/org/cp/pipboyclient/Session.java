package org.cp.pipboyclient;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;

import org.cp.pipboyclient.FalloutControl.RadioToggle;
import org.cp.pipboyclient.FalloutControl.RadioToggle.RadioStation;

import com.google.gson.Gson;

public class Session {
	private static int SESSION_PORT = 27000;
	
	public static void connect(InetAddress address) throws IOException {
		Socket socket = new Socket(address, SESSION_PORT);
		
		OutputStream os = socket.getOutputStream();
		DataInputStream is = new DataInputStream(socket.getInputStream());
		
		long lastControlSent = System.currentTimeMillis();
		
		// looks like a four byte length and a message type byte
		while (true) {
			int lengthByte1 = is.read();
			int lengthByte2 = is.read();
			int lengthByte3 = is.read();
			int lengthByte4 = is.read();
			int messageLength = lengthByte1 + (lengthByte2<<8) + (lengthByte3<<16) + (lengthByte4<<24);
			
			int messageType = is.read();
			
			byte[] messagePayload = new byte[messageLength];
			is.readFully(messagePayload);
			
			boolean messageHandled = true;

			if(messageType == 1) {
				System.out.println("Received JSON Preamble: " + new String(messagePayload));
				System.out.println();
			} else if(messageType == 0 &&  messageLength == 0) {
				// keepalive...lots of these so don't print them
				// System.out.println("Received keepalive...");
				// send a keepalive response
			} else {
				messageHandled = false;
			}
			
			if(!messageHandled) {
				System.out.print("Unhandled Message: ");
				System.out.print("length=" + messageLength + ", type=" + messageType);
				if(messageLength < 5000) {
					System.out.print(" hex=");
					
					for(byte b : messagePayload) {
						System.out.print(Integer.toHexString(0xFF&b) + " ");
					}
					System.out.println();
					System.out.print("asString=");
					System.out.println(new String(messagePayload));
				}
				System.out.println();
			}

			// send a keepalive in response to any messages
			os.write(new byte[]{0, 0, 0, 0, 0});

			if(System.currentTimeMillis() - lastControlSent > 5000) {
				sendFalloutControl(new RadioToggle(RadioStation.DiamondCityRadio), os);
				lastControlSent = System.currentTimeMillis();
			}
		}
	}
	
	public static void sendFalloutControl(FalloutControl control, OutputStream os) throws IOException {
		String json = new Gson().toJson(control);
		
		System.out.println("Sending: " + json);
		
		byte[] payload = json.getBytes();
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		baos.write(payload.length & 0xFF);
		baos.write((payload.length>>8)&0xFF);
		baos.write((payload.length>>16)&0xFF);
		baos.write((payload.length>>24)&0xFF);
		baos.write(0x05);
		baos.write(payload);
		
		os.write(baos.toByteArray());
	}
}
