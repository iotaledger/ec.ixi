# EC.ixi

## About

**Economic Clustering (EC)** is the consensus mechanism in the Ict network. For a more detailed description of the
mechanism, please see [this document](https://github.com/iotaledger/ict/blob/master/docs/EC.md). EC is not part of
the Ict core protocol but an optional extension. In this repoistory EC has been implemented as an [IXI](https://github.com/iotaledger/ixi)
module which you can install on your Ict node to extend it with the consensus mechanism. This allows you to come to
consensus with others on the confirmation state of transactions.

## State of Development

EC.ixi will be developed in iterations. This incremental approach allows to incorporate feedback during the development process. As such it should not be considered finished software. As of now, this module is in an early stage that would best be described as *Proof-of-Concept*. It can already be used for demonstration purposes and to gain a conceptual understanding of Economic Clustering. However, it requires more development until it can be used for practical consensus based applications. **The following features are still missing:**

**Clusters:**
* scalability in regards to estimating confidence based on many trusted actors
* ability to adjust trust based on actor behaviour

**Controlled Actors:**
* scalability in regards to calculating confidence based on many marked Tangles
* scalability in regards to ledger validation of large Tangles
* ability to work with incomplete (pruned) Tangles

## Installation

### Simple Installation (Download)

Make sure the version of your Ict node is `0.6` or higher. Otherwise you have to update it first.

1) Open the Web GUI of your Ict node. It is usually hosted on `http://{HOST}:2187`.
2) Visit the **MANAGE MODULES** tab and click on the **INSTALL THIRD PARTY MODULES** button.
3) Enter the repository `iotaledger/ec.ixi` and click on **INSTALL**.
4) After the module has been installed successfully, reload the page. In the **IXI MODULES** section you should now
find a new tab **EC.IXI**. Click on it to visit the EC web GUI.

### Advanced Installation (Building)

You will need **Git**, **NPM** and **Gradle**. Assuming that your ict directory is `~/Desktop/ict`:

```shell
# 1) Download the source code.
git clone https://github.com/iotaledger/ec.ixi
cd ec.ixi

# 2) Download the dependencies required for the EC.ixi web GUI.
cd web
npm install
cd ../

# 3) Build the .jar file and move it to your ict node's modules directory.
gradle ixi
mv ec.ixi-{VERSION}.jar ~/Desktop/ict/modules
```

## Using EC.ixi

### Step 1: Create your Cluster

- In the **Actors** section, create a new actor (or multiple). This section contains all actors controlled by you.
- Add the actor in your **Cluster** section with a positive trust value (e.g. `0.2` or `1`). This section contains all actors you base your confirmation on. It can include both actors you control as well as actors from other people.

### Step 2: Issue a new Transfer

- Enter a random tryte sequence as seed (**never use a real seed!**) in your **Wallet** section and generate addresses.
- Choose one of those addresses and change its initial balance (e.g. by 1000 iotas) in the **Cluster** section. This will give you some tokens to play around with. Note that these tokens will only be recognized by the actors controlled by you.
- In the **Wallet** section click on the **send** button and issue a new transfer.
- A new transfer should now appear in your **Transfer** section. When clicking on **status**, the confidence for each transaction should be `0`.

### Step 3: Send a Marker

- In **Actors**, click on the **issue** button of your actor.
- Enter the hash displayed in your **Transfer** section into the **branch** and the **trunk** field and click on **issue**.
- When clicking on the **markers** button of the same actor under **Cluster**, you should now see one entry with a confidence of `0.05`.
- Click on the **status** button of the transfer in **Transfers**, the confidence should now be positive. When going on **details**, you should see one entry with `0.05`.
- Keep clicking the **tick** button of any actors controlled by you. This should slowly let the confidence converge towards 1.

### Issuing a Double-Spend

- From section **Cluster** change the balance of one of your addresses A by creating tokens.
- In your **wallet** send a transfer spending all tokens from A to another address B.
- Send a second transfer from A to a third address C. You can do this even after the first transfer confirmed by disabling "check for available balance".

## Disclaimer

EC.ixi is a very early Proof-of-Concept and should under no circumstances be used for real value transfers or other
critical applications. Ict is a testnet and should be treated accordingly. IOTA Tokens in the Ict network are only
intended to play around with and do not hold any value. We are not responsible for any damage caused by running this
software. Use at your own risk
