# EC.ixi

<img src="https://raw.githubusercontent.com/iotaledger/ec.ixi/master/docs/assets/gui.png" />

## About

**Economic Clustering (EC)** is the consensus mechanism in the Ict network. For a more detailed description of the
mechanism, please see [this document](https://github.com/iotaledger/ec.ixi/blob/master/docs/EC.md). EC is not part of
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

[> GUIDE](https://github.com/iotaledger/ec.ixi/tree/master/docs/USING.md)

## Disclaimer

EC.ixi is a very early Proof-of-Concept and should under no circumstances be used for real value transfers or other
critical applications. Ict is a testnet and should be treated accordingly. IOTA Tokens in the Ict network are only
intended to play around with and do not hold any value. We are not responsible for any damage caused by running this
software. Use at your own risk
