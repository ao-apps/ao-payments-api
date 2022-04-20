/*
 * ao-payments-api - Payment processing API supporting multiple payment gateways.
 * Copyright (C) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2015, 2016, 2019, 2020, 2021, 2022  AO Industries, Inc.
 *     support@aoindustries.com
 *     7262 Bull Pen Cir
 *     Mobile, AL 36695
 *
 * This file is part of ao-payments-api.
 *
 * ao-payments-api is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ao-payments-api is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with ao-payments-api.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.aoapps.payments;

import com.aoapps.lang.LocalizedIllegalArgumentException;
import com.aoapps.lang.sql.LocalizedSQLException;
import static com.aoapps.payments.Resources.PACKAGE_RESOURCES;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.Principal;
import java.security.acl.Group;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.NotImplementedException;

/**
 * Processes credit card payments with pluggable merchant services providers and persistence mechanisms.
 *
 * TODO: Age check methods
 *
 * TODO: Make sure that no calls result in cross-provider data, like a card stored with on provider
 *       being used for transactions on another.
 *
 * TODO: Provide batch close calls?
 *
 * @author  AO Industries, Inc.
 */
public class CreditCardProcessor {

  protected final MerchantServicesProvider provider;
  protected final PersistenceMechanism persistenceMechanism;

  public CreditCardProcessor(MerchantServicesProvider provider, PersistenceMechanism persistenceMechanism) {
    this.provider = provider;
    this.persistenceMechanism = persistenceMechanism;
  }

  /**
   * Gets the uniqueId of the provider this processor is using.
   */
  public String getProviderId() {
    return provider.getProviderId();
  }

  /**
   * Performs an immediate sale, effectively a combination of <code>authorize</code> and <code>capture</code>.
   * The transaction is inserted into the persistence layer first in a PROCESSING state,
   * the provider performs the sale, and then the persistence layer transaction state is changed to the appropriate
   * final state.  Thus, any problem or restart will not lose record of the transaction, and the PROCESSING
   * state transaction may be manually resolved.
   *
   * @param  principal  <code>null</code> is acceptable
   * @param  group      <code>null</code> is acceptable
   * @param  transactionRequest  The transaction details for this sale
   * @param  creditCard  The credit card to charge for this sale.
   *                     The masked card number and/or expiration might be updated during the sale, and if updated
   *                     the changes will have already been persisted.
   *
   * @see  #authorize(java.security.Principal, java.security.acl.Group, com.aoapps.payments.TransactionRequest, com.aoapps.payments.CreditCard)
   * @see  #capture(java.security.Principal, com.aoapps.payments.Transaction)
   */
  public Transaction sale(Principal principal, Group group, TransactionRequest transactionRequest, CreditCard creditCard) throws SQLException {
    // Insert into persistence layer
    long currentTimeMillis = System.currentTimeMillis();
    Transaction transaction = new Transaction(
      provider.getProviderId(),
      null, // persistenceUniqueId
      group == null ? null : group.getName(),
      transactionRequest,
      creditCard,
      currentTimeMillis,
      principal == null ? null : principal.getName(),
      null, // authorizationResult
      currentTimeMillis,
      principal == null ? null : principal.getName(),
      null, // captureResult
      -1, // voidTime
      null, // voidPrincipalName
      null, // voidResult
      Transaction.Status.PROCESSING
    );
    String persistenceUniqueId = persistenceMechanism.insertTransaction(principal, group, transaction);
    transaction.setPersistenceUniqueId(persistenceUniqueId);

    // Perform sale
    SaleResult saleResult = provider.sale(transactionRequest, creditCard);
    long completedTimeMillis = System.currentTimeMillis();
    AuthorizationResult authorizationResult = saleResult.getAuthorizationResult();
    transaction.setAuthorizationResult(authorizationResult);
    transaction.setCaptureTime(completedTimeMillis);
    transaction.setCapturePrincipalName(principal == null ? null : principal.getName());
    transaction.setCaptureResult(saleResult.getCaptureResult());
    Transaction.Status status;
    switch (authorizationResult.getCommunicationResult()) {
      case LOCAL_ERROR:
        status = Transaction.Status.LOCAL_ERROR;
        break;
      case IO_ERROR:
        status = Transaction.Status.IO_ERROR;
        break;
      case GATEWAY_ERROR:
        status = Transaction.Status.GATEWAY_ERROR;
        break;
      case SUCCESS:
        switch (authorizationResult.getApprovalResult()) {
          case APPROVED:
            status = Transaction.Status.CAPTURED;
            break;
          case DECLINED:
            status = Transaction.Status.DECLINED;
            break;
          case HOLD:
            status = Transaction.Status.HOLD;
            break;
          default:
            throw new LocalizedSQLException("23000", PACKAGE_RESOURCES, "CreditCardProcessor.sale.unexpectedApprovalResult", authorizationResult.getApprovalResult());
        }
        break;
      default:
        throw new LocalizedSQLException("23000", PACKAGE_RESOURCES, "CreditCardProcessor.sale.unexpectedCommunicationResult", authorizationResult.getCommunicationResult());
    }
    transaction.setStatus(status);

    // Update persistence layer
    if (creditCard.getPersistenceUniqueId() != null) {
      TokenizedCreditCard tokenizedCreditCard = authorizationResult.getTokenizedCreditCard();
      if (tokenizedCreditCard != null) {
        String replacementMaskedCardNumber = tokenizedCreditCard.getReplacementMaskedCardNumber();
        if (replacementMaskedCardNumber != null) {
          creditCard.setMaskedCardNumber(replacementMaskedCardNumber);
          persistenceMechanism.updateCreditCard(principal, creditCard);
        }
        Byte replacementExpirationMonth = tokenizedCreditCard.getReplacementExpirationMonth();
        Short replacementExpirationYear = tokenizedCreditCard.getReplacementExpirationYear();
        if (replacementExpirationMonth != null && replacementExpirationYear != null) {
          creditCard.setExpirationMonth(replacementExpirationMonth);
          creditCard.setExpirationYear(replacementExpirationYear);
          persistenceMechanism.updateExpiration(
            principal,
            creditCard,
            replacementExpirationMonth,
            replacementExpirationYear
          );
        }
      }
    }
    persistenceMechanism.saleCompleted(
      principal,
      transaction
    );

    return transaction;
  }

