package org.iota.ec.model;

import org.iota.ec.IctTestTemplate;
import org.iota.ec.util.SerializableAutoIndexableMerkleTree;
import org.iota.ict.Ict;
import org.iota.ict.utils.Trytes;
import org.iota.ict.utils.crypto.SignatureSchemeImplementation;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class AutonomousEconomicActorTest extends IctTestTemplate {

    @Test
    public void test() {

        Ict ict = createIct();
        EconomicCluster cluster = new EconomicCluster(ict);

        AutonomousEconomicActor underTest = new AutonomousEconomicActor(ict, cluster, new HashMap<String, BigInteger>(), randomMerkleTree(3));
        ControlledEconomicActor otherA = new ControlledEconomicActor(randomMerkleTree(3));
        ControlledEconomicActor otherB = new ControlledEconomicActor(randomMerkleTree(3));

        cluster.addActor(new TrustedEconomicActor(underTest.getAddress(), 0.5), false);
        cluster.addActor(new TrustedEconomicActor(otherA.getAddress(), 0.3), false);
        cluster.addActor(new TrustedEconomicActor(otherB.getAddress(), 0.2), false);

        SignatureSchemeImplementation.PrivateKey key1 = SignatureSchemeImplementation.derivePrivateKeyFromSeed(Trytes.randomSequenceOfLength(81), 0, 1);
        SignatureSchemeImplementation.PrivateKey key2 = SignatureSchemeImplementation.derivePrivateKeyFromSeed(Trytes.randomSequenceOfLength(81), 0, 1);
        BigInteger value = BigInteger.valueOf(10);

        String transfer1 = submitBundle(ict, buildValidTransfer(key1, value, key2.deriveAddress(), Collections.<String>emptySet()));
        String transfer2 = submitBundle(ict, buildValidTransfer(key2, value, key1.deriveAddress(), Collections.singleton(transfer1)));

        assertConfidence(cluster, transfer1, 0);
        assertConfidence(cluster, transfer2, 0);

        submitBundle(ict, otherA.buildMarker(transfer1, transfer1, 0.05));
        saveSleep(50);

        assertConfidence(cluster, transfer1, 0.3*0.05);

        // transfer1 is not valid
        underTest.tick();
        saveSleep(100);
        assertConfidence(cluster, transfer1, 0.5*0 + 0.3*0.05);


        submitBundle(ict, otherB.buildMarker(transfer2, transfer2, 0.07));
        saveSleep(50);
        assertConfidence(cluster, transfer2, 0.2*0.07);

        underTest.tick();
        saveSleep(100);
        assertConfidenceInterval(cluster, transfer1, 0.5*0.01 + 0.3*0.05 + 0.2*0.07, 1);
        assertConfidenceInterval(cluster, transfer2, 0.5*0.01 + 0.3*0.05, 0.5*1 + 0.3*1);
    }

    @Test
    public void testConvergenceConflicting() {

        Ict ict = createIct();
        EconomicCluster cluster = new EconomicCluster(ict);

        SignatureSchemeImplementation.PrivateKey key = SignatureSchemeImplementation.derivePrivateKeyFromSeed(Trytes.randomSequenceOfLength(81), 0, 1);
        BigInteger value = BigInteger.valueOf(10);

        Map<String, BigInteger> initialBalances = new HashMap<>();
        initialBalances.put(key.deriveAddress(), value);

        AutonomousEconomicActor auto1 = new AutonomousEconomicActor(ict, cluster, initialBalances, randomMerkleTree(5));
        AutonomousEconomicActor auto2 = new AutonomousEconomicActor(ict, cluster, initialBalances, randomMerkleTree(5));
        AutonomousEconomicActor auto3 = new AutonomousEconomicActor(ict, cluster, initialBalances, randomMerkleTree(5));

        cluster.addActor(new TrustedEconomicActor(auto1.getAddress(), 0.5), false);
        cluster.addActor(new TrustedEconomicActor(auto2.getAddress(), 0.3), false);
        cluster.addActor(new TrustedEconomicActor(auto3.getAddress(), 0.2), false);

        String transfer1 = submitBundle(ict, buildValidTransfer(key, value, Trytes.randomSequenceOfLength(81), Collections.<String>emptySet()));
        String transfer2 = submitBundle(ict, buildValidTransfer(key, value, Trytes.randomSequenceOfLength(81), Collections.<String>emptySet()));

        assertConfidence(cluster, transfer1, 0);
        assertConfidence(cluster, transfer2, 0);

        // TODO fix: does not work with high confidence because auto1 will have mostConfidentTangle == null
        submitBundle(ict, auto2.buildMarker(transfer1, transfer1, 0.05));
        submitBundle(ict, auto3.buildMarker(transfer2, transfer2, 0.05));
        saveSleep(50);

        double lastConfidence1 = assertConfidence(cluster, transfer1, 0.3*0.05);

        auto1.setAggressivity(0.1);
        auto2.setAggressivity(0.1);
        auto3.setAggressivity(0.1);

        for(int iteration = 0; iteration < 10; iteration++) {
            auto1.tick();
            auto2.tick();
            auto3.tick();
            saveSleep(50);

            lastConfidence1 = assertConfidenceInterval(cluster, transfer1, lastConfidence1+0.01, 1.01);
            assertConfidenceInterval(cluster, transfer2, -0.01, 1-lastConfidence1);
        }
    }

    @Test
    public void testConvergence2() {

        Ict ict = createIct();
        EconomicCluster cluster = new EconomicCluster(ict);

        SignatureSchemeImplementation.PrivateKey key = SignatureSchemeImplementation.derivePrivateKeyFromSeed(Trytes.randomSequenceOfLength(81), 0, 1);
        BigInteger value = BigInteger.valueOf(10);

        Map<String, BigInteger> initialBalances = new HashMap<>();
        initialBalances.put(key.deriveAddress(), value);

        AutonomousEconomicActor auto1 = new AutonomousEconomicActor(ict, cluster, initialBalances, randomMerkleTree(5));
        AutonomousEconomicActor auto2 = new AutonomousEconomicActor(ict, cluster, initialBalances, randomMerkleTree(5));

        cluster.addActor(new TrustedEconomicActor(auto1.getAddress(), 0.4), false);
        cluster.addActor(new TrustedEconomicActor(auto2.getAddress(), 0.6), false);

        String transfer = submitBundle(ict, buildValidTransfer(key, value, Trytes.randomSequenceOfLength(81), Collections.<String>emptySet()));

        assertConfidence(cluster, transfer, 0);

        auto1.tick(Collections.singleton(transfer+transfer));
        saveSleep(50);

        auto1.setAggressivity(2);
        auto2.setAggressivity(2);

        double lastConfidence = 0;

        for(int iteration = 0; iteration < 5; iteration++) {
            auto1.tick();
            auto2.tick();
            saveSleep(50);
            lastConfidence = assertConfidenceInterval(cluster, transfer, lastConfidence + 0.03, 1.01);
        }
    }

    private static double assertConfidenceInterval(EconomicCluster cluster, String transaction, double expectedMin, double expectedMax) {
        double actual = cluster.determineApprovalConfidence(transaction);
        Assert.assertTrue("Unexpected confidence of " + transaction + " ("+actual+" <= "+expectedMin+")", actual > expectedMin);
        Assert.assertTrue("Unexpected confidence of " + transaction + " ("+actual+" >= "+expectedMax+")", actual < expectedMax);
        return actual;
    }

    private static double assertConfidence(EconomicCluster cluster, String transaction, double expected) {
        double actual = cluster.determineApprovalConfidence(transaction);
        Assert.assertEquals("Unexpected confidence of " + transaction, expected, actual, 1E-2);
        return actual;
    }

    private SerializableAutoIndexableMerkleTree randomMerkleTree(int depth) {
        return new SerializableAutoIndexableMerkleTree(Trytes.randomSequenceOfLength(81), 3, depth);
    }
}