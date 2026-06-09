package objConverter;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

import utils.MyFile;

/**
 * .obj uzantılı 3B model dosyalarını okuyarak OpenGL'in anlayacağı indeksli ve 
 * sıralı dizi formatlarına (ModelData) çeviren yardımcı ayrıştırıcı sınıf.
 */
public class OBJFileLoader {

	/**
	 * OBJ dosyasını satır satır okur ve ModelData objesine çevirir.
	 * 
	 * @param objFile Okunacak .obj dosyasının yörüngesi
	 * @return Düzenlenmiş model verisi
	 */
	public static ModelData loadOBJ(MyFile objFile) {
		BufferedReader reader = null;
		try {
			reader = objFile.getReader();
		} catch (Exception e1) {
			e1.printStackTrace();
			System.err.println("Couldn't find model file: " + objFile);
			System.exit(-1);
		}
		String line;
		
		// OBJ dosyasından geçici olarak ayrıştırılacak veri listeleri
		List<Vertex> vertices = new ArrayList<Vertex>();
		List<Vector2f> textures = new ArrayList<Vector2f>();
		List<Vector3f> normals = new ArrayList<Vector3f>();
		List<Integer> indices = new ArrayList<Integer>();
		try {
			// 1. AŞAMA: Dosyayı satır satır oku; Vertex, Texture ve Normal değerlerini parse et
			while (true) {
				line = reader.readLine();
				if (line.startsWith("v ")) {
					// v -> Geometrik Köşe Noktası (Vertex)
					String[] currentLine = line.split(" ");
					Vector3f vertex = new Vector3f((float) Float.valueOf(currentLine[1]),
							(float) Float.valueOf(currentLine[2]), (float) Float.valueOf(currentLine[3]));
					Vertex newVertex = new Vertex(vertices.size(), vertex);
					vertices.add(newVertex);

				} else if (line.startsWith("vt ")) {
					// vt -> Doku Koordinatı (Texture UV)
					String[] currentLine = line.split(" ");
					Vector2f texture = new Vector2f((float) Float.valueOf(currentLine[1]),
							(float) Float.valueOf(currentLine[2]));
					textures.add(texture);
				} else if (line.startsWith("vn ")) {
					// vn -> Yüzey Normali (Vertex Normal)
					String[] currentLine = line.split(" ");
					Vector3f normal = new Vector3f((float) Float.valueOf(currentLine[1]),
							(float) Float.valueOf(currentLine[2]), (float) Float.valueOf(currentLine[3]));
					normals.add(normal);
				} else if (line.startsWith("f ")) {
					// f -> Yüzey / Üçgen (Face) verisine geldik, ilk aşama bitti
					break;
				}
			}
			
			// 2. AŞAMA: Üçgenlerin hangi köşelerden (v/vt/vn) oluştuğunu (indeksleri) parse et
			while (line != null && line.startsWith("f ")) {
				String[] currentLine = line.split(" ");
				// "vertex_index/texture_index/normal_index" formatında gelir
				String[] vertex1 = currentLine[1].split("/");
				String[] vertex2 = currentLine[2].split("/");
				String[] vertex3 = currentLine[3].split("/");
				
				// Her bir üçgen noktasını işleme al
				processVertex(vertex1, vertices, indices);
				processVertex(vertex2, vertices, indices);
				processVertex(vertex3, vertices, indices);
				line = reader.readLine();
			}
			reader.close();
		} catch (IOException e) {
			System.err.println("Couldn't read model file: " + objFile);
			System.exit(-1);
		}
		
		// 3. AŞAMA: Kullanılmayan noktaları sil/düzenle ve dizilere dönüştür
		removeUnusedVertices(vertices);
		float[] verticesArray = new float[vertices.size() * 3];
		float[] texturesArray = new float[vertices.size() * 2];
		float[] normalsArray = new float[vertices.size() * 3];
		
		float furthest = convertDataToArrays(vertices, textures, normals, verticesArray, texturesArray, normalsArray);
		int[] indicesArray = convertIndicesListToArray(indices);
		
		return new ModelData(verticesArray, texturesArray, normalsArray, indicesArray, furthest);
	}

	/**
	 * Gelen yüzey satırındaki tek bir köşeyi işler ve OpenGL standardında 
	 * ilgili listelere yerleştirir.
	 * 
	 * @param vertex OBJ verisindeki parçalanmış indeksler dizisi [v, vt, vn]
	 * @param vertices Yüklenmiş tüm köşe noktaları listesi
	 * @param indices Yüzeyleri oluşturacak OpenGL uyumlu indeks listesi
	 * @return İşlemi tamamlanan Vertex objesi
	 */
	private static Vertex processVertex(String[] vertex, List<Vertex> vertices, List<Integer> indices) {
		// OBJ dosyalarında indeks 1'den başlar, Java dizilerinde 0'dan. Bu yüzden 1 çıkartılır
		int index = Integer.parseInt(vertex[0]) - 1;
		Vertex currentVertex = vertices.get(index);
		int textureIndex = Integer.parseInt(vertex[1]) - 1;
		int normalIndex = Integer.parseInt(vertex[2]) - 1;
		
		// Eğer bu köşenin doku/normal değerleri henüz atanmadıysa, hemen ata
		if (!currentVertex.isSet()) {
			currentVertex.setTextureIndex(textureIndex);
			currentVertex.setNormalIndex(normalIndex);
			indices.add(index);
			return currentVertex;
		} else {
			// Eğer önceden atanmışsa (aynı koordinatta fakat farklı UV/Normal isteniyorsa),
			// çakışmayı çözüp kopyalayarak yeni bir nokta oluştur.
			return dealWithAlreadyProcessedVertex(currentVertex, textureIndex, normalIndex, indices, vertices);
		}
	}

