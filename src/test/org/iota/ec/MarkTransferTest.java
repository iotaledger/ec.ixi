package org.iota.ec;

import org.iota.ec.util.SerializableAutoIndexableMerkleTree;
import org.iota.ict.Ict;
import org.iota.ict.utils.Trytes;
import org.iota.ict.utils.properties.EditableProperties;
import org.junit.Test;

import java.math.BigInteger;
import java.util.LinkedList;

public class MarkTransferTest extends IctTestTemplate {

    @Test
    public void testMarkTransfer() {
        Ict ict = createIct(new EditableProperties());
        ECModule module = new ECModule(ict);

        String transfer = createTransfer(module);
        String actor = createActor(module);
        module.considerTangle(actor, transfer, transfer);
    }

    private static String createActor(ECModule module) {
        String actor = module.createNewActor(new SerializableAutoIndexableMerkleTree(random81Trytes(), 3, 2));
        module.setTrust(actor, 1);
        return actor;
    }

    private static String createTransfer(ECModule module) {
        BigInteger balance = BigInteger.valueOf(1000);
        BigInteger value = BigInteger.valueOf(400);
        int senderIndex = 0;

        String seed = random81Trytes();
        String senderAddress = ECModule.deriveAddressesFromSeed(seed, senderIndex+1).get(senderIndex);

        module.changeInitialBalance(senderAddress, balance);
        return module.sendTransfer(seed, senderIndex, random81Trytes(), random81Trytes(), value, true, new LinkedList<>());
    }

    private static String random81Trytes() {
        return Trytes.randomSequenceOfLength(81);
    }
}
