package network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Oyuncunun sunucuyla haberleşmesini sağlayan ağ istemcisi.
 * Hareket komutlarını sunucuya yollar ve dünyadaki diğer oyuncuların pozisyonlarını alır.
 */
public class GameClient extends Thread {

	private InetAddress serverAddress;
	private int serverPort;
	private DatagramSocket socket;
	private boolean isRunning;

	/**
	 * Yeni bir istemci oluşturur ve sunucuya bağlanmaya hazırlanır.
	 * 
	 * @param ipAddress Sunucunun IP adresi (Local ağ için "localhost")
	 * @param port Sunucunun açık olan portu
	 */
	public GameClient(String ipAddress, int port) {
		try {
			this.socket = new DatagramSocket(); // Dinamik rastgele port alır
			this.serverAddress = InetAddress.getByName(ipAddress);
			this.serverPort = port;
			this.isRunning = true;
			System.out.println("Istemci hazir, sunucuya baglaniliyor: " + ipAddress + ":" + port);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		while (isRunning) {
			byte[] data = new byte[1024];
			DatagramPacket packet = new DatagramPacket(data, data.length);
			try {
				// Sunucudan gelen oyun verisini dinle
				socket.receive(packet);
				
				// Gelen byte dizisini Packet objesine çevirip sahneyi (Scene) güncelle
				
			} catch (IOException e) {
				if (isRunning) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Kendi hareketimizi veya bir eylemi sunucuya bildirir.
	 * 
	 * @param data Ağ paketi byte dizisi
	 */
	public void sendData(byte[] data) {
		DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress, serverPort);
		try {
			socket.send(packet);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void disconnect() {
		isRunning = false;
		if (socket != null) {
			socket.close();
		}
		System.out.println("Sunucudan ayrilindi.");
	}
}