  /**
   * Authorizes a sale.  The funds are reserved but not captured until a later call to capture.
   * The transaction is inserted into the persistence layer first in a PROCESSING state,
   * the provider performs the authorization, and then the persistence layer transaction state is changed to the appropriate
   * final state.  Thus, any problem or restart will not lose record of the transaction, and the PROCESSING
   * state transaction may be manually resolved.
   *
   * @param  principal  <code>null</code> is acceptable
   * @param  group      <code>null</code> is acceptable
   * @param  transactionRequest  The transaction details for this sale
   * @param  creditCard  The credit card to charge for this sale.
   *                     The masked card number and/or expiration might be updated during the sale, and if updated
   *                     the changes will have already been persisted.
   *
   * @see  #capture(java.security.Principal, com.aoapps.payments.Transaction)
   * @see  #voidTransaction(java.security.Principal, com.aoapps.payments.Transaction)
   */
  public Transaction authorize(Principal principal, Group group, TransactionRequest transactionRequest, CreditCard creditCard) throws SQLException {
    // Insert into persistence layer
    long currentTimeMillis = System.currentTimeMillis();
    Transaction transaction = new Transaction(
      provider.getProviderId(),
      null, // persistenceUniqueId
      group == null ? null : group.getName(),
      transactionRequest,
      creditCard,
      currentTimeMillis,
      principal == null ? null : principal.getName(),
      null, // authorizationResult
      -1, // captureTime
      null, // capturePrincipalName
      null, // captureResult
      -1, // voidTime
      null, // voidPrincipalName
      null, // voidResult
      Transaction.Status.PROCESSING
    );
    String persistenceUniqueId = persistenceMechanism.insertTransaction(principal, group, transaction);
    transaction.setPersistenceUniqueId(persistenceUniqueId);

    // Perform authorization
    AuthorizationResult authorizationResult = provider.authorize(transactionRequest, creditCard);
    transaction.setAuthorizationResult(authorizationResult);
    Transaction.Status status;
    switch (authorizationResult.getCommunicationResult()) {
      case LOCAL_ERROR:
        status = Transaction.Status.LOCAL_ERROR;
        break;
      case IO_ERROR:
        status = Transaction.Status.IO_ERROR;
        break;
      case GATEWAY_ERROR:
        status = Transaction.Status.GATEWAY_ERROR;
        break;
      case SUCCESS:
        switch (authorizationResult.getApprovalResult()) {
          case APPROVED:
            status = Transaction.Status.AUTHORIZED;
            break;
          case DECLINED:
            status = Transaction.Status.DECLINED;
            break;
          case HOLD:
            status = Transaction.Status.HOLD;
            break;
          default:
            throw new LocalizedSQLException("23000", PACKAGE_RESOURCES, "CreditCardProcessor.sale.unexpectedApprovalResult", authorizationResult.getApprovalResult());
        }
        break;
      default:
        throw new LocalizedSQLException("23000", PACKAGE_RESOURCES, "CreditCardProcessor.sale.unexpectedCommunicationResult", authorizationResult.getCommunicationResult());
    }
    transaction.setStatus(status);

    // Update persistence layer
    if (creditCard.getPersistenceUniqueId() != null) {
      TokenizedCreditCard tokenizedCreditCard = authorizationResult.getTokenizedCreditCard();
      if (tokenizedCreditCard != null) {
        String replacementMaskedCardNumber = tokenizedCreditCard.getReplacementMaskedCardNumber();
        if (replacementMaskedCardNumber != null) {
          creditCard.setMaskedCardNumber(replacementMaskedCardNumber);
          persistenceMechanism.updateCreditCard(principal, creditCard);
        }
        Byte replacementExpirationMonth = tokenizedCreditCard.getReplacementExpirationMonth();
        Short replacementExpirationYear = tokenizedCreditCard.getReplacementExpirationYear();
        if (replacementExpirationMonth != null && replacementExpirationYear != null) {
          creditCard.setExpirationMonth(replacementExpirationMonth);
          creditCard.setExpirationYear(replacementExpirationYear);
          persistenceMechanism.updateExpiration(
            principal,
            creditCard,
            replacementExpirationMonth,
            replacementExpirationYear
          );
        }
      }
    }
    persistenceMechanism.authorizeCompleted(
      principal,
      transaction
    );

    return transaction;
  }

