import java.io.*;
import java.nio.*;

public class Dump {
    public static void main(String[] args) throws Exception {
        try (DataInputStream dis = new DataInputStream(new FileInputStream("C:/Users/mehem/eclipse-workspace/gane/src/res/DEFAULT_VEC_SHIP/fishing_boat_v.glb"))) {
            dis.readInt();
            dis.readInt();
            dis.readInt();
            int jsonChunkLength = Integer.reverseBytes(dis.readInt());
            dis.readInt();
            byte[] jsonBytes = new byte[jsonChunkLength];
            dis.readFully(jsonBytes);
            String jsonStr = new String(jsonBytes, "UTF-8");
            org.json.JSONObject gltf = new org.json.JSONObject(jsonStr);
            System.out.println("Nodes: ");
            if (gltf.has("nodes")) {
                org.json.JSONArray nodes = gltf.getJSONArray("nodes");
                for(int i=0; i<nodes.length(); i++) {
                    org.json.JSONObject node = nodes.getJSONObject(i);
                    if(node.has("translation") || node.has("rotation") || node.has("scale") || node.has("matrix")) {
                        System.out.println("Node " + i + " has transform: " + node.toString());
                    }
                }
            }
        }
    }
}
