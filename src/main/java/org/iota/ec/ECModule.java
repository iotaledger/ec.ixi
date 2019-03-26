package org.iota.ec;

import org.iota.ec.model.AutonomousEconomicActor;
import org.iota.ec.model.TrustedEconomicActor;
import org.iota.ec.model.EconomicCluster;
import org.iota.ec.util.SerializableAutoIndexableMerkleTree;
import org.iota.ict.ixi.Ixi;
import org.iota.ict.ixi.IxiModule;
import org.iota.ict.ixi.context.IxiContext;
import org.iota.ict.ixi.context.SimpleIxiContext;
import org.iota.ict.model.bundle.Bundle;
import org.iota.ict.model.bundle.BundleBuilder;
import org.iota.ict.model.transaction.Transaction;
import org.iota.ict.model.transaction.TransactionBuilder;
import org.iota.ict.model.transfer.InputBuilder;
import org.iota.ict.model.transfer.OutputBuilder;
import org.iota.ict.model.transfer.TransferBuilder;
import org.iota.ict.utils.Trytes;
import org.iota.ict.utils.crypto.SignatureSchemeImplementation;
import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigInteger;
import java.util.*;

public class ECModule extends IxiModule {

    private static final int TIPS_PER_TRANSFER = 1;
    private static final int TRANSFER_SECURITY = 1;
    private static final double CONFIRMATION_CONFIDENCE = 0.95;

    private final API api;
    private final EconomicCluster cluster;
    private final List<AutonomousEconomicActor> autonomousActors = new LinkedList<>();
    private final List<String> transfers = new LinkedList<>();
    private final Map<String, BigInteger> initialBalances = new HashMap<>();

    private final IxiContext context = new ECContext();

    public ECModule(Ixi ixi) {
        super(ixi);
        this.cluster = new EconomicCluster(ixi);
        this.api = new API(this);
    }

    /****** IXI ******/

    @Override
    public void run() {
        System.err.println("EC is running!");
    }

    @Override
    public IxiContext getContext() {
        return context;
    }

    @Override
    public void onStarted() {
        Persistence.load(this);
    }

    @Override
    public void onTerminate() {
        Persistence.store(this);
    }

    private class ECContext extends SimpleIxiContext {
        public String respondToRequest(String request) {
            return api.processRequest(request);
        }
    }

    /****** SERVICES ******/

    void considerTangle(String actorAddress, String trunk, String branch) {
        AutonomousEconomicActor actor = findAutonomousActor(actorAddress);
        if(actor == null)
            throw new IllegalArgumentException("None of the actors controlled by you has the address '"+actorAddress+"'");
        actor.tick(Collections.singleton(trunk+branch));
    }

    double getConfidence(String hash) {
        return cluster.determineApprovalConfidence(hash);
    }

    JSONArray getMarkers(String actorAddress) {
        TrustedEconomicActor actor = findTrustedActor(actorAddress);
        if(actor == null)
            throw new IllegalArgumentException("You are not following an actor with the address '"+actorAddress+"'");
        JSONArray markers = new JSONArray();
        for(Map.Entry<String, Double> tangle : actor.getMarkedTangles().entrySet()) {
            JSONObject marker = new JSONObject();
            marker.put("ref1", tangle.getKey().substring(0, 81));
            marker.put("ref2", tangle.getKey().substring(81));
            marker.put("confidence", tangle.getValue());
            markers.put(marker);
        }
        return markers;
    }

    String createNewActor(SerializableAutoIndexableMerkleTree merkleTree) {
        AutonomousEconomicActor actor = new AutonomousEconomicActor(ixi, cluster, initialBalances, merkleTree);
        autonomousActors.add(actor);
        return actor.getAddress();
    }

    void deleteAutonomousActor(String address) {
        AutonomousEconomicActor actor = findAutonomousActor(address);
        if(actor == null)
            throw new IllegalArgumentException("You do not own an actor with address '"+address+"'.");
        autonomousActors.remove(actor);
    }

    void setTrust(String address, double trust) {

        TrustedEconomicActor actor;
        if((actor = findTrustedActor(address)) != null) {
            actor.setTrust(trust);
            if(trust == 0) {
                cluster.removeActor(actor);
            }
        } else if(trust > 0) {
            TrustedEconomicActor trustedEconomicActor = new TrustedEconomicActor(address, trust);
            cluster.addActor(trustedEconomicActor, true);
        }
    }

    String sendTransfer(String seed, int index, String receiverAddress, String remainderAddress, BigInteger value, boolean checkBalances, Collection<String> tips) {
        SignatureSchemeImplementation.PrivateKey privateKey = SignatureSchemeImplementation.derivePrivateKeyFromSeed(seed, index, TRANSFER_SECURITY);
        BigInteger balance = getBalanceOfAddress(privateKey.deriveAddress());
        TransferBuilder transferBuilder = buildTransfer(privateKey, balance, receiverAddress, remainderAddress, value, checkBalances, tips.size());
        return submitTransfer(transferBuilder, tips);
    }