  /**
   * Captures the funds from a previous call to <code>authorize</code>.
   *
   * @param  principal  <code>null</code> is acceptable
   *
   * @see  #authorize(java.security.Principal, java.security.acl.Group, com.aoapps.payments.TransactionRequest, com.aoapps.payments.CreditCard)
   */
  public CaptureResult capture(Principal principal, Transaction transaction) throws SQLException {
    CaptureResult captureResult = provider.capture(transaction.getAuthorizationResult());
    long completedTimeMillis = System.currentTimeMillis();
    transaction.setCaptureTime(completedTimeMillis);
    transaction.setCapturePrincipalName(principal == null ? null : principal.getName());
    transaction.setCaptureResult(captureResult);
    Transaction.Status status;
    switch (captureResult.getCommunicationResult()) {
      case LOCAL_ERROR:
        status = Transaction.Status.LOCAL_ERROR;
        break;
      case IO_ERROR:
        status = Transaction.Status.IO_ERROR;
        break;
      case GATEWAY_ERROR:
        status = Transaction.Status.GATEWAY_ERROR;
        break;
      case SUCCESS:
        status = Transaction.Status.CAPTURED;
        break;
      default:
        throw new LocalizedSQLException("23000", PACKAGE_RESOURCES, "CreditCardProcessor.capture.unexpectedCommunicationResult", captureResult.getCommunicationResult());
    }
    transaction.setStatus(status);

    // Update persistence layer
    persistenceMechanism.saleCompleted(
      principal,
      transaction
    );

    return captureResult;
  }

  /**
   * Voids a previous transaction.
   * Updates the persistence mechanism.
   * Updates the voidResult on the transaction.
   * If the void was successful, updates the status of the transaction passed in.
   * Transaction status must be AUTHORIZED, CAPTURED, or HOLD.
   *
   * @throws  SQLException  when unable to update the persistence layer
   *
   * @see  #sale
   * @see  #authorize
   * @see  #capture
   */
  public VoidResult voidTransaction(Principal principal, Transaction transaction) throws SQLException {
    Transaction.Status status = transaction.getStatus();
    if (
      status == Transaction.Status.AUTHORIZED
      || status == Transaction.Status.CAPTURED
      || status == Transaction.Status.HOLD
    ) {
      // Void on the merchant
      if (
        transaction.getAuthorizationResult() != null
        && transaction.getAuthorizationResult().getProviderUniqueId() != null
        && transaction.getAuthorizationResult().getProviderUniqueId().length() > 0
      ) {
        VoidResult voidResult = provider.voidTransaction(transaction);
        // Update the status
        transaction.setVoidResult(voidResult);
        if (voidResult.getCommunicationResult() == TransactionResult.CommunicationResult.SUCCESS) {
          transaction.setStatus(Transaction.Status.VOID);
        }
        persistenceMechanism.voidCompleted(principal, transaction);

        return voidResult;
      } else {
        throw new LocalizedIllegalArgumentException(PACKAGE_RESOURCES, "CreditCardProcessor.voidTransaction.providerUniqueId.required");
      }
    } else {
      throw new LocalizedIllegalArgumentException(PACKAGE_RESOURCES, "CreditCardProcessor.voidTransaction.invalidStatus", status == null ? null : status.toString());
    }
  }

