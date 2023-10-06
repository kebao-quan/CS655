import java.io.*;
import java.net.*;

public class Client {
	public static void main(String[] args) throws IOException {
		if (args.length != 2) {
			System.err.println("Usage: java EchoClient <host name> <port number>");
			System.exit(1);
		}

		String hostname = args[0];
		int port = Integer.parseInt(args[1]);


		try (
				Socket serverSocket = new Socket(hostname, port);
				PrintWriter out = new PrintWriter(serverSocket.getOutputStream(), true);
				BufferedReader in = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));
				BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
		) {
			//<PROTOCOL PHASE><WS><MEASUREMENT TYPE><WS><NUMBER OF PROBES><WS><MESSAGE SIZE><WS><SERVER DELAY>\n
			String userInput;
			while ((userInput = stdIn.readLine()) != null && !userInput.equalsIgnoreCase("exit")) {
				String[] tokens = userInput.split("\\s+");
				if (tokens.length != 5) {
					System.out.println("Invalid argument: <PROTOCOL PHASE><WS><MEASUREMENT TYPE><WS><NUMBER OF PROBES><WS><MESSAGE SIZE><WS><SERVER DELAY>\\n");
					return;
				}
				String protocolPhase = tokens[0];
				String measurementType = tokens[1];
				int numOfProbes = Integer.parseInt(tokens[2]);
				int messageSize = Integer.parseInt(tokens[3]);
				int serverDelay = Integer.parseInt(tokens[4]);
				out.println(userInput);

				String response = in.readLine();
				if (response.equalsIgnoreCase("200 OK: Ready")) {
					System.out.println("200 OK: Ready");

					//m: <PROTOCOL PHASE><WS><PROBE SEQUENCE NUMBER><WS><PAYLOAD>\n
					long totalBytesSent = 0;
					long totalTransmissionTime = 0;
					for (int i = 0; i < numOfProbes; i++) {
						String payload = "x".repeat(messageSize);
						String probe = "m " + (i + 1) + " " + payload;
						long sendTime = System.currentTimeMillis();
						out.println(probe);
						response = in.readLine();
						if (response == null) {
							System.err.println("Lost connection to server.");
							return;
						}
						if (!response.equalsIgnoreCase(probe)) return;
						long receiveTime = System.currentTimeMillis();
						long rtt = receiveTime - sendTime;
						totalTransmissionTime += rtt;
						totalBytesSent += messageSize;
					}


					if ("rtt".equalsIgnoreCase(measurementType)) {
						System.out.println("Mean RTT: " + totalTransmissionTime / numOfProbes);
					} else if ("tput".equalsIgnoreCase(measurementType)) {
						double tput = totalBytesSent / totalTransmissionTime * 1000 * 8;
						System.out.println("Mean Throughput: " + tput + " bits per second");
					}




				} else if (response.equalsIgnoreCase("404 ERROR: Invalid Connection Setup Message")) {
					System.out.println("404 ERROR: Invalid Connection Setup Message");
				} else {
					System.out.println("Err");
				}
			}
		} catch (UnknownHostException e) {
			System.err.println("Don't know about host " + hostname);
			System.exit(1);
		} catch (IOException e) {
			System.err.println("Couldn't get I/O for the connection to " + hostname);
			System.exit(1);
		}










	}

}

