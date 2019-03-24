package org.iota.ec.util;

import org.iota.ict.utils.crypto.AutoIndexedMerkleTree;
import org.json.JSONObject;

import java.io.Serializable;

public class SerializableAutoIndexableMerkleTree extends AutoIndexedMerkleTree {

    private final String seed;

    public static SerializableAutoIndexableMerkleTree fromJSON(JSONObject json) {
        String seed = json.getString("seed");
        int securityLevel = json.getInt("security_level");
        int depth = json.getInt("depth");
        int startIndex = json.getInt("index");
        return new SerializableAutoIndexableMerkleTree(seed, securityLevel, depth, startIndex);
    }

    public SerializableAutoIndexableMerkleTree(String seed, int securityLevel, int depth) {
        super(seed, securityLevel, depth);
        this.seed = seed;
    }

    public SerializableAutoIndexableMerkleTree(String seed, int securityLevel, int depth, int startIndex) {
        super(seed, securityLevel, depth, startIndex);
        this.seed = seed;
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("seed", seed);
        json.put("index", index);
        json.put("security_level", getSecurityLevel());
        json.put("depth", getDepth());
        return json;
    }
}
