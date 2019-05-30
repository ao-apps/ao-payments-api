/*
 * ao-credit-cards-api - Credit card processing API supporting multiple payment gateways.
 * Copyright (C) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2015, 2016, 2019  AO Industries, Inc.
 *     support@aoindustries.com
 *     7262 Bull Pen Cir
 *     Mobile, AL 36695
 *
 * This file is part of ao-credit-cards-api.
 *
 * ao-credit-cards-api is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ao-credit-cards-api is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with ao-credit-cards-api.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.aoindustries.creditcards;

import java.security.Principal;
import java.security.acl.Group;
import java.sql.SQLException;
import java.util.Map;

/**
 * A persistence mechanism stores the credit card data and transaction history.
 *
 * @author  AO Industries, Inc.
 */
public interface PersistenceMechanism {

	/**
	 * Stores a credit card and returns its persistenceUniqueId.
	 */
	String storeCreditCard(Principal principal, CreditCard creditCard) throws SQLException;

	/**
	 * Gets a defensive copy of a stored card given its {@link CreditCard#getPersistenceUniqueId() persistenceUniqueId}.
	 *
	 * @param  persistenceUniqueId  The card's {@link CreditCard#getPersistenceUniqueId() persistenceUniqueId}
	 *
	 * @return  The stored {@link CreditCard} or {@code null} if not found.
	 */
	CreditCard getCreditCard(Principal principal, String persistenceUniqueId) throws SQLException;

	/**
	 * Gets all the stored cards.
	 * <p>
	 * Each element is a defensive copy.  Modifications will not update the
	 * underling persistence.
	 * </p>
	 * <p>
	 * The returned map must be modifiable.
	 * </p>
	 *
	 * @return  The modifiable mapping from {@link CreditCard#getPersistenceUniqueId()} to defensive copies of all the stored cards
	 *
	 * @see  CreditCard#clone()
	 * @see  #updateCreditCard(java.security.Principal, com.aoindustries.creditcards.CreditCard)
	 * @see  #updateCardNumber(java.security.Principal, com.aoindustries.creditcards.CreditCard, java.lang.String, byte, short)
	 * @see  #updateExpiration(java.security.Principal, com.aoindustries.creditcards.CreditCard, byte, short)
	 */
	Map<String,CreditCard> getCreditCards(Principal principal) throws SQLException;

	/**
	 * Updates the stored credit card details, all except the card number and expiration, for a credit card.
	 */
	void updateCreditCard(Principal principal, CreditCard creditCard) throws SQLException;

	/**
	 * Updates the stored card number and expiration for a credit card.
	 */
	void updateCardNumber(
		Principal principal,
		CreditCard creditCard,
		String cardNumber,
		byte expirationMonth,
		short expirationYear
	) throws SQLException;

	/**
	 * Optionally updates the expiration for a credit card.
	 */
	void updateExpiration(
		Principal principal,
		CreditCard creditCard,
		byte expirationMonth,
		short expirationYear
	) throws SQLException;

	/**
	 * Deletes the credit card from the credit card list.
	 */
	void deleteCreditCard(Principal principal, CreditCard creditCard) throws SQLException;

	/**
	 * Inserts a new transaction into the database and returns its persistenceUniqueId.
	 *
	 * @param  principal  <code>null</code> is acceptable
	 * @param  group      <code>null</code> is acceptable
	 */
	String insertTransaction(Principal principal, Group group, Transaction transaction) throws SQLException;

	/**
	 * Updates a transaction in the database after the sale has completed.  The following transaction fields have been updated
	 * and must be stored:
	 * <ol>
	 *   <li>authorizationResult</li>
	 *   <li>captureTime</li>
	 *   <li>capturePrincipalName</li>
	 *   <li>captureResult</li>
	 *   <li>status</li>
	 * </ol>
	 */
	void saleCompleted(Principal principal, Transaction transaction) throws SQLException;

	/**
	 * Updates a transaction in the database after the authorize has completed.  The following transaction fields have been updated
	 * and must be stored:
	 * <ol>
	 *   <li>authorizationResult</li>
	 *   <li>status</li>
	 * </ol>
	 */
	void authorizeCompleted(Principal principal, Transaction transaction) throws SQLException;

	/**
	 * Updates a transaction in the database after the void has completed.
	 */
	void voidCompleted(Principal principal, Transaction transaction) throws SQLException;
}
