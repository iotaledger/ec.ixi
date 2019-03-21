package org.iota.ec;

import org.iota.ict.ec.AutonomousEconomicActor;
import org.iota.ict.ec.ControlledEconomicActor;
import org.iota.ict.ec.EconomicCluster;
import org.iota.ict.ec.TrustedEconomicActor;
import org.iota.ict.ixi.Ixi;
import org.iota.ict.ixi.IxiModule;
import org.iota.ict.ixi.context.IxiContext;
import org.iota.ict.ixi.context.SimpleIxiContext;
import org.iota.ict.model.bundle.Bundle;
import org.iota.ict.model.bundle.BundleBuilder;
import org.iota.ict.model.transaction.Transaction;
import org.iota.ict.model.transfer.InputBuilder;
import org.iota.ict.model.transfer.OutputBuilder;
import org.iota.ict.model.transfer.TransferBuilder;
import org.iota.ict.utils.crypto.AutoIndexedMerkleTree;
import org.iota.ict.utils.crypto.SignatureSchemeImplementation;
import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigInteger;
import java.util.*;

public class ECModule extends IxiModule {

    private static final int TIPS_PER_TRANSFER = 1;
    private static final int TRANSFER_SECURITY = 1;
    private static final double CONFIRMATION_CONFIDENCE = 0.95;

    private final EconomicCluster cluster;
    private final List<AutonomousEconomicActor> autonomousActors = new LinkedList<>();
    private final List<TrustedEconomicActor> trustedActors = new LinkedList<>();
    private final Map<String, BigInteger> initialBalances = new HashMap<>();

    private final IxiContext context = new ECContext();

    public ECModule(Ixi ixi) {
        super(ixi);
        this.cluster = new EconomicCluster(ixi);
    }

    @Override
    public void run() {
        System.err.println("EC is running!");
    }

    @Override
    public IxiContext getContext() {
        return context;
    }

    private Object performAction(String action, JSONObject requestJSON) {
        switch (action) {
            case "hello":
                return performActionHello(requestJSON);
            default:
                throw new IllegalArgumentException("unknown action '"+action+"'");
        }
    }

    private JSONObject performActionHello(JSONObject requestJSON) {
        return new JSONObject().put("hello", requestJSON.getString("name"));
    }

    private void considerTangle(String actorAddress, String trunk, String branch) {
        AutonomousEconomicActor actor = findAutonomousActor(actorAddress);
        if(actor == null)
            throw new IllegalArgumentException("None of the actors controlled by you has the address '"+actorAddress+"'");
        // TODO validate Tangle
        actor.buildMarker(trunk, branch, 0.05); // TODO initial confidence
    }

    private AutonomousEconomicActor findAutonomousActor(String address) {
        for(AutonomousEconomicActor actor : autonomousActors) {
            if(actor.getAddress().equals(address))
                return actor;
        }
        return null;
    }

    private JSONObject getClusterConfidences(Set<String> hashes) {
        JSONObject confidences = new JSONObject();
        for(String hash : hashes)
            confidences.put(hash, cluster.determineApprovalConfidence(hash));
        return confidences;
    }

    private JSONArray getClusterJSON() {
        JSONArray clusterJSON = new JSONArray();
        for(TrustedEconomicActor actor : trustedActors) {
            JSONObject jsonEntry = new JSONObject();
            jsonEntry.put("address", actor.getAddress());
            jsonEntry.put("trust", actor.getTrust());
            clusterJSON.put(jsonEntry);
        }
        return clusterJSON;
    }

    private JSONArray getActorsJSON() {
        JSONArray actorsJSON = new JSONArray();
        for(AutonomousEconomicActor actor : autonomousActors) {
            actorsJSON.put(actor.getAddress());
        }
        return actorsJSON;
    }

    private void createNewActor(String seed, int merkleTreeDepth, int startIndex) {
        AutoIndexedMerkleTree merkleTree = new AutoIndexedMerkleTree(seed, 3, merkleTreeDepth, startIndex);
        AutonomousEconomicActor actor = new AutonomousEconomicActor(ixi, cluster, initialBalances, merkleTree);
        autonomousActors.add(actor);
    }