  /**
   * Requests a credit.
   */
  public CreditResult credit(TransactionRequest transactionRequest, CreditCard creditCard) {
    throw new NotImplementedException("TODO");
    // return provider.credit(transactionRequest, creditCard);
  }

  /**
   * Queries the provider to see if they support the secure storage of credit cards.
   *
   * @throws  IOException   when unable to contact the bank
   */
  public boolean canStoreCreditCards() throws IOException {
    return provider.canStoreCreditCards();
  }

  /**
   * Stores a credit card securely for later reuse.  Sets the providerId, providerUniqueId, principalName, groupName, and persistenceUniqueId.
   * Upon success, clears the cardNumber and expiration date.
   *
   * @throws  IOException   when unable to contact the bank
   * @throws  SQLException  when unable to store in the persistence layer
   */
  public void storeCreditCard(Principal principal, Group group, CreditCard creditCard) throws IOException, SQLException {
    // First, store in the merchant system
    String providerUniqueId = provider.storeCreditCard(creditCard);
    creditCard.setProviderId(provider.getProviderId());
    creditCard.setProviderUniqueId(providerUniqueId);

    // Second, store to the persistence layer (this may also choose to store the card numbers)
    creditCard.setPrincipalName(principal.getName());
    creditCard.setGroupName(group.getName());
    String persistenceUniqueId = persistenceMechanism.storeCreditCard(principal, creditCard);
    creditCard.setPersistenceUniqueId(persistenceUniqueId);

    // Third, clear card numbers (since now stored)
    creditCard.setCardNumber(null);
    creditCard.setExpirationMonth((byte)-1);
    creditCard.setExpirationYear((short)-1);
  }

  /**
   * Updates the credit card details, all except the card number and expiration.  If card stored to secure storage, updates that storage.
   * Any desired changes must have been made to <code>creditCard</code> object preceding this call.
   *
   * @throws  IOException   when unable to contact the bank
   * @throws  SQLException  when unable to store in the persistence layer
   */
  public void updateCreditCard(
    Principal principal,
    CreditCard creditCard
  ) throws IOException, SQLException {
    if (creditCard.getProviderUniqueId() != null) {
      // Update in persistence (this also enforces security)
      persistenceMechanism.updateCreditCard(principal, creditCard);
      // Update in secure storage
      provider.updateCreditCard(creditCard);
    }
  }

  /**
   * Updates the credit card number, expiration, and (optionally) card code.  If card stored to secure storage, updates that storage.
   * Also updates <code>creditCard</code> directly.
   *
   * @throws  IOException   when unable to contact the bank
   * @throws  SQLException  when unable to store in the persistence layer
   */
  public void updateCreditCardNumberAndExpiration(
    Principal principal,
    CreditCard creditCard,
    String cardNumber,
    byte expirationMonth,
    short expirationYear,
    String cardCode
  ) throws IOException, SQLException {
    CreditCard.validateExpirationMonth(expirationMonth, false);
    CreditCard.validateExpirationYear(expirationYear, false);
    cardNumber = CreditCard.numbersOnly(cardNumber);
    if (creditCard.getProviderUniqueId() != null) {
      // Update in persistence (this also enforces security)
      // TODO: 3.0: Store separate type and masked card numbers
      String maskedCardNumber = CreditCard.maskCreditCardNumber(cardNumber);
      persistenceMechanism.updateCardNumber(principal, creditCard, cardNumber, expirationMonth, expirationYear);
      // Update in secure storage
      provider.updateCreditCardNumberAndExpiration(creditCard, cardNumber, expirationMonth, expirationYear, cardCode);
      // Update the masked number
      // TODO: 3.0: Store separate type and masked card numbers
      creditCard.setMaskedCardNumber(maskedCardNumber);
      // cardCode not set here on stored card
    } else {
      // Update directly
      creditCard.setCardNumber(cardNumber); // This also sets the masked value
      if (cardCode != null) {
        creditCard.setCardCode(cardCode);
      }
    }
    creditCard.setExpirationMonth(expirationMonth);
    creditCard.setExpirationYear(expirationYear);
  }

