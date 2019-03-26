package org.iota.ec;

import org.iota.ec.model.AutonomousEconomicActor;
import org.iota.ec.model.TrustedEconomicActor;
import org.iota.ec.util.SerializableAutoIndexableMerkleTree;
import org.iota.ict.model.bundle.Bundle;
import org.iota.ict.model.transaction.Transaction;
import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

class API {

    private final ECModule module;

    API(ECModule module) {
        this.module = module;
    }

    String processRequest(String request) {
        JSONObject response;
        try {
            JSONObject requestJSON = new JSONObject(request);
            String action = requestJSON.getString("action");
            response = performAction(action, requestJSON);
        } catch (Throwable t) {
            response = new JSONObject().put("success", false).put("error", t.toString());
            t.printStackTrace();
        }
        return response.toString();
    }

    private JSONObject performAction(String action, JSONObject requestJSON) {
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
            case "get_transactions":
                String bundleHead = requestJSON.getString("bundle_head");
                JSONArray transactionsJSON = getTransactionsJSON(bundleHead);
                return success.put("transactions", transactionsJSON);
            case "get_markers":
                String actor = requestJSON.getString("actor");
                JSONArray markersJSON = module.getMarkers(actor);
                return success.put("markers", markersJSON);
            case "get_tangle":
                String transaction = requestJSON.getString("transaction");
                return success.put("tangle", getTangle(transaction));
            /* ***** DO ***** */
            case "create_actor":
                module.createNewActor(SerializableAutoIndexableMerkleTree.fromJSON(requestJSON));
                Persistence.store(module);
                return success;
            case "set_trust":
                String address = requestJSON.getString("address");
                double trust = requestJSON.getDouble("trust");
                module.setTrust(address, trust);
                Persistence.store(module);
                return success;
            case "delete_actor":
                performActionDeleteActor(requestJSON);
                Persistence.store(module);
                return success;
            case "unwatch_transfer":
                String transfer = requestJSON.getString("transfer");
                module.unwatchTransfer(transfer);
                Persistence.store(module);
                return success;
            case "submit_transfer":
                String hash = performActionSubmitTransfer(requestJSON);
                Persistence.store(module);
                return success.put("hash", hash);
            case "issue_marker":
                performActionIssueMarker(requestJSON);
                return success;
            case "change_balance": {
                performActionChangeBalance(requestJSON);
                return success;
            }
            default:
                throw new IllegalArgumentException("unknown action '"+action+"'");
        }
    }

    private JSONArray getTransactionsJSON(String bundleHead) {
        JSONArray transactionsJSON = new JSONArray();
        Bundle bundle = module.getBundle(bundleHead);
        if(bundle == null)
            throw new RuntimeException("Could not find transaction " + bundleHead);
        for(Transaction transaction : bundle.getTransactions()) {
            JSONObject entry = new JSONObject();
            entry.put("hash", transaction.hash);
            entry.put("address", transaction.address());
            entry.put("value", transaction.value);
            entry.put("confidences", getConfidenceByEachActor(transaction.hash));
            entry.put("confidence", module.getConfidence(transaction.hash));
            transactionsJSON.put(entry);
        }
        return transactionsJSON;
    }

    private JSONObject getTangle(String hash) {
        JSONArray nodes = new JSONArray();
        JSONArray links = new JSONArray();

        Set<String> traversed = new HashSet<>();
        LinkedList<Transaction> transactions = new LinkedList<>();
        transactions.add(module.getIxi().findTransactionByHash(hash));

        while (transactions.size() > 0) {
            Transaction transaction = transactions.poll();
            if(transaction != null && traversed.add(transaction.hash)) {
                if(transaction.getTrunk() != null) transactions.add(transaction.getTrunk());
                if(transaction.getBranch() != null) transactions.add(transaction.getBranch());
                nodes.put(new JSONObject().put("id", transaction.hash).put("value", transaction.value.toString()));
                links.put(new JSONObject().put("source", transaction.hash).put("target", transaction.branchHash()));
                links.put(new JSONObject().put("source", transaction.hash).put("target", transaction.trunkHash()));
            }
        }

        return new JSONObject().put("nodes", nodes).put("links", links);
    }

    private JSONArray getConfidenceByEachActor(String hash) {
        JSONArray confidences = new JSONArray();
        for(TrustedEconomicActor actor : module.getTrustedActors()) {
            JSONObject entry = new JSONObject();
            entry.put("actor", actor.getAddress());
            entry.put("confidence", actor.getConfidence(hash));
            confidences.put(entry);
        }
        return confidences;
    }

    private void performActionChangeBalance(JSONObject requestJSON) {
        String address = requestJSON.getString("address");
        BigInteger toAdd = new BigInteger(requestJSON.getString("to_add"));
        module.changeInitialBalance(address, toAdd);
    }

    private void performActionIssueMarker(JSONObject requestJSON) {
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
        boolean checkBalances = requestJSON.getBoolean("check_balances");
        return module.sendTransfer(seed, index, receiverAddress, remainderAddress, value, checkBalances);
    }

    private void performActionDeleteActor(JSONObject requestJSON) {
        String address = requestJSON.getString("address");
        module.deleteAutonomousActor(address);
    }

    private JSONArray getClusterJSON() {
        JSONArray clusterJSON = new JSONArray();
        double trustSum = 0;
        for(TrustedEconomicActor actor : module.getTrustedActors())
            trustSum += actor.getTrust();
        for(TrustedEconomicActor actor : module.getTrustedActors()) {
            JSONObject jsonEntry = new JSONObject();
            jsonEntry.put("address", actor.getAddress());
            jsonEntry.put("trust_abs", actor.getTrust());
            jsonEntry.put("trust_rel", actor.getTrust()/trustSum);
            clusterJSON.put(jsonEntry);
        }
        return clusterJSON;
    }

    private JSONArray getBalancesJSON(String seed) {
        List<String> addresses = ECModule.deriveAddressesFromSeed(seed, 10);
        JSONArray balancesJSON = new JSONArray();
        for(String address : addresses) {
            // TODO custom api function
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
