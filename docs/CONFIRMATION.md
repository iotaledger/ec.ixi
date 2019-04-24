# Confirmation

## General Definition

A value transfer is called *confirmed* when the funds are accepted by the recipient. Confirmation therefore requires that the receiver
can spend these funds since otherwise they would not have any value. The original transfer confirms once the recipient can issue a new
transfer (spending the received funds by sending them to someone else) which is able to confirm as well.

When designing a mechanism for confirmation, we also want to forbid double spends in order to prevent entities from inflating the supply,
effectively decreasing the funds of others.

## Confirmation in EC

We consider a transaction confirmed in a cluster if almost all recent markers of almost all actors (weighted by their relevance) approves
that transaction either directly or indirectly. A marker approves all transactions in its depth-limited past cone **directly**. A marker
M<sub>1</sub> of some actor approves a transaction T **indirectly**, if there exists a chain of markers (M<sub>1</sub>, M<sub>2</sub>, ...,
M<sub>n</sub>) such that all markers in the chain belong to the actor and each marker M<sub>i</sub> directly approves its successor marker
M<sub>i+1</sub> (for all integers i with 0<i<n) and the last marker M<sub>n</sub> directly approves the transaction T.

The idea of this mechanism is to have each marker validate a subset of the Tangle instead of the entire Tangle. This enables
a high grade of scalability because (in contrast to traditional blockchain technologies) this approach does not require every node to
have access to the full ledger history.

## Validation

Each marker expresses that the issuing actor has validated the past cone to conclude that the cone could confirm based on the current
ledger state.

An actor will only approve a past cone if it approves every single transfer included in that past cone. A transfer will be naturally
approved if it has already confirmed in the cluster. All other transfers will only be approved if they comply to all three conditions:
* **authenticated spends** (the signatures of all inputs are valid)
* **consistent changes** (no tokens are created or destroyed: sum of the values of all transactions in the transfer is 0)
* **existent funds** (inputs can be derived from confirmed transfers, without any double spends within the cone)

The first two conditions can be validated for each transfer individually with complexity `O(1)` as soon as they are received. The third
condition can only be checked in the context of some past cone with complexity `O(N)` where `N` is the amount of transfers in the cone.

### Validating the Existence of Inputs

The bottom of the double cone will contain a set of transfers which are already confirmed. We do not question the inputs of these
transfers because they must have been validated before by other markers, otherwise they would not have been confirmed. However, we
recreate a balance snapshot from these transfers by accumulating the values of all transactions to their respective addresses. This leaves
us with a confirmed ledger `L` state which contains the funds that can be spent by the other (unconfirmed) transfers.

We then accumulate those other transfers to end up with a resulting ledger state `L'`. To decide whether this transfer is valid, we iterate
over all addresses `a` with negative balances (`L'[a] < 0`). If any of these balances is less than the confirmed ledger (`L'[a] < L[a]`
and `L'[a] < 0` which is equivalent to `L'[a] < min(0, L[a])`), non-existent funds were spent and the ledger is invalid.

## Double Spends

We now show that this mechanism will indeed prevent double spends. A double spend happens when we spend the same input twice and both
spends confirm. If this was possible, we could spend more tokens than we actually had.

Based on our definition of confirmation, each of both transfers `T1` and `T2` would have to be referenced by almost all markers of
almost all actors. Assuming that *"almost all"* is realized by some high percentage (e.g. 97%) that is definitely above 50%,
there must be at least one marker for almost every actor that approves both T1 and T2. We now show that no actor complying with the
proposed mechanism would issue such a marker, so that there can only be double spends if *"almost all"* actors are dishonest (e.g. less
the 3% honest nodes).

By definition, for an actor to have any markers reference a transfer directly or indirectly, there must be at least one marker of that
actor referencing the transfer directly. So if there is some marker referencing both T1 and T2, there must be at least one marker M1
referencing T1 and one marker M2 referencing T2. M1 and M2 cannot be identical because both T1 and T2 would be part of the past cone and
would be recognized as spending of non-existing funds due to an invalid resulting ledger `L'` state (see [here](#validating-the-existence-of-inputs)).
Hence validation would fail and no such marker would be issued.

So since there cannot be a marker referencing both T1 and T2 directly, a marker would have to reference T1 or T2 indirectly to approve the
double spend. Let T1 be the transfer that is referenced indirectly and T2 be referenced directly or indirectly.

...