  /**
   * Updates the credit card expiration.  If card stored to secure storage, updates that storage.
   * Also updates <code>creditCard</code> directly.
   *
   * @throws  IOException   when unable to contact the bank
   */
  public void updateCreditCardExpiration(
    Principal principal,
    CreditCard creditCard,
    byte expirationMonth,
    short expirationYear
  ) throws IOException, SQLException  {
    CreditCard.validateExpirationMonth(expirationMonth, false);
    CreditCard.validateExpirationYear(expirationYear, false);
    if (creditCard.getProviderUniqueId() != null) {
      // Update in persistence (this also enforces security)
      persistenceMechanism.updateExpiration(principal, creditCard, expirationMonth, expirationYear);
      // Update in secure storage
      provider.updateCreditCardExpiration(creditCard, expirationMonth, expirationYear);
    }
    // Update directly
    creditCard.setExpirationMonth(expirationMonth);
    creditCard.setExpirationYear(expirationYear);
  }

  /**
   * Deletes the credit card information from the secure storage.  Clears the providerUniqueId and persistenceUniqueId on the creditCard.
   *
   * @throws  IOException   when unable to contact the bank
   * @throws  SQLException  when unable to update the persistence layer
   */
  public void deleteCreditCard(Principal principal, CreditCard creditCard) throws IOException, SQLException {
    // Delete from persistence (this also implements security)
    if (creditCard.getPersistenceUniqueId() != null) {
      persistenceMechanism.deleteCreditCard(principal, creditCard);
      creditCard.setPersistenceUniqueId(null);
    }
    // Delete from provider database
    if (creditCard.getProviderUniqueId() != null) {
      provider.deleteCreditCard(creditCard);
      creditCard.setProviderUniqueId(null);
    }
  }

