package org.cp.pipboyclient;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

public class Discovery {
	private static final InetAddress BROADCAST_ADDRESS;
	private static final int DISCOVERY_PORT = 28000;
	
	static {
		try {
			BROADCAST_ADDRESS = InetAddress.getByName("255.255.255.255");
		} catch (UnknownHostException e) {
			throw new RuntimeException("Failed to retrieve broadcast address", e);
		}
	}
	
	/**
	 * Broadcasts a discovery command to port 28000 and returns any responses received.
	 * Waits until no response has been received for 5 seconds.
	 * 
	 * If multiple discover responses are returned from the same host this filters out
	 * any but the first.
	 */
	public static List<DiscoverResponse> discover() throws IOException {
		List<DiscoverResponse> responses = discoverRaw();
		
		Set<InetAddress> encounteredAddresses = new HashSet<>();
		List<DiscoverResponse> filteredResponses = new ArrayList<>();
		
		// only add to filtered list if we haven't encountered it
		for(DiscoverResponse response : responses) {
			if(encounteredAddresses.add(response.getMachineAddress())) {
				filteredResponses.add(response);
			}
		}
		
		return filteredResponses;
	}
	
	/**
	 * Broadcasts a discovery command to port 28000 and returns any responses received.
	 * Waits until no response has been received for 5 seconds.
	 * 
	 * Hosts seem to return multiple responses...this will return every response.
	 */
	public static List<DiscoverResponse> discoverRaw() throws IOException {
		List<DiscoverResponse> responses = new ArrayList<>();
		
		try(DatagramSocket datagramSocket = new DatagramSocket()) {
			Gson gson = new Gson();
			
			datagramSocket.setBroadcast(true);
			
			final byte[] discoveryPayload = gson.toJson(new DiscoverCommand()).getBytes();
			
			DatagramPacket autodiscoveryPacket = new DatagramPacket(discoveryPayload, discoveryPayload.length, 
					BROADCAST_ADDRESS, DISCOVERY_PORT);
			
			datagramSocket.send(autodiscoveryPacket);
			
			byte[] responseBuffer = new byte[4096];
			DatagramPacket responsePacket = new DatagramPacket(responseBuffer, responseBuffer.length);
			
			datagramSocket.setSoTimeout(5000);
			
			try {
				while(true) {
					datagramSocket.receive(responsePacket);
					DiscoverResponse discoverResponse = gson.fromJson(
							new String(responsePacket.getData(), 0, responsePacket.getLength()), DiscoverResponse.class);
					
					discoverResponse.setAddress(responsePacket.getAddress());
					
					responses.add(discoverResponse);
				}
			} catch (SocketTimeoutException e) {
				// no response without timeout
			}
		}	
		
		return responses;
	}
	
	public static class DiscoverCommand {
		private String cmd = "autodiscover";
	}
	
	public static class DiscoverResponse {
		@SerializedName("IsBusy")
		private boolean isBusy;
		
		@SerializedName("MachineType")
		private String machineType;
		
		private InetAddress machineAddress;

		public DiscoverResponse(boolean isBusy, String machineType, InetAddress machineAddress) {
			super();
			this.isBusy = isBusy;
			this.machineType = machineType;
			this.machineAddress = machineAddress;
		}
		
		public DiscoverResponse() {
			
		}
		
		public void setAddress(InetAddress machineAddress) {
			this.machineAddress = machineAddress;
		}
		
		public boolean isBusy() {
			return isBusy;
		}
		
		public InetAddress getMachineAddress() {
			return machineAddress;
		}
		
		public String getMachineType() {
			return machineType;
		}
		
		@Override
		public String toString() {
			return "DiscoverResponse [isBusy=" + isBusy + ", machineType=" + machineType 
					+ ", machineAddress=" + machineAddress + "]";
		}		
	}
}
