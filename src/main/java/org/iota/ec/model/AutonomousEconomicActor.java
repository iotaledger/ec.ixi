package org.iota.ec.model;

import org.iota.ec.util.SerializableAutoIndexableMerkleTree;
import org.iota.ict.ixi.Ixi;
import org.iota.ict.model.bundle.Bundle;
import org.iota.ict.model.transaction.Transaction;
import org.iota.ict.utils.crypto.AutoIndexedMerkleTree;

import java.math.BigInteger;
import java.util.*;

public class AutonomousEconomicActor extends ControlledEconomicActor {

    private final Ixi ixi;
    private final LedgerValidator ledgerValidator;
    private final Map<String, Double> publishedConfidenceByMarkedTangle = new HashMap<>();
    private Map.Entry<String, Double> mostConfident = null;
    private final EconomicCluster economicCluster;
    private final Set<String> validTangles = new HashSet<>();
    private final Set<String> invalidTangles = new HashSet<>();
    private double aggressivity = 1.1;

    public AutonomousEconomicActor(Ixi ixi, EconomicCluster economicCluster, Map<String, BigInteger> initialBalances, SerializableAutoIndexableMerkleTree merkleTree) {
        super(merkleTree);
        this.ixi = ixi;
        this.economicCluster = economicCluster;
        this.ledgerValidator = new LedgerValidator(ixi, initialBalances);
    }

    public void setAggressivity(double aggressivity) {
        this.aggressivity = aggressivity;
    }

    public void changeInitialBalance(String address, BigInteger toAdd) {
        ledgerValidator.changeInitialBalance(address, toAdd);
    }

    public void tick() {
        tick(Collections.emptySet());
    }

    public void tick(Collection<String> newTangles) {
        // TODO do not consider all which is too computationally expensive
        List<String> tangles = new LinkedList<>(economicCluster.getAllTangles());
        tangles.addAll(newTangles);
        removeInvalidTangles(tangles);
        ConfidenceCalculator confidenceCalculator = createConfidenceCalculator(tangles);
        Map<String, Double> newConfidenceByTangle = new HashMap<>();
        for(String tangle : tangles) {
            double calculatedConfidence = confidenceCalculator.confidenceOf(tangle);
            newConfidenceByTangle.put(tangle, calculatedConfidence);
        }

        mostConfident = null;
        for(Map.Entry<String, Double> entry : newConfidenceByTangle.entrySet()) {
            adjustConfidence(entry.getKey(), entry.getValue());
            if(mostConfident == null || entry.getValue() > mostConfident.getValue())
                mostConfident = entry;
        }
    }

    protected void removeInvalidTangles(List<String> tangles) {
        for(int i = 0; i < tangles.size(); i++) {
            String tangle = tangles.get(i);
            if(tangle.length() != Transaction.Field.BRANCH_HASH.tryteLength + Transaction.Field.TRUNK_HASH.tryteLength)
                throw new IllegalArgumentException("Not a tangle, nvalid length: " + tangle);
            if(!isTangleValid(tangle))
                tangles.remove(i--);
        }
    }

    public boolean isTangleValid(String tangle) {
        if(validTangles.contains(tangle))
            return true;
        if(invalidTangles.contains(tangle))
            return false;
        String ref1 = tangle.substring(0, 81);
        String ref2 = tangle.substring(81);
        boolean isValid = ledgerValidator.areTanglesCompatible(ref1, ref2);
        (isValid ? validTangles : invalidTangles).add(tangle);
        return isValid;
    }

    protected ConfidenceCalculator createConfidenceCalculator(List<String> tangles) {
        assert tangles.size() > 0;
        Set<ConfidenceCalculator.Conflict> conflicts = findAllConflicts(tangles);
        double[] initialProbabilities = new double[tangles.size()];
        for(int i = 0; i < tangles.size(); i++) {
            String tangle = tangles.get(i);
            initialProbabilities[i] = guessApprovalConfidence(tangle);
        }
        return new ConfidenceCalculator(tangles, conflicts, initialProbabilities);
    }

    protected double guessApprovalConfidence(String tangle) {
        String ref1 = tangle.substring(0, 81);
        String ref2 = tangle.substring(81);
        double confidenceRef1 = economicCluster.determineApprovalConfidence(ref1);
        double confidenceRef2 = economicCluster.determineApprovalConfidence(ref2);
        return (mostConfident != null && mostConfident.getKey().equals(tangle) ? 1+aggressivity : 1) * Math.min(confidenceRef1, confidenceRef2);
        // TODO improve
    }

    protected Set<ConfidenceCalculator.Conflict> findAllConflicts(List<String> tangles) {
        Set<ConfidenceCalculator.Conflict> conflicts = new HashSet<>();
        for(int i = 0; i < tangles.size(); i++) {
            String tangleI = tangles.get(i);
            for(int j = i+1; j < tangles.size(); j++) {
                String tangleJ = tangles.get(j);
                if(!ledgerValidator.areTanglesCompatible(tangleI.substring(0, 81), tangleI.substring(81), tangleJ.substring(0, 81), tangleJ.substring(81)))
                    conflicts.add(new ConfidenceCalculator.Conflict(tangleI, tangleJ));
            }
        }
        return conflicts;
    }

    protected void adjustConfidence(String tangle, double newConfidence) {
        System.err.println("adjusting confidence for " + tangle + " towards " + newConfidence);
        double oldConfidence = publishedConfidenceByMarkedTangle.getOrDefault(tangle, new Double(0));
        boolean shouldIssueNewMarker = !publishedConfidenceByMarkedTangle.containsKey(tangle) || shouldIssueMarkerToUpdateConfidence(oldConfidence, newConfidence);
        if(shouldIssueNewMarker) {
            double conservativeConfidence = oldConfidence + (newConfidence - oldConfidence) / 20.0;
            publishedConfidenceByMarkedTangle.put(tangle, conservativeConfidence);
            String trunk = tangle.substring(0, 81);
            String branch = tangle.substring(81);
            Bundle marker = buildMarker(trunk, branch, conservativeConfidence);
            for (Transaction t : marker.getTransactions())
                ixi.submit(t);
        }
    }

    private static boolean shouldIssueMarkerToUpdateConfidence(double currentConfidence, double newConfidence) {
        String currentEncodedConfidence = encodeConfidence(currentConfidence, Transaction.Field.TAG.tryteLength);
        String newEncodedConfidence = encodeConfidence(newConfidence, Transaction.Field.TAG.tryteLength);
        return !currentEncodedConfidence.equals(newEncodedConfidence);
    }
}
