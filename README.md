# EC.ixi

## About

**Economic Clustering (EC)** is the consensus mechanism in the Ict network. For a more detailed description of the
mechanism, please see [this document](https://github.com/iotaledger/ict/blob/master/docs/EC.md). EC is not part of
the Ict core protocol but an optional extension. In this repoistory EC has been implemented as an [IXI](https://github.com/iotaledger/ixi)
module which you can install on your Ict node to extend it with the consensus mechanism. This allows you to come to
consensus with others on the confirmation state of transactions.

## Installation

Make sure the version of your Ict node is `0.6` or higher. Otherwise you have to update it first.

1) Open the Web GUI of your Ict node. It is usually hosted on `http://{HOST}:2187`.
2) Visit the **MANAGE MODULES** tab and click on the **INSTALL THIRD PARTY MODULES** button.
3) Enter the repository `iotaledger/ec.ixi` and click on **INSTALL**.
4) After the module has been installed successfully, reload the page. In the **IXI MODULES** section you should now
find a new tab **EC.IXI**. Click on it to visit the EC web GUI.

## Usage

```java
// TODO
```

## Disclaimer

EC.ixi is a very early Proof-of-Concept and should under no circumstances be used for real value transfers or other
critical applications. Ict is a testnet and should be treated accordingly. IOTA Tokens in the Ict network are only
intended to play around with and do not hold any value. We are not responsible for any damage caused by running this
software. Use at your own risk