    private void addActorToCluster(String address, double trust) {
        TrustedEconomicActor trustedEconomicActor = new TrustedEconomicActor(address, trust);
        cluster.addActor(trustedEconomicActor);
        trustedActors.add(trustedEconomicActor);
    }

    private String sendTransfer(String seed, int index, String receiverAddress, String remainderAddress, BigInteger value) {
        SignatureSchemeImplementation.PrivateKey privateKey = SignatureSchemeImplementation.derivePrivateKeyFromSeed(seed, index, TRANSFER_SECURITY);
        BigInteger balance = getBalanceOfAddress(privateKey.deriveAddress());
        Set<String> tips = findTips(TIPS_PER_TRANSFER);
        TransferBuilder transferBuilder = buildTransfer(privateKey, balance, receiverAddress, remainderAddress, value, tips);
        return submitTransfer(transferBuilder);
    }

    private Set<String> findTips(int amount) {
        Set<String> tips = new HashSet<>();
        for(int i = 0; i < amount; i++)
            tips.add(findTip());
        return tips;
    }

    private String findTip() {
        throw new RuntimeException("implement me");
    }

    private TransferBuilder buildTransfer(SignatureSchemeImplementation.PrivateKey privateKey, BigInteger balance, String receiverAddress, String remainderAddress, BigInteger value, Set<String> references) {
        if(balance.compareTo(value) < 0)
            throw new IllegalArgumentException("insufficient balance (balance="+balance+" < value="+value+")");
        InputBuilder inputBuilder = new InputBuilder(privateKey, balance);
        Set<OutputBuilder> outputs = new HashSet<>();
        outputs.add(new OutputBuilder(receiverAddress, value, "EC9RECEIVER"));
        outputs.add(new OutputBuilder(remainderAddress, balance.subtract(value), "EC9REMAINDER"));
        return new TransferBuilder(Collections.singleton(inputBuilder), outputs, TRANSFER_SECURITY);
    }

    /**
     * Builds a transfer and submits it to the Ict network.
     * @param transferBuilder Modeled transfer to submit.
     * @return Hash of bundle head of submitted bundle.
     * */
    private String submitTransfer(TransferBuilder transferBuilder) {
        BundleBuilder bundleBuilder = transferBuilder.build();
        Bundle bundle = bundleBuilder.build();
        for(Transaction transaction : bundle.getTransactions())
            ixi.submit(transaction);
        return bundle.getHead().hash;
    }

    private JSONArray getBalancesJSON(String seed) {
        List<String> addresses = deriveAddressesFromSeed(seed, 10);
        JSONArray balancesJSON = new JSONArray();
        for(String address : addresses) {
            JSONObject jsonEntry = new JSONObject();
            jsonEntry.put("address", address);
            jsonEntry.put("balance", getBalanceOfAddress(address));
            balancesJSON.put(jsonEntry);
        }
        return balancesJSON;
    }

    private BigInteger getBalanceOfAddress(String address) {
        BigInteger sum = BigInteger.ZERO;
        Set<Transaction> transactionsOnAddress = ixi.findTransactionsByAddress(address);
        for(Transaction transaction : transactionsOnAddress) {
            if(!transaction.value.equals(BigInteger.ZERO)) {
                double confidence = cluster.determineApprovalConfidence(transaction.hash);
                if(confidence > CONFIRMATION_CONFIDENCE)
                    sum = sum.add(transaction.value);
            }
        }
        return sum;
    }

    private List<String> deriveAddressesFromSeed(String seed, int amount) {
        List<String> addresses = new LinkedList<>();
        for(int i = 0; i < amount; i++)
            addresses.add(SignatureSchemeImplementation.deriveAddressFromSeed(seed, i, TRANSFER_SECURITY));
        return addresses;
    }

    private class ECContext extends SimpleIxiContext {

        public String respondToRequest(String request) {
            JSONObject requestJSON = new JSONObject(request);
            String action = requestJSON.getString("action");
            Object responseJSON = performAction(action, requestJSON);
            return responseJSON.toString();
        }
    }
}