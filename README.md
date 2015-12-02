# PipBoyClient
Just experimenting with some code to act as a Fallout 4 Pip-Boy client after sniffing traffic from the app

Example code shows:
1.) A UDP Broadcast on 28000 which Fallout 4 will send a unicast response to.
2.) Picking the first available responder to connect to (establish TCP connection to port 27000 with Fallout 4 System IP)
3.) Spitting out hex for messages received from the game except for keepalive messages, and sending keepalive messages 
in response to each message so Fallout 4 doesn't disconnect the client.
4.) Sending a message to Fallout 4 once every 5 seconds to toggle the Diamond City Radio Station

The UDP Discover Broadcast and Discover Response are JSON payloads.  After establishing a TCP connection, Fallout 4 sends 
messages (header seems to be 5 bytes...4 byte length with least significant byte first, and the fifth byte is a message type),
immediately.  Message type 1 is a simple JSON payload indicating language and version, message type 0 is an empty payload keepalive
sent quite frequently, and the other message types seem like generic contains for game data which I have no decoded yet.

Upon connecting Fallout 4 sends a large ~500KB payload with a bunch of information on the game state with periodic updates after that.

The Pip-Boy Client has a pretty simple RPC like interface for sending commands...it is just a JSON payload with the previously
mentioned header (4 byte length + message type 5).  The JSON payload has an incrementing ID field, variable length args field, 
and most importantly a type field that indicates the type of command.  The challenging part is that most of the args
seem like indexed values into the large data set send down upon connecting...so without decoding that it will be difficult
to do many useful commands.

As an example, ths following json has type 12 (Radio Toggle Command), taking a single argument representing the ID of the radio station to toggle.

    {"type":12,"args":[28751],"id":1}
  
At least on my PS4, 28751 corresponded to the Diamond City Radio Station.  The official Pip-Boy app increments the id field for each,
command, but for now this client just uses 1 for each command which seems to be accepted by Fallout 4.

A rough example of the above can be seen in the Session class:

    sendFalloutControl(new RadioToggle(RadioStation.DiamondCityRadio), os);

The Driver class shows how one might use the discovery functionality to find the first available Fallout 4 app and then establish a session to it with the session class to periodically execute some behavior.  It's all very much a prototype to show what is possible at the barebones level.

	public static void main(String[] args) throws IOException {
		System.out.println("Discovering hosts...");
		List<DiscoverResponse> responses = Discovery.discover();
		System.out.println("Discovery complete");
		System.out.println();
		
		for(DiscoverResponse response : responses) {
			if(response.isBusy()) {
				System.out.println("Skipping busy host: " + response);
			} else {
				System.out.println("Connecting to available host: " + response);
				Session.connect(response.getMachineAddress());
			}
		}
	}