	/**
	 * Liste türündeki indeks dizisini donanıma (OpenGL'e) gönderilecek ilkel int[] dizisine çevirir.
	 */
	private static int[] convertIndicesListToArray(List<Integer> indices) {
		int[] indicesArray = new int[indices.size()];
		for (int i = 0; i < indicesArray.length; i++) {
			indicesArray[i] = indices.get(i);
		}
		return indicesArray;
	}

	/**
	 * Karmaşık veri yapılarını (List), VAO'ya yüklenecek düz float[] dizilerine yerleştirir.
	 * Ayrıca render esnasında culling (görüş alanı kırpma) yapmak için merkezden en uzak noktayı da hesaplar.
	 * 
	 * @param vertices Düzenlenmiş vertex listesi
	 * @param textures Yüklenmiş tüm doku koordinatları listesi
	 * @param normals Yüklenmiş tüm normal vektörleri listesi
	 * @param verticesArray Çıktı olarak üretilecek vertex dizisi
	 * @param texturesArray Çıktı olarak üretilecek UV dizisi
	 * @param normalsArray Çıktı olarak üretilecek normal dizisi
	 * @return Merkezden en uzak noktanın (uzunluğun) değeri
	 */
	private static float convertDataToArrays(List<Vertex> vertices, List<Vector2f> textures, List<Vector3f> normals,
			float[] verticesArray, float[] texturesArray, float[] normalsArray) {
		float furthestPoint = 0;
		for (int i = 0; i < vertices.size(); i++) {
			Vertex currentVertex = vertices.get(i);
			
			// En uzak nokta tespiti
			if (currentVertex.getLength() > furthestPoint) {
				furthestPoint = currentVertex.getLength();
			}
			
			Vector3f position = currentVertex.getPosition();
			Vector2f textureCoord = textures.get(currentVertex.getTextureIndex());
			Vector3f normalVector = normals.get(currentVertex.getNormalIndex());
			
			// Vertex koordinatlarını array'e yerleştir (x, y, z)
			verticesArray[i * 3] = position.x;
			verticesArray[i * 3 + 1] = position.y;
			verticesArray[i * 3 + 2] = position.z;
			
			// Doku koordinatlarını array'e yerleştir (OpenGL'de V ekseni ters çevrilir)
			texturesArray[i * 2] = textureCoord.x;
			texturesArray[i * 2 + 1] = 1 - textureCoord.y;
			
			// Normalleri array'e yerleştir (x, y, z)
			normalsArray[i * 3] = normalVector.x;
			normalsArray[i * 3 + 1] = normalVector.y;
			normalsArray[i * 3 + 2] = normalVector.z;

		}
		return furthestPoint;
	}

	/**
	 * Daha önce doku/normal atanmış olan bir köşenin üzerine farklı bir doku/normal
	 * ataması yapılmaya çalışılıyorsa, bu köşenin kopyasını çıkarıp vertex listesinin 
	 * sonuna ekleyerek indeks çakışmasını engeller.
	 * OpenGL her noktanın (pozisyon) sadece 1 doku ve normal haritasına sahip olmasına izin verir.
	 * 
	 * @param previousVertex İşlenmeye çalışılan asıl Vertex objesi
	 * @param newTextureIndex Yeni gelen doku indeksi
	 * @param newNormalIndex Yeni gelen normal indeksi
	 * @param indices O anki üçgen dizilimi indeksleri
	 * @param vertices Tüm objelerin vertex havuzu
	 * @return Kullanılacak olan doğru (ya aynı ya da kopya) Vertex objesi
	 */
	private static Vertex dealWithAlreadyProcessedVertex(Vertex previousVertex, int newTextureIndex, int newNormalIndex,
			List<Integer> indices, List<Vertex> vertices) {
		
		// Eğer eski değerlerle yeni değerler aynıysa ekstra işlem yapmaya gerek yok
		if (previousVertex.hasSameTextureAndNormal(newTextureIndex, newNormalIndex)) {
			indices.add(previousVertex.getIndex());
			return previousVertex;
		} else {
			// Değerler farklıysa ve zaten bir kopya üretilmişse kopyayı kullanmayı dene
			Vertex anotherVertex = previousVertex.getDuplicateVertex();
			if (anotherVertex != null) {
				return dealWithAlreadyProcessedVertex(anotherVertex, newTextureIndex, newNormalIndex, indices,
						vertices);
			} else {
				// Daha önceden kopya üretilmemişse; yeni, eşsiz bir kopya vertex oluştur
				Vertex duplicateVertex = new Vertex(vertices.size(), previousVertex.getPosition());
				duplicateVertex.setTextureIndex(newTextureIndex);
				duplicateVertex.setNormalIndex(newNormalIndex);
				previousVertex.setDuplicateVertex(duplicateVertex);
				vertices.add(duplicateVertex);
				indices.add(duplicateVertex.getIndex());
				return duplicateVertex;
			}

		}
	}

	/**
	 * Model dosyasında bulunup da hiçbir üçgen(yüzey) tarafından kullanılmayan ölü vertexleri temizler / sıfırlar.
	 * 
	 * @param vertices İşlenecek vertex listesi
	 */
	private static void removeUnusedVertices(List<Vertex> vertices) {
		for (Vertex vertex : vertices) {
			vertex.averageTangents();
			if (!vertex.isSet()) {
				vertex.setTextureIndex(0);
				vertex.setNormalIndex(0);
			}
		}
	}

}