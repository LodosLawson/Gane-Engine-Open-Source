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
            int jointsAccessorIndex = attributes.has("JOINTS_0") ? attributes.getInt("JOINTS_0") : -1;
            int weightsAccessorIndex = attributes.has("WEIGHTS_0") ? attributes.getInt("WEIGHTS_0") : -1;
            int indicesAccessorIndex = firstPrimitive.has("indices") ? firstPrimitive.getInt("indices") : -1;
            
            float[] positions = readFloatArray(positionAccessorIndex, gltf, binBuffer);
            float[] normals = normalAccessorIndex != -1 ? readFloatArray(normalAccessorIndex, gltf, binBuffer) : new float[positions.length];
            float[] texCoords = texCoordAccessorIndex != -1 ? readFloatArray(texCoordAccessorIndex, gltf, binBuffer) : new float[(positions.length / 3) * 2];
            int[] indices = indicesAccessorIndex != -1 ? readIntArray(indicesAccessorIndex, gltf, binBuffer) : generateLinearIndices(positions.length / 3);
            
            float[] joints = null;
            if (jointsAccessorIndex != -1) {
            	int[] intJoints = readIntArray(jointsAccessorIndex, gltf, binBuffer);
            	joints = new float[intJoints.length];
            	for(int i=0; i<intJoints.length; i++) joints[i] = (float)intJoints[i];
            }
            float[] weights = weightsAccessorIndex != -1 ? readFloatArray(weightsAccessorIndex, gltf, binBuffer) : null;
            
            // 5. Calculate furthest point and BoundingBox
            float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
            float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
            float furthestPoint = 0;
            
            for (int i = 0; i < positions.length; i += 3) {
                float x = positions[i];
                float y = positions[i+1];
                float z = positions[i+2];
                
                if (x < minX) minX = x;
                if (x > maxX) maxX = x;
                if (y < minY) minY = y;
                if (y > maxY) maxY = y;
                if (z < minZ) minZ = z;
                if (z > maxZ) maxZ = z;
                
                float distSq = x*x + y*y + z*z;
                if (distSq > furthestPoint) {
                    furthestPoint = distSq;
                }
            }
            furthestPoint = (float) Math.sqrt(furthestPoint);
            
            ModelData modelData = new ModelData(positions, texCoords, normals, indices, joints, weights, furthestPoint, minX, maxX, minY, maxY, minZ, maxZ);
            
            // 6. Extract Animation Data
            if (gltf.has("skins") && joints != null) {
            	org.json.JSONArray skins = gltf.getJSONArray("skins");
            	org.json.JSONObject skin = skins.getJSONObject(0);
            	org.json.JSONArray jointsArray = skin.getJSONArray("joints");
            	int inverseBindAccessorIndex = skin.getInt("inverseBindMatrices");
            	float[] inverseBindMatrices = readFloatArray(inverseBindAccessorIndex, gltf, binBuffer);
            	
            	scene.animation.Joint[] allJoints = GLBAnimatorBuilder.parseSkeleton(gltf, binBuffer, inverseBindMatrices, jointsArray);
            	
            	if (allJoints != null && allJoints.length > 0) {
            		scene.animation.Joint rootJoint = null;
            		// Find root joint (the one without parent in allJoints? Actually, just the first node or any node that isn't a child of another joint)
            		// We can just set the root as the first joint in jointsArray
            		rootJoint = allJoints[0]; 
            		
            		scene.animation.Animation anim = GLBAnimatorBuilder.parseAnimation(gltf, binBuffer, jointsArray, allJoints);
            		
            		modelData.setAnimationData(rootJoint, allJoints.length, anim);
            	}
            }
            
            // 7. Extract Texture Data
            if (gltf.has("images") && gltf.has("materials")) {
                org.json.JSONArray materials = gltf.getJSONArray("materials");
                if (materials.length() > 0) {
                    org.json.JSONObject mat = materials.getJSONObject(0);
                    if (mat.has("pbrMetallicRoughness")) {
                        org.json.JSONObject pbr = mat.getJSONObject("pbrMetallicRoughness");
                        if (pbr.has("baseColorTexture")) {
                            int texIndex = pbr.getJSONObject("baseColorTexture").getInt("index");
                            org.json.JSONArray textures = gltf.getJSONArray("textures");
                            int imgSource = textures.getJSONObject(texIndex).getInt("source");
                            
                            org.json.JSONArray images = gltf.getJSONArray("images");
                            org.json.JSONObject img = images.getJSONObject(imgSource);
                            
                            if (img.has("bufferView")) {
                                int bvIndex = img.getInt("bufferView");
                                org.json.JSONObject bufferView = gltf.getJSONArray("bufferViews").getJSONObject(bvIndex);
                                int byteOffset = bufferView.has("byteOffset") ? bufferView.getInt("byteOffset") : 0;
                                int byteLength = bufferView.getInt("byteLength");
                                
                                byte[] imgData = new byte[byteLength];
                                binBuffer.position(byteOffset);
                                binBuffer.get(imgData, 0, byteLength);
                                
                                modelData.setEmbeddedTextureData(imgData);
                            }
                        }
                    }
                }
            }
            
            return modelData;

        } catch (Exception e) {
            System.err.println("Failed to load GLB: " + file.getPath());
            e.printStackTrace();
            return null;
        }
    }

    public static java.util.List<ModelData> loadGLBModels(MyFile file) {
        java.util.List<ModelData> models = new java.util.ArrayList<>();
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
            
            // 4. Iterate Over All Meshes and Primitives
            JSONArray meshes = gltf.getJSONArray("meshes");
            for (int meshIndex = 0; meshIndex < meshes.length(); meshIndex++) {
                JSONObject mesh = meshes.getJSONObject(meshIndex);
                JSONArray primitives = mesh.getJSONArray("primitives");
                
                for (int primIndex = 0; primIndex < primitives.length(); primIndex++) {
                    JSONObject primitive = primitives.getJSONObject(primIndex);
                    
                    JSONObject attributes = primitive.getJSONObject("attributes");
                    int positionAccessorIndex = attributes.getInt("POSITION");
                    int normalAccessorIndex = attributes.has("NORMAL") ? attributes.getInt("NORMAL") : -1;
                    int texCoordAccessorIndex = attributes.has("TEXCOORD_0") ? attributes.getInt("TEXCOORD_0") : -1;
                    int jointsAccessorIndex = attributes.has("JOINTS_0") ? attributes.getInt("JOINTS_0") : -1;
                    int weightsAccessorIndex = attributes.has("WEIGHTS_0") ? attributes.getInt("WEIGHTS_0") : -1;
                    int indicesAccessorIndex = primitive.has("indices") ? primitive.getInt("indices") : -1;
                    
                    float[] positions = readFloatArray(positionAccessorIndex, gltf, binBuffer);
                    float[] normals = normalAccessorIndex != -1 ? readFloatArray(normalAccessorIndex, gltf, binBuffer) : new float[positions.length];
                    float[] texCoords = texCoordAccessorIndex != -1 ? readFloatArray(texCoordAccessorIndex, gltf, binBuffer) : new float[(positions.length / 3) * 2];
                    int[] indices = indicesAccessorIndex != -1 ? readIntArray(indicesAccessorIndex, gltf, binBuffer) : generateLinearIndices(positions.length / 3);
                    
                    float[] joints = null;
                    if (jointsAccessorIndex != -1) {
                        int[] intJoints = readIntArray(jointsAccessorIndex, gltf, binBuffer);
                        joints = new float[intJoints.length];
                        for(int i=0; i<intJoints.length; i++) joints[i] = (float)intJoints[i];
                    }
                    float[] weights = weightsAccessorIndex != -1 ? readFloatArray(weightsAccessorIndex, gltf, binBuffer) : null;
                    
                    float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
                    float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
                    float furthestPoint = 0;
                    
                    for (int i = 0; i < positions.length; i += 3) {
                        float x = positions[i];
                        float y = positions[i+1];
                        float z = positions[i+2];
                        
                        if (x < minX) minX = x;
                        if (x > maxX) maxX = x;
                        if (y < minY) minY = y;
                        if (y > maxY) maxY = y;
                        if (z < minZ) minZ = z;
                        if (z > maxZ) maxZ = z;
                        
                        float distSq = x*x + y*y + z*z;
                        if (distSq > furthestPoint) {
                            furthestPoint = distSq;
                        }
                    }
                    furthestPoint = (float) Math.sqrt(furthestPoint);
                    
                    ModelData modelData = new ModelData(positions, texCoords, normals, indices, joints, weights, furthestPoint,
                            minX, maxX, minY, maxY, minZ, maxZ);
                    
                    // Extract Texture Data and Material properties for this specific primitive
                    if (primitive.has("material") && gltf.has("images") && gltf.has("materials")) {
                        int materialIndex = primitive.getInt("material");
                        org.json.JSONObject mat = gltf.getJSONArray("materials").getJSONObject(materialIndex);
                        
                        if (mat.has("doubleSided") && mat.getBoolean("doubleSided")) {
                            modelData.setDoubleSided(true);
                        }
                        if (mat.has("alphaMode") && !mat.getString("alphaMode").equals("OPAQUE")) {
                            modelData.setTransparent(true);
                        }
                        
                        if (mat.has("pbrMetallicRoughness")) {
                            org.json.JSONObject pbr = mat.getJSONObject("pbrMetallicRoughness");
                            if (pbr.has("baseColorTexture")) {
                                int texIndex = pbr.getJSONObject("baseColorTexture").getInt("index");
                                org.json.JSONArray textures = gltf.getJSONArray("textures");
                                int imgSource = textures.getJSONObject(texIndex).getInt("source");
                                
                                org.json.JSONObject img = gltf.getJSONArray("images").getJSONObject(imgSource);
                                if (img.has("bufferView")) {
                                    int bvIndex = img.getInt("bufferView");
                                    org.json.JSONObject bufferView = gltf.getJSONArray("bufferViews").getJSONObject(bvIndex);
                                    int byteOffset = bufferView.has("byteOffset") ? bufferView.getInt("byteOffset") : 0;
                                    int byteLength = bufferView.getInt("byteLength");
                                    
                                    byte[] imgData = new byte[byteLength];
                                    binBuffer.position(byteOffset);
                                    binBuffer.get(imgData, 0, byteLength);
                                    
                                    modelData.setEmbeddedTextureData(imgData);
                                }
                            }
                        }
                    }
                    String meshName = mesh.has("name") ? mesh.getString("name") : "Mesh_" + meshIndex;
                    modelData.setName(meshName);
                    
                    models.add(modelData);
                }
            }
            return models;

        } catch (Exception e) {
            System.err.println("Failed to load GLB: " + file.getPath());
            e.printStackTrace();
            return models; // Return what we have so far
        }
    }

    public static float[] readFloatArray(int accessorIndex, JSONObject gltf, ByteBuffer binBuffer) {
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
        if (type.equals("MAT2")) numComponents = 4;
        if (type.equals("MAT3")) numComponents = 9;
        if (type.equals("MAT4")) numComponents = 16;
        
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

    public static int[] readIntArray(int accessorIndex, JSONObject gltf, ByteBuffer binBuffer) {
        JSONObject accessor = gltf.getJSONArray("accessors").getJSONObject(accessorIndex);
        int bufferViewIndex = accessor.getInt("bufferView");
        JSONObject bufferView = gltf.getJSONArray("bufferViews").getJSONObject(bufferViewIndex);
        
        int byteOffset = (accessor.has("byteOffset") ? accessor.getInt("byteOffset") : 0) + 
                         (bufferView.has("byteOffset") ? bufferView.getInt("byteOffset") : 0);
        int count = accessor.getInt("count");
        int componentType = accessor.getInt("componentType");
        String type = accessor.getString("type");
        
        int numComponents = 1;
        if (type.equals("VEC2")) numComponents = 2;
        if (type.equals("VEC3")) numComponents = 3;
        if (type.equals("VEC4")) numComponents = 4;
        
        int bytesPerComponent = (componentType == 5123 ? 2 : (componentType == 5125 ? 4 : 1));
        int byteStride = bufferView.has("byteStride") ? bufferView.getInt("byteStride") : (bytesPerComponent * numComponents);
        
        int[] result = new int[count * numComponents];
        for (int i = 0; i < count; i++) {
            binBuffer.position(byteOffset + i * byteStride);
            for (int j = 0; j < numComponents; j++) {
	            if (componentType == 5123) { // UNSIGNED_SHORT
	                result[i * numComponents + j] = binBuffer.getShort() & 0xFFFF;
	            } else if (componentType == 5125) { // UNSIGNED_INT
	                result[i * numComponents + j] = binBuffer.getInt();
	            } else if (componentType == 5121) { // UNSIGNED_BYTE
	                result[i * numComponents + j] = binBuffer.get() & 0xFF;
	            }
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
