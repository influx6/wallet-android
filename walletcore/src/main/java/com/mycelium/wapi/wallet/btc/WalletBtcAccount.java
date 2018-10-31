/*
 * Copyright 2013, 2014 Megion Research & Development GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mycelium.wapi.wallet.btc;

import com.google.common.base.Optional;
import com.mrd.bitlib.StandardTransactionBuilder;
import com.mrd.bitlib.StandardTransactionBuilder.InsufficientFundsException;
import com.mrd.bitlib.StandardTransactionBuilder.OutputTooSmallException;
import com.mrd.bitlib.UnsignedTransaction;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.NetworkParameters;
import com.mrd.bitlib.model.OutputList;
import com.mrd.bitlib.model.Transaction;
import com.mrd.bitlib.util.Sha256Hash;
import com.mycelium.wapi.model.BalanceSatoshis;
import com.mycelium.wapi.model.TransactionDetails;
import com.mycelium.wapi.model.TransactionEx;
import com.mycelium.wapi.model.TransactionOutputSummary;
import com.mycelium.wapi.model.TransactionSummary;
import com.mycelium.wapi.wallet.BroadcastResult;
import com.mycelium.wapi.wallet.KeyCipher;
import com.mycelium.wapi.wallet.KeyCipher.InvalidKeyCipher;
import com.mycelium.wapi.wallet.WalletAccount;
import com.mycelium.wapi.wallet.coins.Value;
import com.mycelium.wapi.wallet.currency.CurrencyBasedBalance;
import com.mycelium.wapi.wallet.currency.ExactCurrencyValue;

import java.util.List;
import java.util.UUID;

public interface WalletBtcAccount extends WalletAccount<BtcTransaction, BtcLegacyAddress> {

   /**
    * Get the network that this account is for.
    *
    * @return the network that this account is for.
    */
   NetworkParameters getNetwork();

   /**
    * Determine whether an address is one of our own addresses
    *
    * @param address the address to check
    * @return true iff this address is one of our own
    */
   boolean isMine(Address address);

   /**
    * Get the unique ID of this account
    */
   UUID getId();

   /**
    * Set whether this account is allowed to do zero confirmation spending
    * <p/>
    * Zero confirmation spending is disabled by default. When enabled then zero confirmation outputs send from address
    * not part of this account will be part of the spendable outputs.
    *
    * @param allowZeroConfSpending if true the account will allow zero confirmation spending
    */
   void setAllowZeroConfSpending(boolean allowZeroConfSpending);

   /**
    * Get the current receiving address of this account. Some accounts will
    * continuously use the same receiving address while others return new ones
    * as they get used.
    */
   Optional<Address> getReceivingAddress();

   /**
    * Get the current balance of this account based on the last synchronized
    * state.
    */
   BalanceSatoshis getBalance();

   CurrencyBasedBalance getCurrencyBasedBalance();

   /**
    * Get the transaction history of this account based on the last synchronized
    * state.
    *
    * @param offset the offset into the transaction history
    * @param limit  the maximum number of records to retrieve
    */
   List<TransactionSummary> getTransactionHistory(int offset, int limit);


   /**
    * Get the transaction history of this account since the stated timestamp
    * @param receivingSince only include tx older than this
    */
   List<TransactionSummary> getTransactionsSince(Long receivingSince);

   /**
    * Get the details of a transaction that originated from this account
    *
    * @param txid the ID of the transaction
    * @return the details of a transaction
    */
   TransactionDetails getTransactionDetails(Sha256Hash txid);

   /**
    * Broadcast a transaction
    * @param transaction the transaction to broadcast
    * @return the broadcast result
    */
   BroadcastResult broadcastTransaction(Transaction transaction);

   /**
    * Create a new unsigned transaction sending funds to one or more addresses.
    * <p/>
    * The unsigned transaction must be signed and queued before it will affect
    * the transaction history.
    * <p/>
    * If you call this method twice without signing and queuing the unsigned
    * transaction you are likely to create another unsigned transaction that
    * double spends the first one. In other words, if you call this method and
    * do not sign and queue the unspent transaction, then you should discard the
    * unsigned transaction.
    *
    * @param receivers the receiving address and amount to send
    * @return an unsigned transaction.
    * @throws StandardTransactionBuilder.OutputTooSmallException    if one of the outputs were too small
    * @throws StandardTransactionBuilder.InsufficientFundsException if not enough funds were present to create the unsigned
    *                                    transaction
    */
   UnsignedTransaction createUnsignedTransaction(List<WalletAccount.Receiver> receivers, long minerFeeToUse) throws StandardTransactionBuilder.OutputTooSmallException,
           StandardTransactionBuilder.InsufficientFundsException, StandardTransactionBuilder.UnableToBuildTransactionException;

   /**
    * Create a new unsigned transaction sending funds to one or more defined script outputs.
    * <p/>
    * The unsigned transaction must be signed and queued before it will affect
    * the transaction history.
    * <p/>
    * If you call this method twice without signing and queuing the unsigned
    * transaction you are likely to create another unsigned transaction that
    * double spends the first one. In other words, if you call this method and
    * do not sign and queue the unspent transaction, then you should discard the
    * unsigned transaction.
    *
    * @param outputs the receiving output (script and amount)
    * @param minerFeeToUse use this minerFee
    * @return an unsigned transaction.
    * @throws OutputTooSmallException    if one of the outputs were too small
    * @throws InsufficientFundsException if not enough funds were present to create the unsigned
    *                                    transaction
    */
   UnsignedTransaction createUnsignedTransaction(OutputList outputs, long minerFeeToUse) throws OutputTooSmallException,
           InsufficientFundsException, StandardTransactionBuilder.UnableToBuildTransactionException;

   /**
    * Sign an unsigned transaction without broadcasting it.
    *
    * @param unsigned     an unsigned transaction
    * @param cipher       the key cipher to use for decrypting the private key
    * @return the signed transaction.
    * @throws InvalidKeyCipher
    */
   Transaction signTransaction(UnsignedTransaction unsigned, KeyCipher cipher)
         throws InvalidKeyCipher;

   boolean broadcastOutgoingTransactions();

   /**
    * returns the transactionex for the hash from the accountBacking, if available
    * @param txid transaction hash
    * @return the corresponding transaction or null
    */
   TransactionEx getTransaction(Sha256Hash txid);
   /**
    * Determine whether the provided encryption key is valid for this wallet account.
    * <p/>
    * This function allows you to verify whether the user has entered the right encryption key for the wallet.
    *
    * @param cipher the encryption key to verify
    * @return true iff the encryption key is valid for this wallet account
    */
   boolean isValidEncryptionKey(KeyCipher cipher);

   /**
    * Get the summary list of unspent transaction outputs for this account.
    *
    * @return the summary list of unspent transaction outputs for this account.
    */
   List<TransactionOutputSummary> getUnspentTransactionOutputSummary();

   /**
    * Only sync this account if it is the active one
    * @return false if this account should always be synced
    */
   boolean onlySyncWhenActive();

   /**
    * Queue a transaction for broadcasting.
    * <p/>
    * The transaction is broadcast on next synchronization.
    *
    * @param transaction     an transaction
    */
   void queueTransaction(TransactionEx transaction);


   TransactionSummary getTransactionSummary(Sha256Hash txid);

   /**
    * Remove a pending outgoing tx from the queue
    *
    * A new synchronisation is needed afterwards, as we already purged some UTXOs as we saved the
    * tx in the queue
    *
    * @param transactionId     an transaction id
    */
   boolean cancelQueuedTransaction(Sha256Hash transactionId);

   /**
    * Delete a transaction from the accountBacking
    * Snyc is needed afterwards
    */
   boolean deleteTransaction(Sha256Hash transactionId);

   /**
    * Create a new unsigned Proof of Payment according to
    * <a href="https://github.com/bitcoin/bips/blob/master/bip-0120.mediawiki">BIP 120</a>.
    * @param txid The transaction id for the transaction to prove.
    * @param nonce The nonce, generated by the server requesting the PoP.
    * @return An UnsignedTransaction that represents the unsigned PoP.
    */
   UnsignedTransaction createUnsignedPop(Sha256Hash txid, byte[] nonce);

   /**
    * returns true if this is one of our already used or monitored external (=normal receiving) addresses
    */
   boolean isOwnExternalAddress(Address address);

   /**
    * returns true if this is one of our already used or monitored internal (="change") addresses
    */
   boolean isOwnInternalAddress(Address address);
}
