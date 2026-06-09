package network;

import java.nio.ByteBuffer;

import com.codedisaster.steamworks.SteamException;
import com.codedisaster.steamworks.SteamID;
import com.codedisaster.steamworks.SteamNetworking;
import com.codedisaster.steamworks.SteamNetworkingCallback;

/**
 * IP ve Port açma zahmetine girmeden oyuncuların doğrudan Steam arkadaşları
 * üzerinden birbirine bağlanmasını sağlayan "Peer-to-Peer" yöneticisi.
 */
public class SteamP2PManager implements SteamNetworkingCallback {

	private SteamNetworking networking;

	/**
	 * Steam P2P ağını başlatır.
	 */
	public void init() {
		// SteamWorks kütüphanesindeki networking modülünü bu callback sınıfına bağlarız
		networking = new SteamNetworking(this);
		System.out.println("Steam P2P Networking baslatildi.");
	}

	/**
	 * Diğer Steam oyuncusuna (Arkadaş vs.) P2P üzerinden mesaj veya lokasyon atar.
	 * 
	 * @param targetSteamId Mesajın gideceği oyuncunun Steam Profil ID'si
	 * @param data Gönderilecek ham veri paketi
	 */
	public void sendPacket(SteamID targetSteamId, byte[] data) {
		if (networking != null) {
			try {
				// ByteBuffer'ın Direct olması önemlidir (Native C++ kodları için)
				ByteBuffer buffer = ByteBuffer.allocateDirect(data.length);
				buffer.put(data);
				buffer.flip();

				// EP2PSendType.Unreliable (UDP mantığı - kaybolabilir ama hızlı)
				networking.sendP2PPacket(targetSteamId, buffer, com.codedisaster.steamworks.SteamNetworking.P2PSend.Unreliable, 0);
			} catch (SteamException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Gelen paketleri oyun döngüsünde (Game Loop) kontrol eder.
	 */
	public void update() {
		if (networking != null) {
			int[] msgSize = new int[1];
			// Kaç byte veri okunmayı bekliyor?
			if (networking.isP2PPacketAvailable(0, msgSize)) {
				int packetSize = msgSize[0];
				if (packetSize > 0) {
					try {
						ByteBuffer buffer = ByteBuffer.allocateDirect(packetSize);
						SteamID senderID = new SteamID();
						int bytesRead = networking.readP2PPacket(senderID, buffer, 0);
						if (bytesRead > 0) {
							byte[] data = new byte[bytesRead];
							buffer.get(data);
							// Veriyi okuyup oyundaki objeleri güncelle
						}
					} catch (SteamException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	// --- SteamNetworkingCallback Interface Metotları ---

	@Override
	public void onP2PSessionRequest(SteamID steamIDRemote) {
		// Biri bize bağlanmak istiyor. Otomatik kabul ediyoruz.
		System.out.println(steamIDRemote.getAccountID() + " ID'li oyuncu P2P baglantisi istiyor. Kabul edildi.");
		networking.acceptP2PSessionWithUser(steamIDRemote);
	}

	@Override
	public void onP2PSessionConnectFail(SteamID steamIDRemote, com.codedisaster.steamworks.SteamNetworking.P2PSessionError sessionError) {
		System.out.println("Steam P2P Baglantisi Koptu/Basarisiz: " + sessionError.name());
	}
}
