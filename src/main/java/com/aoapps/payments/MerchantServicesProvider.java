/*
 * ao-payments-api - Payment processing API supporting multiple payment gateways.
 * Copyright (C) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2015, 2019, 2020, 2021  AO Industries, Inc.
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
 * along with ao-payments-api.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.aoapps.payments;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

/**
 * This set of functions may be provided by a specific merchant services provider/bank.
 * Not all of the functions will be supported by all providers.
 *
 * @author  AO Industries, Inc.
 */
public interface MerchantServicesProvider {

	/**
	 * Gets the unique identifier for this provider.  The persistence layer may enforce unique
	 * IDs per each provider.  Also, each credit, void, capture, and stored credit card transaction
	 * must be performed by the same provider.  The identifier is compared to test this equality.
	 */
	String getProviderId();

	/**
	 * Performs an immediate sale, effectively a combination of <code>authorize</code> and <code>capture</code>.
	 * No exceptions should ever be thrown, the CommunicationResult should be set to the appropriate error instead.
	 *
	 * @see  #authorize
	 * @see  #capture
	 * @see  CreditCardProcessor#sale(java.security.Principal, java.security.acl.Group, com.aoapps.payments.TransactionRequest, com.aoapps.payments.CreditCard)
	 */
	SaleResult sale(TransactionRequest transactionRequest, CreditCard creditCard);

	/**
	 * Authorizes a sale.  The funds are reserved but not captured until a later call to capture.
	 *
	 * @see  #capture(com.aoapps.payments.AuthorizationResult)
	 * @see  #voidTransaction(com.aoapps.payments.Transaction)
	 * @see  CreditCardProcessor#authorize(java.security.Principal, java.security.acl.Group, com.aoapps.payments.TransactionRequest, com.aoapps.payments.CreditCard)
	 */
	AuthorizationResult authorize(TransactionRequest transactionRequest, CreditCard creditCard);

	/**
	 * Captures the funds from a previous call to <code>authorize</code>.
	 *
	 * @see  #authorize
	 * @see  CreditCardProcessor#capture(java.security.Principal, com.aoapps.payments.Transaction)
	 */
	CaptureResult capture(AuthorizationResult authorizationResult);

	/**
	 * Voids a previous transaction.
	 *
	 * @see  CreditCardProcessor#voidTransaction(java.security.Principal, com.aoapps.payments.Transaction)
	 */
	VoidResult voidTransaction(Transaction transaction);

	/**
	 * Requests a credit.
	 *
	 * @see  CreditCardProcessor#credit(com.aoapps.payments.TransactionRequest, com.aoapps.payments.CreditCard)
	 */
	CreditResult credit(TransactionRequest transactionRequest, CreditCard creditCard);

	/**
	 * Queries the provider to see if they support the secure storage of credit cards.
	 *
	 * @throws  IOException   when unable to contact the bank
	 *
	 * @see  CreditCardProcessor#canStoreCreditCards()
	 */
	// Java 8: default method
	boolean canStoreCreditCards() throws IOException;

	/**
	 * Stores a credit card securely for later reuse, returning its providerUniqueId.
	 *
	 * @throws  UnsupportedOperationException  when not supported
	 * @throws  IOException   when unable to contact the bank
	 *
	 * @see  #canStoreCreditCards()
	 * @see  CreditCardProcessor#storeCreditCard(java.security.Principal, java.security.acl.Group, com.aoapps.payments.CreditCard)
	 */
	// Java 8: default method
	String storeCreditCard(CreditCard creditCard) throws UnsupportedOperationException, IOException;

	/**
	 * Updates the credit card details, all except the card number and expiration.
	 *
	 * @throws  UnsupportedOperationException  when not supported
	 *
	 * @see  #canStoreCreditCards()
	 * @see  CreditCardProcessor#updateCreditCard(java.security.Principal, com.aoapps.payments.CreditCard)
	 */
	// Java 8: default method
	void updateCreditCard(CreditCard creditCard) throws UnsupportedOperationException, IOException;