    BigInteger getBalanceOfAddress(String address) {
        BigInteger sum = initialBalances.getOrDefault(address, BigInteger.ZERO);
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

    Bundle getBundle(String bundleHead) {
        Transaction head = ixi.findTransactionByHash(bundleHead);
        return head == null ? null : new Bundle(head);
    }

    /****** HELPERS ******/

    private AutonomousEconomicActor findAutonomousActor(String address) {
        for(AutonomousEconomicActor actor : autonomousActors) {
            if(actor.getAddress().equals(address))
                return actor;
        }
        return null;
    }

    private TrustedEconomicActor findTrustedActor(String address) {
        for(TrustedEconomicActor actor : cluster.getActors()) {
            if(actor.getAddress().equals(address))
                return actor;
        }
        return null;
    }

    static List<String> deriveAddressesFromSeed(String seed, int amount) {
        List<String> addresses = new LinkedList<>();
        for(int i = 0; i < amount; i++)
            addresses.add(SignatureSchemeImplementation.deriveAddressFromSeed(seed, i, TRANSFER_SECURITY));
        return addresses;
    }

    private TransferBuilder buildTransfer(SignatureSchemeImplementation.PrivateKey privateKey, BigInteger balance, String receiverAddress, String remainderAddress, BigInteger value, boolean checkBalances, int amountOfTips) {
        if(balance.compareTo(value) < 0) {
            if(checkBalances)
                throw new IllegalArgumentException("insufficient balance (balance="+balance+" < value="+value+")");
            balance = value;
        }

        Set<InputBuilder> inputs = value.equals(BigInteger.ZERO) ? Collections.emptySet() : Collections.singleton( new InputBuilder(privateKey, BigInteger.ZERO.subtract(balance)));
        Set<OutputBuilder> outputs = new HashSet<>();
        outputs.add(new OutputBuilder(receiverAddress, value, "EC9RECEIVER"));
        if(!balance.equals(value))
            outputs.add(new OutputBuilder(remainderAddress, balance.subtract(value), "EC9REMAINDER"));

        while ((amountOfTips+1)/2 > inputs.size() * TRANSFER_SECURITY + outputs.size())
            outputs.add(new OutputBuilder(Trytes.NULL_HASH, BigInteger.ZERO, "JUST9HERE9FOR9THE9TIPS"));

        return new TransferBuilder(inputs, outputs, TRANSFER_SECURITY);
    }

    private Set<String> findTips(int amount) {
        Set<String> tips = new HashSet<>();
        for(int i = 0; i < amount; i++)
            tips.add(findTip());
        return tips;
    }

    private String findTip() {
        return Transaction.NULL_TRANSACTION.hash;
        //throw new RuntimeException("implement me");
    }

    /**
     * Builds a transfer and submits it to the Ict network.
     * @param transferBuilder Modeled transfer to submit.
     * @return Hash of bundle head of submitted bundle.
     * */
    private String submitTransfer(TransferBuilder transferBuilder, Collection<String> tips) {
        BundleBuilder bundleBuilder = transferBuilder.build();

        int tipNr = 0;
        for(String tip : tips) {
            TransactionBuilder transactionBuilder = bundleBuilder.getTailToHead().get(tipNr/2);
            if(tipNr%2 == 0)
                transactionBuilder.branchHash = tip;
            else
                transactionBuilder.trunkHash = tip;
            tipNr++;
        }

        Bundle bundle = bundleBuilder.build();
        for(Transaction transaction : bundle.getTransactions())
            ixi.submit(transaction);
        String bundleHead = bundle.getHead().hash;
        watchTransfer(bundleHead);
        return bundleHead;
    }

    public void changeInitialBalance(String address, BigInteger toAdd) {
        initialBalances.put(address, initialBalances.getOrDefault(address, BigInteger.ZERO).add(toAdd));
        for(AutonomousEconomicActor actor : autonomousActors) {
            actor.changeInitialBalance(address, toAdd);
        }
    }

    public void watchTransfer(String bundleHead) {
        transfers.add(bundleHead);
    }

    public void unwatchTransfer(String bundleHead) {
        transfers.remove(bundleHead);
    }

    /****** GETTERS *****/

    Ixi getIxi() {
        return ixi;
    }

    List<TrustedEconomicActor> getTrustedActors() {
        return cluster.getActors();
    }

    List<AutonomousEconomicActor> getAutonomousActors() {
        return new LinkedList<>(autonomousActors);
    }

    public List<String> getTransfers() {
        return new LinkedList<>(transfers);
    }
}