package org.iota.ec;

import org.iota.ec.model.AutonomousEconomicActor;
import org.iota.ec.model.TrustedEconomicActor;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Persistence {

    private static final Path persistence = Paths.get("persistence.json");

    private final ECModule module;

    private Persistence(ECModule module) {
        this.module = module;
    }

    public static void store(ECModule module) {
        try {
            new Persistence(module).store();
        } catch (IOException e) {
            throw new RuntimeException("Could not store state of EC.ixi in " + persistence, e);
        }
    }

    public static void load(ECModule module) {
        try {
            if(Files.exists(persistence))
                new Persistence(module).load();
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize EC.ixi from " + persistence, e);
        }
    }

    private void store() throws IOException {
        JSONObject persistenceJSON = new JSONObject();
        persistenceJSON.put("trusted", serializeTrustedActors());
        persistenceJSON.put("autonomous", serializeAutonomousActors());
        persistenceJSON.put("transfers", serializeTransfers());
        write(persistence, persistenceJSON.toString());
    }

    public void load() throws IOException {
        JSONObject persistenceJSON = new JSONObject(read(persistence));
        deserializeTrustedActors(persistenceJSON.getJSONArray("trusted"));
        deserializeTransfers(persistenceJSON.getJSONArray("transfers"));
    }

    private JSONArray serializeTrustedActors() {
        JSONArray array = new JSONArray();
        for(TrustedEconomicActor actor : module.getTrustedActors()) {
            JSONObject entry = new JSONObject();
            entry.put("address", actor.getAddress());
            entry.put("trust", actor.getTrust());
            array.put(entry);
        }
        return array;
    }

    private JSONArray serializeAutonomousActors() {
        JSONArray array = new JSONArray();
        for(AutonomousEconomicActor actor : module.getAutonomousActors()) {
            JSONObject entry = new JSONObject();
            // TODO store merkle tree
            array.put(entry);
        }
        return array;
    }

    private void deserializeTrustedActors(JSONArray serialized) {
        for(int i = 0; i < serialized.length(); i++) {
            JSONObject entry = serialized.getJSONObject(i);
            String address = entry.getString("address");
            double trust = entry.getDouble("trust");
            module.setTrust(address, trust);
        }
    }

    private JSONArray serializeTransfers() {
        return new JSONArray(module.getTransfers());
    }

    private void deserializeTransfers(JSONArray serialized) {
        for(int i = 0; i < serialized.length(); i++) {
            module.watchTransfer(serialized.getString(i));
        }
    }

    private static void write(Path path, String data) throws IOException {
        Files.write(path, data.getBytes());
    }

    private static String read(Path path) throws IOException {
        return new String(Files.readAllBytes(path));
    }
}
