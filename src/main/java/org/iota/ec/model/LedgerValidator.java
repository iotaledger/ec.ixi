package org.iota.ec.model;

import org.iota.ict.ixi.Ixi;
import org.iota.ict.model.bundle.Bundle;
import org.iota.ict.model.transaction.Transaction;
import org.iota.ict.model.transaction.TransactionBuilder;
import org.iota.ict.model.transfer.Transfer;

import java.math.BigInteger;
import java.util.*;

public class LedgerValidator {

    protected final Ixi ixi;

    protected final Map<String, BigInteger> initialBalances;
    protected final Map<String, String> dependencyByTransfer = new HashMap<>();
    protected final Map<String, RuntimeException> invalidTransfers = new HashMap<>();
    protected final Set<String> validTransfers = new HashSet<>();

    LedgerValidator(Ixi ixi) {
        this.ixi = ixi;
        validTransfers.add(Transaction.NULL_TRANSACTION.hash);
        initialBalances = new HashMap<>();
    }

    LedgerValidator(Ixi ixi, Map<String, BigInteger> initialBalances) {
        this.ixi = ixi;
        validTransfers.add(Transaction.NULL_TRANSACTION.hash);
        this.initialBalances = new HashMap<>(initialBalances);
    }

    public void changeInitialBalance(String address, BigInteger toAdd) {
        initialBalances.put(address, initialBalances.containsKey(address) ? initialBalances.get(address).add(toAdd) : toAdd);
    }

    public boolean areTanglesCompatible(String hashA, String hashB, String hashC, String hashD) {
        Transaction refA = ixi.findTransactionByHash(hashA);
        Transaction refB = ixi.findTransactionByHash(hashB);
        Transaction refC = ixi.findTransactionByHash(hashC);
        Transaction refD = ixi.findTransactionByHash(hashD);
        return isTangleSolid(merge(merge(refA, refB), merge(refC, refD)));
    }

    public boolean areTanglesCompatible(String hashA, String hashB) {
        Transaction refA = ixi.findTransactionByHash(hashA);
        Transaction refB = ixi.findTransactionByHash(hashB);
        if(refA == null) throw new IncompleteTangleException(hashA);
        if(refB == null) throw new IncompleteTangleException(hashB);
        return isTangleSolid(merge(refA, refB));
    }

    private Transaction merge(Transaction refA, Transaction refB) {
        TransactionBuilder builder = new TransactionBuilder();
        builder.trunkHash = refA.hash;
        builder.branchHash = refB.hash;
        Transaction merge = builder.build();
        merge.setTrunk(refA);
        merge.setBranch(refB);
        return merge;
    }

    public boolean isTangleSolid(String rootHash) {
        return isTangleSolid(ixi.findTransactionByHash(rootHash));
    }

    protected boolean isTangleSolid(Transaction root) {
        return isTangleValid(root) && noNegativeBalanceInTangle(root);
    }

    protected boolean noNegativeBalanceInTangle(Transaction root) {
        Map<String, BigInteger> balances = calcBalances(root);
        for (Map.Entry<String, BigInteger> entry : balances.entrySet()) {
            if(entry.getValue().compareTo(BigInteger.ZERO) < 0) {
                return false;
            }
        }
        return true;
    }

    protected Map<String, BigInteger> calcBalances(Transaction root) {

        Map<String, BigInteger> balances = new HashMap<>(initialBalances);
        LinkedList<Transaction> toTraverse = new LinkedList<>();
        Set<String> traversed = new HashSet<>();
        toTraverse.add(root);

        while (toTraverse.size() > 0) {
            Transaction current = toTraverse.poll();

            if(traversed.add(current.hash)) {
                if(!current.value.equals(BigInteger.ZERO)) {
                    String address = current.address();
                    balances.put(address, balances.containsKey(address) ? balances.get(address).add(current.value) : current.value);
                }

                Transaction branch = current.getBranch();
                Transaction trunk = current.getTrunk();
                if(branch == null || trunk == null)
                    throw new IncompleteTangleException(branch == null ? current.branchHash() : current.trunkHash());
                toTraverse.add(branch);
                toTraverse.add(trunk);
            }
        }

        return balances;
    }

    public boolean isTangleValid(String rootHash) {
        return isTangleValid(ixi.findTransactionByHash(rootHash));
    }

    protected boolean isTangleValid(Transaction root) {

        if(root == null)
            throw new NullPointerException("root is null");

        try {
            validateTangle(root.hash, root);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    protected void validateTangle(String rootHash) {
        validateTangle(rootHash, ixi.findTransactionByHash(rootHash));
    }

    protected void validateTangle(String rootHash, Transaction root) {

        if(root == null)
            throw new IncompleteTangleException(rootHash);
        if(validTransfers.contains(rootHash))
            return;
        RuntimeException exception = invalidTransfers.get(rootHash);
        if(exception != null)
            throw exception;

        checkForMissingDependency(root.hash);
        try {
            if (root.isBundleHead && !(root.isBundleTail && root.value.compareTo(BigInteger.ZERO) == 0)) {
                validateTransfer(root);
            }
            validateTangle(root.branchHash(), root.getBranch());
            validateTangle(root.trunkHash(), root.getTrunk());
            validTransfers.add(root.hash);
        } catch (IncompleteTangleException incompleteTangleException) {
            dependencyByTransfer.put(root.hash, incompleteTangleException.unavailableTransactionHash);
            throw incompleteTangleException;
        } catch (RuntimeException e) {
            invalidTransfers.put(root.hash, e);
            throw e;
        }
    }

    protected void validateTransfer(Transaction head) {
        Transfer transfer = new Transfer(new Bundle(head));
        if(!transfer.isValid()) {
            throw transfer.areSignaturesValid()
                    ? new InvalidTransferSumException(head.hash)
                    : new InvalidSignatureException(head.hash);
            // TODO make Transfer.isSumZero public in Ict repo
        }
    }

    protected void checkForMissingDependency(String rootHash) {
        if(dependencyByTransfer.containsKey(rootHash)) {
            String dependency = dependencyByTransfer.get(rootHash);
            if(ixi.findTransactionByHash(dependency) == null) {
                throw new IncompleteTangleException(dependency);
            } else {
                dependencyByTransfer.remove(rootHash);
            }
        }
    }

    protected static class InvalidTransferException extends RuntimeException {
        private final String headHash;

        InvalidTransferException(String headHash) {
            super("Invalid Transfer: '"+headHash);
            assert headHash.length() == Transaction.Field.TRUNK_HASH.tryteLength;
            this.headHash = headHash;
        }
    }

    protected static class InvalidSignatureException extends InvalidTransferException {
        InvalidSignatureException(String headHash) {
            super(headHash);
        }
    }

    protected static class InvalidTransferSumException extends InvalidTransferException {
        InvalidTransferSumException(String headHash) {
            super(headHash);
        }
    }

    protected static class IncompleteTangleException extends RuntimeException {
        protected final String unavailableTransactionHash;

        IncompleteTangleException(String unavailableTransactionHash) {
            super("Missing transaction: '"+unavailableTransactionHash+"'");
            assert unavailableTransactionHash.length() == Transaction.Field.TRUNK_HASH.tryteLength;
            this.unavailableTransactionHash = unavailableTransactionHash;
        }
    }
}