  /**
   * Synchronizes any replacement masked card numbers or expiration dates from the provider back into the persistence mechanism.
   * <p>
   * This should be called periodically to keep the local representation of the card up-to-date with any new card information
   * available from the payment provider, such as automatic card expiration updates.
   * </p>
   * <p>
   * Scheduling of the synchronization is beyond the scope of this project, but <a href="https://oss.aoapps.com/cron/">AO Cron</a>
   * may be fit for purpose.
   * </p>
   */
  public void synchronizeStoredCards(Principal principal, PrintWriter verboseOut, PrintWriter infoOut, PrintWriter warningOut, boolean dryRun) throws IOException, SQLException {
    if (!provider.canGetTokenizedCreditCards()) {
      if (infoOut != null) {
        infoOut.println(CreditCardProcessor.class.getSimpleName() + "(" + provider.getProviderId() + ").synchronizeStoredCards: Stored card synchronization not supported: skipping");
      }
    } else {
      if (infoOut != null) {
        infoOut.println(CreditCardProcessor.class.getSimpleName() + "(" + provider.getProviderId() + ").synchronizeStoredCards: Synchronizing stored cards");
      }
      // Find all the persisted cards for this provider
      Map<String, CreditCard> persistedCards = persistenceMechanism.getCreditCards(principal, provider.getProviderId());
      if (infoOut != null) {
        infoOut.println(CreditCardProcessor.class.getSimpleName() + "(" + provider.getProviderId() + ").synchronizeStoredCards: Found " + persistedCards.size() + " " + (persistedCards.size() == 1 ? "persisted card" : "persisted cards"));
      }
      // Get all the tokenized cards, with possible replacement masked card numbers and/or expiration dates
      Map<String, TokenizedCreditCard> tokenizedCards = provider.getTokenizedCreditCards(persistedCards, verboseOut, infoOut, warningOut);
      if (infoOut != null) {
        infoOut.println(CreditCardProcessor.class.getSimpleName() + "(" + provider.getProviderId() + ").synchronizeStoredCards: Found " + tokenizedCards.size() + " " + (tokenizedCards.size() == 1 ? "tokenized card" : "tokenized cards"));
      }
      List<TokenizedCreditCard> tokenizedNotFoundPersisted = new ArrayList<>();
      // Perform any replacements
      for (TokenizedCreditCard tokenizedCard : tokenizedCards.values()) {
        String providerUniqueId = tokenizedCard.getProviderUniqueId();
        CreditCard persistedCard = persistedCards.remove(providerUniqueId);
        if (persistedCard == null) {
          tokenizedNotFoundPersisted.add(tokenizedCard);
        } else {
          String replacementMaskedCardNumber = tokenizedCard.getReplacementMaskedCardNumber();
          if (replacementMaskedCardNumber != null) {
            if (infoOut != null) {
              infoOut.println(
                CreditCardProcessor.class.getSimpleName() + "(" + provider.getProviderId() + ").synchronizeStoredCards: CreditCard(persistenceUniqueId = "
                + persistedCard.getPersistenceUniqueId() + ", providerUniqueId = " + providerUniqueId
                + "): Replacing masked card number: " + persistedCard.getMaskedCardNumber()
                + " -> " + replacementMaskedCardNumber
                + (dryRun ? " (DRY RUN)" : "")
              );
            }
            if (!dryRun) {
              persistedCard.setMaskedCardNumber(replacementMaskedCardNumber);
              persistenceMechanism.updateCreditCard(principal, persistedCard);
            }
          }
          Byte replacementExpirationMonth = tokenizedCard.getReplacementExpirationMonth();
          Short replacementExpirationYear = tokenizedCard.getReplacementExpirationYear();
          if (replacementExpirationMonth != null && replacementExpirationYear != null) {
            if (infoOut != null) {
              infoOut.println(
                CreditCardProcessor.class.getSimpleName() + "(" + provider.getProviderId() + ").synchronizeStoredCards: CreditCard(persistenceUniqueId = "
                + persistedCard.getPersistenceUniqueId() + ", providerUniqueId = " + providerUniqueId
                + "): Replacing expiration: " + persistedCard.getExpirationDisplay()
                + " -> " + CreditCard.getExpirationDisplay(replacementExpirationMonth, replacementExpirationYear)
                + (dryRun ? " (DRY RUN)" : "")
              );
            }
            if (!dryRun) {
              persistedCard.setExpirationMonth(replacementExpirationMonth);
              persistedCard.setExpirationYear(replacementExpirationYear);
              persistenceMechanism.updateExpiration(
                principal,
                persistedCard,
                replacementExpirationMonth,
                replacementExpirationYear
              );
            }
          }
        }
      }
      // Warn any persisted cards not found tokenized
      if (!persistedCards.isEmpty()) {
        if (warningOut != null) {
          warningOut.println(CreditCardProcessor.class.getSimpleName() + "(" + provider.getProviderId() + ").synchronizeStoredCards: Found " + persistedCards.size() + " " + (persistedCards.size() == 1 ? "persisted card" : "persisted cards") + " not tokenized ↵");
          for (CreditCard persistedCard : persistedCards.values()) {
            warningOut.println("    persistenceUniqueId: " + persistedCard.getPersistenceUniqueId() + " ↵");
            warningOut.println("        providerUniqueId: " + persistedCard.getProviderUniqueId());
            warningOut.println("        maskedCardNumber: " + persistedCard.getMaskedCardNumber());
            warningOut.println("        comments........: " + persistedCard.getComments());
          }
        }
      }
      // Warn any tokenized cards not found persisted
      if (!tokenizedNotFoundPersisted.isEmpty()) {
        if (warningOut != null) {
          warningOut.println(CreditCardProcessor.class.getSimpleName() + "(" + provider.getProviderId() + ").synchronizeStoredCards: Found " + tokenizedNotFoundPersisted.size() + " " + (tokenizedNotFoundPersisted.size() == 1 ? "tokenized card" : "tokenized cards") + " not persisted ↵");
          for (TokenizedCreditCard tokenizedCard : tokenizedNotFoundPersisted) {
            warningOut.println("    providerUniqueId: " + tokenizedCard.getProviderUniqueId() + " ↵");
            warningOut.println("        providerReplacementMaskedCardNumber: " + tokenizedCard.getProviderReplacementMaskedCardNumber());
            warningOut.println("        replacementMaskedCardNumber........: " + tokenizedCard.getReplacementMaskedCardNumber());
            warningOut.println("        providerReplacementExpiration......: " + tokenizedCard.getProviderReplacementExpiration());
            warningOut.println("        replacementExpiration..............: " + tokenizedCard.getReplacementExpirationMonth() + CreditCard.EXPIRATION_DISPLAY_SEPARATOR + tokenizedCard.getReplacementExpirationYear());
          }
        }
      }
    }
  }
}
