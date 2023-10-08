import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {
	public static void main(String[] args) throws IOException {
		String user_enter;
		System.out.println("Please enter the hostname>");
		Scanner sc = new Scanner(System.in);
		String hostname = sc.next();

		System.out.println("Please enter the port number of Server>");
		user_enter = sc.next();
		int portnumber = Integer.parseInt(user_enter);
		System.out.println("your hostname is: " + hostname);
		System.out.println("your portnumber is:" + portnumber);

		try (
				Socket serverSocket = new Socket(hostname, portnumber);
				PrintWriter out = new PrintWriter(serverSocket.getOutputStream(), true);
				BufferedReader in = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));
		) {
			//<PROTOCOL PHASE><WS><MEASUREMENT TYPE><WS><NUMBER OF PROBES><WS><MESSAGE SIZE><WS><SERVER DELAY>\n
			String userInput;
			String protocolPhase;
			String measurementType;
			int numOfProbes;
			int messageSize;
			int serverDelay;

			System.out.println("Please enter the Protocol Phase(i.e. 's')");
			protocolPhase = sc.next();

			System.out.println("Please enter the Measurement Type (i.e. 'rtt' or 'tput')");
			measurementType = sc.next();

			System.out.println("Please enter the Number of Probes");
			numOfProbes = Integer.parseInt(sc.next());

			System.out.println("Please enter the Message Size(Bytes)");
			messageSize = Integer.parseInt(sc.next());

			System.out.println("Please enter the Server Delay(Milliseconds)");
			serverDelay = Integer.parseInt(sc.next());

			userInput = protocolPhase + "\t" + measurementType + "\t" + numOfProbes + "\t" +messageSize + "\t" +serverDelay;
			System.out.println("sending request: " + userInput);
			out.println(userInput);

			String response = in.readLine();
			if (!response.equalsIgnoreCase("200 OK: Ready")) {
				System.out.println(response);
				return;
			}

			System.out.println("200 OK: Ready");

			//m: <PROTOCOL PHASE><WS><PROBE SEQUENCE NUMBER><WS><PAYLOAD>\n
			long totalBytesSent = 0;
			long totalTransmissionTime = 0;
			for (int i = 0; i < numOfProbes; i++) {
				String payload = "";
				for(int j = 0; j < messageSize; j++) {
					payload = payload + "x";
				}
				String probe = "m " + (i + 1) + " " + payload;
				long sendTime = System.currentTimeMillis();
				out.println(probe);
				response = in.readLine();
				if (response == null) {
					System.err.println("Lost connection to server.");
					return;
				}
				if (response.equalsIgnoreCase(probe)) {
					System.out.println("probe " + (i+1));
					long receiveTime = System.currentTimeMillis();
					long rtt = receiveTime - sendTime;
					totalTransmissionTime += rtt;
					totalBytesSent += messageSize;
				}
				else{
					return;
				}
			}

			if ("rtt".equalsIgnoreCase(measurementType)) {
				double mean_rtt = (double) totalTransmissionTime / numOfProbes;
				System.out.println("Mean RTT: " + mean_rtt + " ms");
			} else if ("tput".equalsIgnoreCase(measurementType)) {
				double tput = (double) totalBytesSent / totalTransmissionTime  * 8;
				System.out.println("Mean Throughput: " + tput + "kbps");
			}

			//t: <PROTOCOL PHASE>\n
			System.out.println("sending request: t");
			out.println("t");
			response = in.readLine();
			if (response != "200 OK: Closing Connection") {
				System.out.println(response);
				return;
			}
			return;


		} catch (UnknownHostException e) {
			System.err.println("Don't know about host " + hostname);
			System.exit(1);
		} catch (IOException e) {
			System.err.println("Couldn't get I/O for the connection to " + hostname);
			System.exit(1);
		}
	}
}

