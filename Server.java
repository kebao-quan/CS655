import java.net.*;
import java.io.*;


public class Server{
	public static void main(String [] args) throws IOException{
		if (args.length != 1) {
			System.err.println("Usage: java EchoServer <port number>");
			System.exit(1);
		}

		int portNumber = Integer.parseInt(args[0]);

		try (ServerSocket serverSocket = new ServerSocket(portNumber)) {
			while (true) {
				Socket clientSocket = serverSocket.accept();

				//Accept new client request. Serve request with new thread.
				new ClientHandler(clientSocket).start();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}


class ClientHandler extends Thread {
	private Socket clientSocket;

	public ClientHandler(Socket socket) {
		this.clientSocket = socket;
	}

	public void run() {
		try (
				PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
				BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))
		) {
			String inputLine;
			while ((inputLine = in.readLine()) != null) {
				String[] tokens = inputLine.split("\\s+");

				//s: <PROTOCOL PHASE><WS><MEASUREMENT TYPE><WS><NUMBER OF PROBES><WS><MESSAGE SIZE><WS><SERVER DELAY>\n
				//m: <PROTOCOL PHASE><WS><PROBE SEQUENCE NUMBER><WS><PAYLOAD>\n
				//t: <PROTOCOL PHASE>\n
				String protocolPhase = tokens[0];
				String measurementType = tokens[1];
				int numOfProbes = Integer.parseInt(tokens[2]);
				int messageSize = Integer.parseInt(tokens[3]);
				int serverDelay = Integer.parseInt(tokens[4]);

				System.out.println("PROTOCOL PHASE: " + protocolPhase);
				System.out.println("MEASUREMENT TYPE: " + measurementType);
				System.out.println("NUMBER OF PROBES: " + numOfProbes);
				System.out.println("MESSAGE SIZE: " + messageSize);
				System.out.println("SERVER DELAY: " + serverDelay);


				if (
						"s".equalsIgnoreCase(protocolPhase)
								&& ("rtt".equalsIgnoreCase(measurementType) || "tput".equalsIgnoreCase(measurementType))
								&& (numOfProbes > 0)
								&& (messageSize > 0)
								&& (serverDelay >= 0)
				) {
					out.println("200 OK: Ready");
					for (int i = 0; i < numOfProbes; i++) {
						String probeInput = in.readLine();
						String[] probeMessage = probeInput.split("\\s+");
						String pPhase = probeMessage[0];
						int suqNum = Integer.parseInt(probeMessage[1]);
						String payload = probeMessage[2];

						if (!pPhase.equalsIgnoreCase("m") || suqNum != i + 1 || payload.length() != messageSize) {
							out.println(probeInput);
							clientSocket.close();
							return;
						}

						if (serverDelay != 0) {
							try {
								Thread.sleep(serverDelay);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
						out.println(probeInput);


					}
				} else {
					out.println("404 ERROR: Invalid Connection Setup Message");
					clientSocket.close();
					return;
				}

			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

