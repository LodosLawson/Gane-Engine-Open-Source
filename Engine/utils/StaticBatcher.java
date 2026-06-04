package utils;

import java.util.List;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import objConverter.ModelData;
import openglObjects.Vao;
import scene.Entity;
import scene.Model;
import scene.Skin;

/**
 * Birden fazla durağan objeyi tek bir devasa modele (Mesh) dönüştüren
 * Statik Batching yardımcı sınıfı.
 */
public class StaticBatcher {

	/**
	 * Verilen statik varlık listesini tek bir Entity altında birleştirir.
	 * 
	 * DİKKAT: Verilen tüm Entity'lerin aynı kaplamayı (Skin) kullandığı ve
	 * orjinal ModelData verisine (CPU belleğinde) sahip olduğu varsayılır.
	 * 
	 * @param entities Birleştirilecek varlıkların listesi
	 * @param skin     Oluşacak devasa yeni varlığa atanacak kaplama
	 * @return Tüm modellerin birleşimi olan tek bir büyük Entity
	 */
	public static Entity batch(List<Entity> entities, Skin skin) {
		if (entities == null || entities.isEmpty()) {
			return null;
		}

		int totalVertices = 0;
		int totalIndices = 0;

		// 1. Önce toplam boyutu hesapla
		for (Entity e : entities) {
			ModelData md = e.getModel().getModelData();
			if (md == null) {
				System.err.println("StaticBatcher HATASI: ModelData CPU belleginde bulunamadi! Model ID: " 
						+ e.getModel().getVao().id);
				return null;
			}
			totalVertices += md.getVertexCount();
			totalIndices += md.getIndices().length;
		}

		// 2. Dev dizileri (Array) oluştur
		float[] outVertices = new float[totalVertices * 3];
		float[] outTexCoords = new float[totalVertices * 2];
		float[] outNormals = new float[totalVertices * 3];
		int[] outIndices = new int[totalIndices];

		int vOffset = 0; // vertices array ofseti (float)
		int tOffset = 0; // texCoords array ofseti (float)
		int nOffset = 0; // normals array ofseti (float)
		int iOffset = 0; // indices array ofseti (int)
		
		int vertexIndexOffset = 0; // İndekslerin vertex sayısına göre kayması

		// 3. Her bir varlığın verilerini matrisi ile transform edip dev dizilere ekle
		Vector4f tempVertex = new Vector4f();
		Vector4f tempNormal = new Vector4f();

		for (Entity e : entities) {
			ModelData md = e.getModel().getModelData();
			
			Matrix4f transformationMatrix = new Matrix4f();
			transformationMatrix.setIdentity();
			Matrix4f.translate(e.getPosition(), transformationMatrix, transformationMatrix);
			if (e.getRotation().y != 0) {
				Matrix4f.rotate((float) Math.toRadians(e.getRotation().y), new Vector3f(0, 1, 0), transformationMatrix, transformationMatrix);
			}
			if (e.getRotation().x != 0) {
				Matrix4f.rotate((float) Math.toRadians(e.getRotation().x), new Vector3f(1, 0, 0), transformationMatrix, transformationMatrix);
			}
			if (e.getRotation().z != 0) {
				Matrix4f.rotate((float) Math.toRadians(e.getRotation().z), new Vector3f(0, 0, 1), transformationMatrix, transformationMatrix);
			}
			
			if (e.getModelOffsetRot().x != 0) Matrix4f.rotate((float) Math.toRadians(e.getModelOffsetRot().x), new Vector3f(1, 0, 0), transformationMatrix, transformationMatrix);
			if (e.getModelOffsetRot().y != 0) Matrix4f.rotate((float) Math.toRadians(e.getModelOffsetRot().y), new Vector3f(0, 1, 0), transformationMatrix, transformationMatrix);
			if (e.getModelOffsetRot().z != 0) Matrix4f.rotate((float) Math.toRadians(e.getModelOffsetRot().z), new Vector3f(0, 0, 1), transformationMatrix, transformationMatrix);

			if (e.getScale() != 1.0f) {
				Matrix4f.scale(new Vector3f(e.getScale(), e.getScale(), e.getScale()), transformationMatrix, transformationMatrix);
			}
			if (e.getBaseOffset().x != 0 || e.getBaseOffset().y != 0 || e.getBaseOffset().z != 0) {
				Matrix4f.translate(e.getBaseOffset(), transformationMatrix, transformationMatrix);
			}

			float[] inVertices = md.getVertices();
			float[] inTexCoords = md.getTextureCoords();
			float[] inNormals = md.getNormals();
			int[] inIndices = md.getIndices();
			
			int vCount = md.getVertexCount();

			// Köşeleri dönüştür
			for (int i = 0; i < vCount; i++) {
				float vx = inVertices[i * 3];
				float vy = inVertices[i * 3 + 1];
				float vz = inVertices[i * 3 + 2];
				
				tempVertex.set(vx, vy, vz, 1.0f);
				Matrix4f.transform(transformationMatrix, tempVertex, tempVertex);
				
				outVertices[vOffset++] = tempVertex.x;
				outVertices[vOffset++] = tempVertex.y;
				outVertices[vOffset++] = tempVertex.z;

				// Dokuları olduğu gibi al
				outTexCoords[tOffset++] = inTexCoords[i * 2];
				outTexCoords[tOffset++] = inTexCoords[i * 2 + 1];

				// Normalleri sadece döndürme ile dönüştür (Translation/Scale uygulanmaz, ama Scale uniform ise Matrix4f ile yapılabilir)
				// Ancak doğru olan matrisin inversinin transpozunu almaktır, burada basitleştirip normal matris kullanıyoruz,
				// sadece pozisyonu sıfırlayarak.
				float nx = inNormals[i * 3];
				float ny = inNormals[i * 3 + 1];
				float nz = inNormals[i * 3 + 2];
				
				tempNormal.set(nx, ny, nz, 0.0f); // W = 0 olduğu için translation etkilemez
				Matrix4f.transform(transformationMatrix, tempNormal, tempNormal);
				
				// Normalizasyon (Scale'den etkilendiyse düzeltmek için)
				Vector3f normalVec = new Vector3f(tempNormal.x, tempNormal.y, tempNormal.z);
				if (normalVec.lengthSquared() != 0) {
					normalVec.normalise();
				}
				
				outNormals[nOffset++] = normalVec.x;
				outNormals[nOffset++] = normalVec.y;
				outNormals[nOffset++] = normalVec.z;
			}

			// İndeksleri offset ile ekle
			for (int i = 0; i < inIndices.length; i++) {
				outIndices[iOffset++] = inIndices[i] + vertexIndexOffset;
			}

			// Bir sonraki model için vertex indeksini kaydır
			vertexIndexOffset += vCount;
		}

		// 4. Yeni devasa Vao ve Model nesnesi oluştur
		Vao vao = Vao.create();
		vao.storeData(outIndices, totalVertices, outVertices, outTexCoords, outNormals);
		
		// ModelData'yı da kaydetmek istersen diye (Yeni bir furtestPoint hesaplamıyoruz şimdilik, culling için gerekebilir)
		ModelData combinedData = new ModelData(outVertices, outTexCoords, outNormals, outIndices, 1000.0f);
		Model combinedModel = new Model(vao, combinedData);

		// 5. Yeni bir Varlık (Entity) oluştur ve origin (0,0,0) konumuna yerleştir
		Entity batchedEntity = new Entity(combinedModel, skin);
		batchedEntity.setPosition(new Vector3f(0, 0, 0));
		batchedEntity.getRotation().set(0, 0, 0);
		batchedEntity.setScale(1.0f);

		return batchedEntity;
	}
}
