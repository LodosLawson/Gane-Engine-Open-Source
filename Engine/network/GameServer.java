package network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Bağımsız (Dedicated) veya Oyuncu-Kurucu (Listen) modunda çalışan sunucu yapısı.
 * UDP üzerinden oyuncu konumlarını alır ve diğer tüm oyunculara dağıtır.
 */
public class GameServer extends Thread {

	private DatagramSocket socket;
	private boolean isRunning;
	private int port = 1337;

	public GameServer() {
		try {
			// Sunucu soketini belirtilen portta aç
			this.socket = new DatagramSocket(this.port);
			this.isRunning = true;
			System.out.println("Oyun sunucusu " + port + " portunda baslatildi.");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		// Sunucu dinleme döngüsü
		while (isRunning) {
			byte[] data = new byte[1024];
			DatagramPacket packet = new DatagramPacket(data, data.length);
			try {
				// İstemcilerden gelecek veriyi bekle (Bloke edici - blocking)
				socket.receive(packet);
				
				// Gelen veriyi işle ve diğer tüm oyunculara dağıt (Broadcast)
				// Örn: PacketMove ise parse edip oyuncu koordinatlarını güncelle
				
			} catch (IOException e) {
				if (isRunning) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Bir oyuncuya veri paketi gönderir (Byte dizisi olarak).
	 */
	public void sendData(byte[] data, InetAddress ipAddress, int port) {
		DatagramPacket packet = new DatagramPacket(data, data.length, ipAddress, port);
		try {
			socket.send(packet);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void shutdown() {
		isRunning = false;
		if(socket != null) {
			socket.close();
		}
		System.out.println("Oyun sunucusu kapatildi.");
	}
}
