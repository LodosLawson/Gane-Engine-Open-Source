package network;

import java.io.Serializable;

/**
 * Ağ üzerinden (Server - Client arası) gönderilecek tüm verilerin temel (Base) paket sınıfı.
 * Java Serialization mekanizması (veya Kryo gibi kütüphaneler) ile byte dizilerine çevrilerek gönderilir.
 */
public abstract class Packet implements Serializable {
	
	private static final long serialVersionUID = 1L;

	// Paketin türünü belirten numara
	protected byte packetId;

	public Packet(byte packetId) {
		this.packetId = packetId;
	}

	public byte getPacketId() {
		return packetId;
	}

	// İhtiyaca göre eklenebilecek alt sınıflar:
	
	/**
	 * Sunucuya giriş yapmak için kullanılan örnek paket.
	 */
	public static class PacketConnect extends Packet {
		private static final long serialVersionUID = 1L;
		public String username;

		public PacketConnect(String username) {
			super((byte) 0x01);
			this.username = username;
		}
	}

	/**
	 * Oyuncunun hareketini (x, y, z) diğer oyunculara bildiren paket.
	 */
	public static class PacketMove extends Packet {
		private static final long serialVersionUID = 1L;
		public float x, y, z;
		public String playerId;

		public PacketMove(String playerId, float x, float y, float z) {
			super((byte) 0x02);
			this.playerId = playerId;
			this.x = x;
			this.y = y;
			this.z = z;
		}
	}
	
	/**
	 * Oyuncunun sunucudan ayrıldığını bildiren paket.
	 */
	public static class PacketDisconnect extends Packet {
		private static final long serialVersionUID = 1L;
		public String playerId;

		public PacketDisconnect(String playerId) {
			super((byte) 0x03);
			this.playerId = playerId;
		}
	}
}
