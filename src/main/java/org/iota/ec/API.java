package org.iota.ec;

import org.iota.ict.ec.AutonomousEconomicActor;
import org.iota.ict.ec.TrustedEconomicActor;
import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigInteger;
import java.util.List;

class API {

    private final ECModule module;

    API(ECModule module) {
        this.module = module;
    }

    String processRequest(String request) {
        JSONObject response;
        try {
            System.out.println("--> " + request);
            JSONObject requestJSON = new JSONObject(request);
            String action = requestJSON.getString("action");
            response = performAction(action, requestJSON);
        } catch (Throwable t) {
            response = new JSONObject().put("success", false).put("error", t.toString());
            t.printStackTrace();
        }
        return response.toString();
    }

    JSONObject performAction(String action, JSONObject requestJSON) {
        JSONObject success = new JSONObject().put("success", true);
        switch (action) {
            /* ***** GET ***** */
            case "get_cluster":
                return success.put("cluster", getClusterJSON());
            case "get_cluster_confidences":
                JSONArray hashes = requestJSON.getJSONArray("hashes");
                return success.put("cluster_confidences", getClusterConfidences(hashes));
            case "get_actors":
                return success.put("actors", getActorsJSON());
            case "get_balances":
                String seed = requestJSON.getString("seed");
                return success.put("balances", getBalancesJSON(seed));
            case "get_transfers":
                JSONArray transfersJSON = new JSONArray(module.getTransfers());
                return success.put("transfers", transfersJSON);
            /* ***** DO ***** */
            case "create_actor":
                performActionCreateActor(requestJSON);
                return success;
            case "add_actor":
                String address = requestJSON.getString("address");
                double trust = requestJSON.getDouble("trust");
                module.addActorToCluster(address, trust);
                return success;
            case "submit_transfer":
                String hash = performActionSubmitTransfer(requestJSON);
                return success.put("hash", hash);
            case "consider_tangle":
                performActionConsiderTangle(requestJSON);
                return success;
            default:
                throw new IllegalArgumentException("unknown action '"+action+"'");
        }
    }

    private void performActionConsiderTangle(JSONObject requestJSON) {
        String actor = requestJSON.getString("actor");
        String trunk = requestJSON.getString("trunk");
        String branch = requestJSON.getString("branch");
        module.considerTangle(actor, trunk, branch);
    }

    private JSONObject getClusterConfidences(JSONArray hashes) {
        JSONObject confidences = new JSONObject();
        for(int i = 0; i < hashes.length(); i++) {
            String hash = hashes.getString(i);
            confidences.put(hash, module.getConfidence(hash));
        }
        return confidences;
    }

    private String performActionSubmitTransfer(JSONObject requestJSON) {
        String seed = requestJSON.getString("seed");
        int index = requestJSON.getInt("index");
        String receiverAddress = requestJSON.getString("receiver");
        String remainderAddress = requestJSON.getString("remainder");
        BigInteger value = new BigInteger(requestJSON.getString("value"));
        return module.sendTransfer(seed, index, receiverAddress, remainderAddress, value);
    }

    private void performActionCreateActor(JSONObject requestJSON) {
        String seed = requestJSON.getString("seed");
        int merkleTreeDepth = requestJSON.getInt("merkle_tree_depth");
        int startIndex = requestJSON.getInt("start_index");
        module.createNewActor(seed, merkleTreeDepth, startIndex);
    }

    private JSONArray getClusterJSON() {
        JSONArray clusterJSON = new JSONArray();
        for(TrustedEconomicActor actor : module.getTrustedActors()) {
            JSONObject jsonEntry = new JSONObject();
            jsonEntry.put("address", actor.getAddress());
            jsonEntry.put("trust", actor.getTrust());
            clusterJSON.put(jsonEntry);
        }
        return clusterJSON;
    }

    private JSONArray getBalancesJSON(String seed) {
        List<String> addresses = module.deriveAddressesFromSeed(seed, 10);
        JSONArray balancesJSON = new JSONArray();
        for(String address : addresses) {
            JSONObject jsonEntry = new JSONObject();
            jsonEntry.put("address", address);
            jsonEntry.put("balance", module.getBalanceOfAddress(address));
            balancesJSON.put(jsonEntry);
        }
        return balancesJSON;
    }

    private JSONArray getActorsJSON() {
        JSONArray actorsJSON = new JSONArray();
        for(AutonomousEconomicActor actor : module.getAutonomousActors()) {
            actorsJSON.put(actor.getAddress());
        }
        return actorsJSON;
    }
}
