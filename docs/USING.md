# Using EC.ixi

## Step 1: Create your Cluster

In the **Actors** section, create a new actor (or multiple). This section contains all actors controlled by you.
<img src="https://raw.githubusercontent.com/iotaledger/ec.ixi/master/docs/assets/tut0_create_actor.PNG" />

The actor should automatically be added to your **Cluster** section. This section contains all actors you base your
confirmation on. It can include both actors you control as well as actors from other people.
<img src="https://raw.githubusercontent.com/iotaledger/ec.ixi/master/docs/assets/tut0_create_actor2.PNG" />

## Step 2: Issue a new Transfer

Enter a random tryte sequence as seed (**never use a real seed!**) in your **Wallet** section and generate addresses.
<img src="https://raw.githubusercontent.com/iotaledger/ec.ixi/master/docs/assets/tut1_gen_wallet.PNG" />

Choose one of those addresses and change its initial balance (e.g. by 1000 iotas) in the **Cluster** section. You can copy addresses (and all other hashes) to your clipboard simply by clicking on it. This will give you some tokens to play around with. Note that these tokens will only be recognized by the actors controlled by you.
<img src="https://raw.githubusercontent.com/iotaledger/ec.ixi/master/docs/assets/tut2_change_balance.PNG" />
<img src="https://raw.githubusercontent.com/iotaledger/ec.ixi/master/docs/assets/tut2_change_balance2.PNG" />

In the **Wallet** section click on the **send** button to issue a new transfer.
<img src="https://raw.githubusercontent.com/iotaledger/ec.ixi/master/docs/assets/tut3_issue_transfer.PNG" />

A new transfer should now appear in your **Transfer** section.
<img src="https://raw.githubusercontent.com/iotaledger/ec.ixi/master/docs/assets/tut3_issue_transfer2.PNG" />

When clicking on **status**, the confidence for each transaction should be `0`.
<img src="https://raw.githubusercontent.com/iotaledger/ec.ixi/master/docs/assets/tut3_issue_transfer3.PNG" />

## Step 3: Send a Marker

In **Actors**, click on the **issue** button of your actor. Enter the hash displayed in your **Transfer** section into the **reference #1* field and click on **issue**.
<img src="https://raw.githubusercontent.com/iotaledger/ec.ixi/master/docs/assets/tut4_issue_marker.PNG" />

When clicking on the **markers** button of the same actor under **Cluster**, you should now see one entry with a confidence of something like `0.05`.
<img src="https://raw.githubusercontent.com/iotaledger/ec.ixi/master/docs/assets/tut4_issue_marker3.PNG" />

Click on the **status** button of the transfer in **Transfers**, the confidence should now be positive. When going on **details**, you should see one entry with `0.05`.
<img src="https://raw.githubusercontent.com/iotaledger/ec.ixi/master/docs/assets/tut4_issue_marker2.PNG" />

Keep clicking the **tick** button of actors controlled by you. This should slowly let the confidence converge towards 1.
<img src="https://raw.githubusercontent.com/iotaledger/ec.ixi/master/docs/assets/tut5_tick.PNG" />
<img src="https://raw.githubusercontent.com/iotaledger/ec.ixi/master/docs/assets/tut5_tick2.PNG" />
<img src="https://raw.githubusercontent.com/iotaledger/ec.ixi/master/docs/assets/tut5_tick3.PNG" />

## Issuing a Double-Spend

* From section **Cluster** change the balance of one of your addresses A by creating tokens.
* In your **wallet** send a transfer spending all tokens from A to another address B.
* Send a second transfer from A to a third address C. You can do this even after the first transfer confirmed by disabling "check for available balance".
