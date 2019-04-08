# Economic Clustering in the Tangle

# Abstract

**Economic Clustering (EC)** applies economic laws to **Distributed Ledger Technology (DLT)** in order to enable scalability in large networks. This document will limit the scope of Economic Clustering to the **Directed Acyclic Graph (DAG)** structure of the **IOTA Tangle**.

In DLT each node usually processes and stores a local copy of the entire ledger. Due to their global nature and increasing adoption, this approach will necessarily run into scaling issues. While memory is cheap, bandwidth and latency are the first expected to cause issues. Economic Clustering offers infinite scalability for the entire system while keeping the requirements for individual nodes low by splitting the global ledger into economically relevant clusters. The concept is backed by economic laws.

# Introduction

### Scalability in Real World Economics

Inspired by global real-world economy which does not suffer such limitations, EC tries to apply their concepts to DLT. The most important of these is the concept of **hierarchical locality**: the global economy (we will call it the **global level**) is a system composed of smaller national economies and the relations among them. Recursively, these national economies consist of many regional economies which are connected with each other. And once again, regional economies are a multitude of companies and individuals interacting with each-other within their regional economy. We will stop zooming in at this last level and consider it the **atomic level** (the elements of this level are the **atoms**) at which we cannot undertake further divisions.

It is important to note that the global level does not physically exist as a separate entity but is an abstract term describing the entire system. In fact, the system consists of billions of atoms interacting with each-other. While the global level is highly complex, no atom is fully aware of the entire picture but only follows their own self-interest and interacts mostly within a tiny horizon (for example a city) which we will call **cluster**. This is caused by the fact that the vast majority of economic activity is not relevant to an atom (you don't care whether someone on a distant continent is buying a coffee).

### Applying Locality to DLT

By splitting up the global ledger in smaller clusters, we can enable scalability while allowing the entire system to grow without limitations. Each node, representing an atom, can decide which cluster it considers economically relevant. It will then follow only the activity within the chosen cluster.

## Structure of EC

### Economic Cluster

An economic cluster is a set of nodes sharing the same ledger. Clusters are not compatible. One can only transact within a cluster. A real-world equivalent of an economic cluster is a currency. Within a nation a certain currency is accepted while other currencies must usually be exchanged to the national currency before use.

All tokens within an economic cluster are fungible and share the same value. This does not hold between tokens of different clusters. Just like different fiat currencies, they differ in value and the exchange rate is subject to market forces.

### Economic Actor

An economic actor is an entity operating within an economic cluster. Actors can be institutions, individuals or machines. An actor is relevant to us only if we care about how that actor sees the ledger.

We care directly if we depend on this actor. The value of our tokens only results from our ability to exchange these tokens for goods. If none of the actors we want to perform transactions with does recognize our tokens as valuable, these tokens are of no value to us either. For example, the supermarket around the corner would be directly relevant for us if we intend to buy groceries with our tokens there.

We care indirectly if an actor who is relevant to us heavily relies on another actor. For example, we might not plan to interact with the bank of our supermarket ourselves. However, that bank is certainly relevant to our supermarket and as such it is relevant to us as well, even though in a smaller degree.

### Neighbor Clusters

> Each cluster processes its own transactions and transactions of the neighbor clusters, all the other transactions are ignored because they simply don’t reach the cluster.

(to be continued ...)

# Economics of EC

## Inter-Cluster Token Exchange

Clusters are not compatible. However, just like in the real world where we have currency exchanges, nodes who have balances in both clusters can offer an exchange service between clusters. This service would (like the real-world equivalent) of course require a tiny fee to compensate the serving node.

Even though this service is provided by a third-party, practically no trust is required when realizing transacting through flash. A tiny micro-payment stream will flow between both nodes in both clusters. If the exchanging node turned out to be malicious and would not transfer the balance to the other cluster but keep it, only an insignificantly low amount of tokens would have been stolen.

## Token Creation

Clusters are equivalent to ledger forks. As such, there is no mechanism preventing someone from creating a new cluster with new tokens. Instead, Economic Clustering relies on economic laws to ensure that there is no damage caused by someone creating tokens out of thin air - which is possible at any time through forking anyways.

Although tokens can be created out of thin air, value cannot. The value of a cluster lies in its economic relevance. A cluster without relevance will have no value and therefore the tokens will have no value either. Funds in economic clusters will therefore closely resemble those of the real world. There is no way to avoid this, neither is there any use in doing so.

# Implementation

## Terminology

### Span-Tangle

A span-tangle `span(T)` of a transaction `T` (or a set of transactions) is the Tangle derived by looking only at this transaction and its entire history (all directly or indirectly referenced transactions). All other transactions are not part of this sub-tangle:

```
span(T1, T2, ... Tn) = span(T1) ⋃ span(T2) ⋃ ... ⋃ span(Tn)
span(T) = T ⋃ span(T.branch, T.trunk) = T ⋃ span(T.branch) ⋃ span(T.trunk)
```

### Referenced Tangle

The tangle referenced by a transaction `T` contains exactly those transactions which are directly or indirectly referenced by `T`.
It does not contain `T` itself.

```
T ... transaction
referenced(T) ... tangle referenced by T

referenced(T) = span(T) ∖ T
              = span(T.branch, T.trunk)
              = span(T.branch) ⋃ span(T.trunk)
  ```
  
### Sub- and Super-Tangle

A sub-tangle S' of a tangle S is the span-tangle of a transaction T in S. Thus S' is a subset of S. S will be called the super-tangle of S'.

```
S ... tangle (super-tangle of S')
T∈S ... transaction in S
S' = span(T); S' ⊆ S ... (sub-tangle of S)
```

## Confidence Markers

### Issuing

Each economic actor regularly issues confidence markers to publish its current view of the ledger. These markers are signed
bundles and reference a tangle. Additionally, they specify a probability representing how confident the actor is that the
referenced tangle is confirmed. This confidence will be updated/overwritten whenever the actor issues a new marker
referencing the same tangle.

```
M ... marker bundle
M.tail ... tail of M, last transaction in the bundle
referenced(M) ... tangle referenced by the marker M

referenced(M) = referenced(M.tail)
              = span(M.tail) ∖ M.tail
              = span(M.tail.branch, M.tail.trunk)
              = span(M.tail.branch) ⋃ span(M.tail.trunk)
```

### Consensus

Economic actors publish such markers and adjust the probabilities depending on the markers issued by other actors within their cluster. This dynamic and probabilistic voting process goes on until the cluster comes to consensus. This happens when the vast economically relevant majority of the cluster approves a certain sub-tangle with 100% confidence each. In case a conflicting sub-tangle was previously marked with a non-zero confidence, the respective actor would overwrite that confidence with 0% because it is not compatible with the accepted sub-tangle and no longer likely to be accepted by the cluster.

### Confidence

Actors will be more confident in a tangle the more other actors are confident in that same tangle. Therefore their confidence levels of a certain tangle quickly moves into the same direction (0% or 100%) which allows the cluster to come to consensus on a shared ledger.

### Cryptography

Each actor can be identified by an IOTA address. The confidence markers are implemented with merkle trees. This allows actors to issue multiple markers to the same address without compromising security despite the one-timeliness of the underlying Lamport signature scheme while using its advantages to stay quantum-secure.

### Priority

Since each actor can issue multiple markers, we need to determine which marker has the priority for a transaction T for which we want to determine the confidence.

**Overwriting:** The index of each signature within the merkle tree can be derived. To allow overwriting, a marker with a higher index will have priority over all markers with a lower index referencing the same tangle.

**Locality:** Let `S` be the referenced tangle of a marker `M` and `S'` be the referenced tangle of a marker `M'`. `T` is an element of `S'` and `S'` is a sub-tangle of `S`. As the more specific marker, `M'` has priority over the more general marker `M` for `T`. On a side note, the confidence in `S` cannot be higher than that of `S'` because the former includes the latter and as such relies on its confidence.

```
=== GIVEN ===
M, M' ... markers
S = referenced(M) ... tangle (super-tangle of S')
S' = referenced(M') ... tangle (sub-tangle of S)
T∈S' ... transaction in S'

== THEN ===
priority(M', T) ≥ priority(M, T)
S' ⊆ S ⇒ confidence(M') ≥ confidence(M)
```

## Cluster Confidence

The confidence of a transaction specifies how likely we consider that transaction confirmed. It depends on the confidence and relevance of all actors within our cluster. As such it is just as subjective as in the real world. Let `T` be a transaction. Let `A` be an economic actor. `p(T)` describes our cluster confidence (the confidence we have in `T`). `p(T|A)` is the confidence the actor `A` has in `T`. `relevance(A)` is the relative relevance of actor A (subjectively). The sum of the relevance over all actors must equal 1.

```
=== GIVEN ===
Actors ... set of all economic actors
T ... transaction
Σ[∀A∈Actors] relevance(A) = 1

=== THEN ===
p(T) = Σ[∀A∈Actors] relevance(A) ⋅ p(T|A)
```

## Ledger Validation

### Definition

Ledger validation describes the problem of determining whether a tangle complies to certain validity rules in which case
it is called "valid". Validity is a necessary requirement for confirmation. While confirmation is the result of consensus
and describes the acceptance of a tangle, validity is a technical property which can be checked deterministically and
describes the absence of inconsistencies within the tangle.

**Note that ledger validation is only done by Economic Actors. Weaker devices rely on their results so they don't have to
do perform these heavy computations themselves.**

In invalid ledger will have a confidence of zero. If the smallest merge of two tangles is invalid (e.g. because both include a
spend of the same funds - a *double spend*), we define these tangles as *conflicting*. They cannot be merged without
additional transfers and therefore we assume that at most one of both will confirm. Based on the conflict relation we can
model the probability of a Tangle confirming. This is the basis to calculate the confidence.

### General Validity Rules

A tangle `S` is valid if all transfers within are valid. A transfer `T` is valid **in a tangle `S`** if:
* the signatures of all inputs in `T` are valid
* no tokens are created or destroyed by `T` (input value = output value)
* when combining all transfers **in `S`**, no address being used as input for `T` is left with a negative balance (inputs exist)

The first two conditions can be checked efficiently for each transfer once without the context of S (complexity `O(1)` for each new transfer). The third condition
on the other hand must be validated from scratch for each tangle S (complexity `O(S)` for each ledger validation). In practice we will
keep a snapshot of all balances from a certain point of history we consider confirmed with high probability. This reduces the complexity
from validating the entire ledger since genesis to validating only the new part of the ledger since that snapshot.

# References

* [Economic Clustering and IOTA](https://medium.com/@comefrombeyond/economic-clustering-and-iota-d3a77388900) (Medium Article) by Come-from-Beyond on Jun 9, 2018