	/**
	 * Updates the credit card number, expiration, and (optionally) card code in the secure storage, card number, expiration, and card code on <code>creditCard</code> are not changed.
	 * This information is stored by local persistence, but some providers also have a copy of this information.  This is used to keep the two
	 * systems in sync.
	 *
	 * @throws  UnsupportedOperationException  when not supported
	 * @throws  IOException   when unable to contact the bank
	 *
	 * @see  #canStoreCreditCards()
	 * @see  CreditCardProcessor#updateCreditCardNumberAndExpiration(java.security.Principal, com.aoapps.payments.CreditCard, java.lang.String, byte, short, java.lang.String)
	 */
	// Java 8: default method
	void updateCreditCardNumberAndExpiration(
		CreditCard creditCard,
		String cardNumber,
		byte expirationMonth, // TODO: 3.0: Make nullable Byte
		short expirationYear, // TODO: 3.0: Make nullable Short
		String cardCode
	) throws UnsupportedOperationException, IOException;

	/**
	 * Updates the credit card expiration in the secure storage, card expiration on <code>creditCard</code> are not changed.
	 *
	 * @throws  UnsupportedOperationException  when not supported
	 * @throws  IOException   when unable to contact the bank
	 *
	 * @see  #canStoreCreditCards()
	 * @see  CreditCardProcessor#updateCreditCardExpiration(java.security.Principal, com.aoapps.payments.CreditCard, byte, short)
	 */
	// Java 8: default method
	void updateCreditCardExpiration(
		CreditCard creditCard,
		byte expirationMonth, // TODO: 3.0: Make nullable Byte
		short expirationYear // TODO: 3.0: Make nullable Short
	) throws UnsupportedOperationException, IOException;

	/**
	 * Deleted the credit card from the provider's secure storage.
	 *
	 * @throws  UnsupportedOperationException  when not supported
	 * @throws  IOException   when unable to contact the bank
	 *
	 * @see  CreditCardProcessor#deleteCreditCard(java.security.Principal, com.aoapps.payments.CreditCard)
	 */
	// Java 8: default method
	void deleteCreditCard(CreditCard creditCard) throws UnsupportedOperationException, IOException;

	/**
	 * Queries the provider to see if they support retrieving the list of cards from the secure storage of credit cards.
	 *
	 * @throws  IOException   when unable to contact the bank
	 *
	 * @see  #getTokenizedCreditCards(java.util.Map, java.io.PrintWriter, java.io.PrintWriter, java.io.PrintWriter)
	 * @see  CreditCardProcessor#synchronizeStoredCards(java.security.Principal, java.io.PrintWriter, java.io.PrintWriter, java.io.PrintWriter, boolean)
	 */
	// Java 8: default method
	boolean canGetTokenizedCreditCards() throws IOException;

	/**
	 * Gets all stored credit cards, with optional replacement masked card numbers and/or expiration dates.
	 * <p>
	 * The returned map must be unmodifiable.
	 * </p>
	 *
	 * @return  The unmodifiable mapping of stores cards from {@link TokenizedCreditCard#getProviderUniqueId()} to {@link TokenizedCreditCard}.
	 *
	 * @throws  UnsupportedOperationException  when not supported
	 * @throws  IOException   when unable to contact the bank
	 *
	 * @see  #canStoreCreditCards()
	 * @see  #canGetTokenizedCreditCards()
	 * @see  CreditCardProcessor#synchronizeStoredCards(java.security.Principal, java.io.PrintWriter, java.io.PrintWriter, java.io.PrintWriter, boolean)
	 * @see  PersistenceMechanism#getCreditCards(java.security.Principal, java.lang.String)
	 */
	// Java 8: default method
	Map<String, TokenizedCreditCard> getTokenizedCreditCards(Map<String, CreditCard> persistedCards, PrintWriter verboseOut, PrintWriter infoOut, PrintWriter warningOut) throws UnsupportedOperationException, IOException;
}
