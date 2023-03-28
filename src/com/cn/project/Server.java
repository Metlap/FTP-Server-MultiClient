package com.cn.project;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
	private static int serverPort = 8000; // The server will be listening on this port number

	private static String FILE_PATH = "./resources/";

	ServerSocket serverSocket; // serversocket used to lisen on port number 8000

	public static void main(String args[]) throws Exception {

		System.out.println("The server is running.");
		// create a serversocket
		ServerSocket serverSocket = new ServerSocket(serverPort);
		// Wait for connection
		System.out.println("Waiting for connection");
		int clientNum = 1; // count of the clientNumber to create one new thread for each client
		try {
			while (true) {
				//accept a connection from a client
				new Handler(serverSocket.accept(), clientNum).start();
				System.out.println("Client " + clientNum + " is connected!");
				System.out.println("Connection received from " + serverSocket.getInetAddress().getHostName());
				clientNum++;
			}
		} finally {
			serverSocket.close();
		}

	}

	public Server() {
	}

	/**
	 * A handler thread class. Handlers are spawned from the listening loop and are
	 * responsible for dealing with a single client's requests.
	 */
	private static class Handler extends Thread {

		private String message; // message received from the client
		private String MESSAGE; // uppercase message send to the client
		private ObjectOutputStream out; // stream write to the socket
		private ObjectInputStream in; // stream read from the socket
		private DataInputStream dis;
		private DataOutputStream dos;
		private int no;

		private Socket connection = null; // socket for the connection with the client

		public Handler(Socket connection, int no) {
			this.connection = connection;
			this.no = no;
		}

		public void run() {
			try {
				
				System.out.println("Connection received from " + connection.getInetAddress().getHostName());
				// initialize Input and Output streams
				out = new ObjectOutputStream(connection.getOutputStream());
				out.flush();
				in = new ObjectInputStream(connection.getInputStream());

				while (true) {

//				connection.setSoTimeout(900000);
					message = (String) in.readObject();

					String[] splitStr = message.trim().split("\\s+");

					if (splitStr.length == 2 && (splitStr[0]).equals("upload") && splitStr[1].length() > 0) {

						// Appending new to requested file name
						receiveFile("new" + splitStr[1]);
					}

					else if (splitStr.length == 2 && (splitStr[0]).equals("get") && splitStr[1].length() > 0) {

						sendFile(splitStr[1]);
					}

					else if (splitStr.length == 2 && (splitStr[0]).equals("exit")
							&& (splitStr[1]).equals("ftpclient")) {

						// Exiting and closing by quitting the while loop
						break;

					} else {

						System.out.println("Please enter a valid command");
						sendMessage("wrong command");

					}

				}

			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				System.err.println("Data received in unknown format");
				e.printStackTrace();
			} catch (IOException ioException) {
				//ioException.printStackTrace();
				System.err.println("Client number " + this.no + " is closed. Closing the socket" );
			} finally {
				// Close connections
				try {
					in.close();
					out.close();
					if (dos != null)
						dos.close();
					if (dis != null)
						dis.close();
					connection.close();
				} catch (IOException ioException) {
					ioException.printStackTrace();
				}
			}
		}

		// send a message to the output stream
		void sendMessage(String msg) {
			try {
				out.writeObject(msg);
				out.flush();
				System.out.println("Sent message: " + msg);
			} catch (IOException ioException) {
				ioException.printStackTrace();
			}
		}

		void receiveFile(String fileName) {

			try {

				String filePath = FILE_PATH + fileName;
				File file = new File(filePath);
				dis = new DataInputStream(connection.getInputStream());

				long size = dis.readLong();
				if (size == 0L) {
					System.out.println("File not found at the given directory");
					return;
				}

				FileOutputStream fileOutputStream = new FileOutputStream(file);

				int bytesRead = 0;
				byte[] buffer = new byte[1024];

				while (size > 0 && (bytesRead = dis.read(buffer, 0, (int) Math.min(buffer.length, size))) != -1) {
					fileOutputStream.write(buffer, 0, bytesRead);
					size -= bytesRead; // read upto file size
					fileOutputStream.flush();
				}
//				fileOutputStream.close();

				System.out.println("======File Receive from client " + this.no + " success======");

			}

			catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				System.out.println("File not found");
				e.printStackTrace();
			} catch (IOException e) {
				System.out.println("Exception other than File not found");
				e.printStackTrace();
			}

		}

		void sendFile(String fileName) {

			String filePath = FILE_PATH + fileName;

			System.out.println("Sending file to the client " + this.no);

			// Make file chunks of size 1kb and send to the server
			int totalBytesTransferred = 0;

			try {
				File file = new File(filePath);
				dos = new DataOutputStream(connection.getOutputStream());
				if (file.exists()) {
					FileInputStream fileInputStream = new FileInputStream(file);

					// Getting the size of the file and sending it to server
					dos.writeLong(file.length());

					System.out.println("Length is ::" + file.length());
					dos.flush();

					// Read data into buffer array
					byte[] buffer = new byte[1024];
					// -1 -> EOF
					while ((totalBytesTransferred = fileInputStream.read(buffer)) != -1) {

						// Writing sub arrays to include case of last chunk where size could be less
						// than 1kb
						dos.write(buffer, 0, totalBytesTransferred);
						dos.flush();
					}
//				fileInputStream.close();
					System.out.println("======File Send to client " + this.no + " success======");

				} else {
					dos.writeLong(0L);
					dos.flush();
//				dos.close();
					System.out.println("File not found at the given directory");
					return;
				}
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.err.println("File not found in the specified path");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.out.println("Other than File not found");
			}

		}

	}

}
