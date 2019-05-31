/*
 * ao-credit-cards-api - Credit card processing API supporting multiple payment gateways.
 * Copyright (C) 2019  AO Industries, Inc.
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

import com.aoindustries.lang.NullArgumentException;

/**
 * Minimal information available from {@link MerchantServicesProvider} for stored cards.
 *
 * @see  MerchantServicesProvider#storeCreditCard(com.aoindustries.creditcards.CreditCard)
 * @see  MerchantServicesProvider#getTokenizedCreditCards(java.util.Map, java.io.PrintWriter, java.io.PrintWriter, java.io.PrintWriter)
 *
 * @author  AO Industries, Inc.
 */
public class TokenizedCreditCard {

	private final String providerUniqueId;
	private final String providerReplacementMaskedCardNumber;
	// TODO: 2.0: Store separate type and masked card numbers
	private final String replacementMaskedCardNumber;
	private final String providerReplacementExpiration;
	// TODO: 2.0: A value type to encapsulate Month and Year here and other parts of the API
	private final Byte replacementExpirationMonth;
	private final Short replacementExpirationYear;

	public TokenizedCreditCard(
		String providerUniqueId,
		String providerReplacementMaskedCardNumber,
		// TODO: 2.0: Store separate type and masked card numbers
		String replacementMaskedCardNumber,
		String providerReplacementExpiration,
		Byte replacementExpirationMonth,
		Short replacementExpirationYear
	) {
		this.providerUniqueId = NullArgumentException.checkNotNull(providerUniqueId, "providerUniqueId");
		this.providerReplacementMaskedCardNumber = providerReplacementMaskedCardNumber;
		// TODO: 2.0: Store separate type and masked card numbers
		this.replacementMaskedCardNumber = replacementMaskedCardNumber;
		this.providerReplacementExpiration = providerReplacementExpiration;
		this.replacementExpirationMonth = replacementExpirationMonth;
		this.replacementExpirationYear = replacementExpirationYear;
	}

	/**
	 * Gets the provider-specific unique identifier.
	 */
	public String getProviderUniqueId() {
		return providerUniqueId;
	}

	/**
	 * Gets the provider-specific replacement masked card number for this tokenized card.
	 *
	 * @see  #getReplacementMaskedCardNumber()
	 */
	public String getProviderReplacementMaskedCardNumber() {
		return providerReplacementMaskedCardNumber;
	}

	// TODO: 2.0: Store separate type and masked card numbers

	/**
	 * Gets the replacement masked card number for this tokenized card.
	 * This may be updated by the provider when new card information is available.
	 * This updated number may be then stored back into any local persistence.
	 *
	 * @return  the new masked card number or {@code null} when unchanged or auto-replacements not supported
	 */
	public String getReplacementMaskedCardNumber() {
		return replacementMaskedCardNumber;
	}

	/**
	 * Gets the provider-specific replacement expiration date for this tokenized card.
	 *
	 * @see  #getReplacementExpirationMonth()
	 * @see  #getReplacementExpirationYear()
	 */
	public String getProviderReplacementExpiration() {
		return providerReplacementExpiration;
	}

	/**
	 * Gets the replacement expiration month for this tokenized card.
	 * This may be updated by the provider when new card information is available.
	 * This updated expiration may be then stored back into any local persistence.
	 *
	 * @return  the new expiration month or {@code null} when unchanged or auto-replacements not supported
	 */
	public Byte getReplacementExpirationMonth() {
		return replacementExpirationMonth;
	}

	/**
	 * Gets the replacement expiration year for this tokenized card.
	 * This may be updated by the provider when new card information is available.
	 * This updated expiration may be then stored back into any local persistence.
	 *
	 * @return  the new expiration year or {@code null} when unchanged or auto-replacements not supported
	 */
	public Short getReplacementExpirationYear() {
		return replacementExpirationYear;
	}
}
