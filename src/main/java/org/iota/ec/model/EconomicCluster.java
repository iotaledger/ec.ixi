package org.iota.ec.model;

import org.iota.ict.eee.Environment;
import org.iota.ict.ixi.Ixi;
import org.iota.ict.model.bundle.Bundle;
import org.iota.ict.model.transaction.Transaction;
import org.iota.ict.network.gossip.GossipEvent;
import org.iota.ict.network.gossip.GossipFilter;
import org.iota.ict.network.gossip.GossipListener;
import org.iota.ict.utils.Constants;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class EconomicCluster implements GossipListener {

    private final Ixi ixi;
    private List<TrustedEconomicActor> actors = new LinkedList<>();
    private final ECGossipFilter filter = new ECGossipFilter();

    public EconomicCluster(Ixi ixi) {
        this.ixi = ixi;
        ixi.addListener(this);
    }

    public void addActor(TrustedEconomicActor actor, boolean sync) {
        if(filter.getWatchedAddresses().contains(actor.getAddress()))
            throw new IllegalArgumentException("Actor " + actor.getAddress() + " already added.");
        actors.add(actor);
        filter.watchAddress(actor.getAddress());

        if(sync) {
            Set<Transaction> possibleMarkers = ixi.findTransactionsByAddress(actor.getAddress());
            for(Transaction transaction : possibleMarkers) {
                if(!transaction.isBundleHead)
                    continue;
                Bundle possiblyMarker = new Bundle(transaction);
                if(!possiblyMarker.isStructureValid())
                    return;
                actor.processMarker(possiblyMarker);
            }
        }
    }

    public void removeActor(TrustedEconomicActor actor) {
        actors.remove(actor);
        filter.unwatchAddress(actor.getAddress());
    }

    public Set<String> getAllTangles() {
        Set<String> allTangles = new HashSet<>();
        for(TrustedEconomicActor actor : actors)
            allTangles.addAll(actor.getMarkedTangles().keySet());
        return allTangles;
    }

    public double determineApprovalConfidence(String transactionHash) {
        double maxAbsTrust = calcMaxAbsTrust();
        double absTrust = 0;
        for(TrustedEconomicActor actor : actors) {
            absTrust += actor.getTrust() * actor.getConfidence(transactionHash);
        }
        return maxAbsTrust > 0 ? absTrust / maxAbsTrust : 0;
    }

    private double calcMaxAbsTrust() {
        double trustSum = 0;
        for(TrustedEconomicActor actor : actors) {
            trustSum += actor.getTrust();
        }
        return trustSum;
    }

    @Override
    public void onReceive(GossipEvent event) {
        Transaction transaction = event.getTransaction();

        if(filter.passes(transaction)) {

            Bundle possiblyMarker = new Bundle(transaction);
            if(!possiblyMarker.isStructureValid())
                return;

            for(TrustedEconomicActor actor : actors) {
                if(actor.getAddress().equals(transaction.address()))
                    actor.processMarker(possiblyMarker);
            }
        }

        for(TrustedEconomicActor actor : actors) {
            actor.processTransaction(transaction);
        }
    }

    public LinkedList<TrustedEconomicActor> getActors() {
        return new LinkedList<>(actors);
    }

    @Override
    public Environment getEnvironment() {
        return Constants.Environments.GOSSIP;
    }

    private class ECGossipFilter extends GossipFilter {

        @Override
        public boolean passes(Transaction transaction) {
            return transaction.isBundleHead && super.passes(transaction);
        }
    }
}
