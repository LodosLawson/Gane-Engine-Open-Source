package objConverter;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

import org.json.JSONArray;
import org.json.JSONObject;

import utils.MyFile;
import org.lwjgl.util.vector.Vector3f;

public class GLBFileLoader {

    public static ModelData loadGLB(MyFile file) {
        try (DataInputStream dis = new DataInputStream(file.getInputStream())) {
            
            // 1. Read Header (12 bytes)
            int magic = Integer.reverseBytes(dis.readInt());
            if (magic != 0x46546C67) { // "glTF"
                throw new RuntimeException("Not a valid GLB file: " + file.getPath());
            }
            int version = Integer.reverseBytes(dis.readInt());
            int length = Integer.reverseBytes(dis.readInt());

            // 2. Read JSON Chunk
            int jsonChunkLength = Integer.reverseBytes(dis.readInt());
            int jsonChunkType = Integer.reverseBytes(dis.readInt());
            if (jsonChunkType != 0x4E4F534A) { // "JSON"
                throw new RuntimeException("First chunk must be JSON in GLB");
            }
            
            byte[] jsonBytes = new byte[jsonChunkLength];
            dis.readFully(jsonBytes);
            String jsonString = new String(jsonBytes, StandardCharsets.UTF_8);
            JSONObject gltf = new JSONObject(jsonString);
            
            // 3. Read Binary Chunk
            int binChunkLength = Integer.reverseBytes(dis.readInt());
            int binChunkType = Integer.reverseBytes(dis.readInt());
            if (binChunkType != 0x004E4942) { // "BIN\0"
                throw new RuntimeException("Second chunk must be BIN");
            }
            
            byte[] binBytes = new byte[binChunkLength];
            dis.readFully(binBytes);
            ByteBuffer binBuffer = ByteBuffer.wrap(binBytes).order(ByteOrder.LITTLE_ENDIAN);
            
            // 4. Extract Mesh Data
            JSONArray meshes = gltf.getJSONArray("meshes");
            JSONObject firstMesh = meshes.getJSONObject(0);
            JSONObject firstPrimitive = firstMesh.getJSONArray("primitives").getJSONObject(0);
            
            JSONObject attributes = firstPrimitive.getJSONObject("attributes");
            int positionAccessorIndex = attributes.getInt("POSITION");
            int normalAccessorIndex = attributes.has("NORMAL") ? attributes.getInt("NORMAL") : -1;
            int texCoordAccessorIndex = attributes.has("TEXCOORD_0") ? attributes.getInt("TEXCOORD_0") : -1;
            int indicesAccessorIndex = firstPrimitive.has("indices") ? firstPrimitive.getInt("indices") : -1;
            
            float[] positions = readFloatArray(positionAccessorIndex, gltf, binBuffer);
            float[] normals = normalAccessorIndex != -1 ? readFloatArray(normalAccessorIndex, gltf, binBuffer) : new float[positions.length];
            float[] texCoords = texCoordAccessorIndex != -1 ? readFloatArray(texCoordAccessorIndex, gltf, binBuffer) : new float[(positions.length / 3) * 2];
            int[] indices = indicesAccessorIndex != -1 ? readIntArray(indicesAccessorIndex, gltf, binBuffer) : generateLinearIndices(positions.length / 3);
            
            // 5. Calculate furthest point
            float furthestPoint = 0;
            for (int i = 0; i < positions.length; i += 3) {
                float x = positions[i];
                float y = positions[i+1];
                float z = positions[i+2];
                float distSq = x*x + y*y + z*z;
                if (distSq > furthestPoint) {
                    furthestPoint = distSq;
                }
            }
            furthestPoint = (float) Math.sqrt(furthestPoint);
            
            return new ModelData(positions, texCoords, normals, indices, furthestPoint);

        } catch (Exception e) {
            System.err.println("Failed to load GLB: " + file.getPath());
            e.printStackTrace();
            return null;
        }
    }

    private static float[] readFloatArray(int accessorIndex, JSONObject gltf, ByteBuffer binBuffer) {
        JSONObject accessor = gltf.getJSONArray("accessors").getJSONObject(accessorIndex);
        int bufferViewIndex = accessor.getInt("bufferView");
        JSONObject bufferView = gltf.getJSONArray("bufferViews").getJSONObject(bufferViewIndex);
        
        int byteOffset = (accessor.has("byteOffset") ? accessor.getInt("byteOffset") : 0) + 
                         (bufferView.has("byteOffset") ? bufferView.getInt("byteOffset") : 0);
        int count = accessor.getInt("count");
        String type = accessor.getString("type");
        
        int numComponents = 1;
        if (type.equals("VEC2")) numComponents = 2;
        if (type.equals("VEC3")) numComponents = 3;
        if (type.equals("VEC4")) numComponents = 4;
        
        int byteStride = bufferView.has("byteStride") ? bufferView.getInt("byteStride") : (numComponents * 4);
        
        float[] result = new float[count * numComponents];
        for (int i = 0; i < count; i++) {
            binBuffer.position(byteOffset + i * byteStride);
            for (int j = 0; j < numComponents; j++) {
                result[i * numComponents + j] = binBuffer.getFloat();
            }
        }
        return result;
    }

    private static int[] readIntArray(int accessorIndex, JSONObject gltf, ByteBuffer binBuffer) {
        JSONObject accessor = gltf.getJSONArray("accessors").getJSONObject(accessorIndex);
        int bufferViewIndex = accessor.getInt("bufferView");
        JSONObject bufferView = gltf.getJSONArray("bufferViews").getJSONObject(bufferViewIndex);
        
        int byteOffset = (accessor.has("byteOffset") ? accessor.getInt("byteOffset") : 0) + 
                         (bufferView.has("byteOffset") ? bufferView.getInt("byteOffset") : 0);
        int count = accessor.getInt("count");
        int componentType = accessor.getInt("componentType");
        
        int byteStride = bufferView.has("byteStride") ? bufferView.getInt("byteStride") : 
                         (componentType == 5123 ? 2 : (componentType == 5125 ? 4 : 1));
        
        int[] result = new int[count];
        for (int i = 0; i < count; i++) {
            binBuffer.position(byteOffset + i * byteStride);
            if (componentType == 5123) { // UNSIGNED_SHORT
                result[i] = binBuffer.getShort() & 0xFFFF;
            } else if (componentType == 5125) { // UNSIGNED_INT
                result[i] = binBuffer.getInt();
            } else if (componentType == 5121) { // UNSIGNED_BYTE
                result[i] = binBuffer.get() & 0xFF;
            }
        }
        return result;
    }

    private static int[] generateLinearIndices(int count) {
        int[] indices = new int[count];
        for (int i = 0; i < count; i++) {
            indices[i] = i;
        }
        return indices;
    }
}
