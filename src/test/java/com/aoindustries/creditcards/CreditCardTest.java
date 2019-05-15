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

import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 * @see CreditCard
 *
 * @author  AO Industries, Inc.
 */
public class CreditCardTest {

	// <editor-fold defaultstate="collapsed" desc="Test maskCreditCardNumber">
	@Test
	public void testMaskCreditCardNumberDigitsOnly() {
		assertEquals(
			"521234XXXXXX1234",
			CreditCard.maskCreditCardNumber("5212345678901234")
		);
	}

	@Test
	public void testMaskCreditCardNumberDigitsWithSpacesTrimmed() {
		assertEquals(
			"521234XXXXXX1234",
			CreditCard.maskCreditCardNumber("    5212345678901234    ")
		);
	}

	@Test
	public void testMaskCreditCardNumberDigitsWithSpacesInside() {
		assertEquals(
			"5 21234XXXXXX1234",
			CreditCard.maskCreditCardNumber("5 212345678901234")
		);
		assertEquals(
			"521234XXXXXX123 4",
			CreditCard.maskCreditCardNumber("521234567890123 4")
		);
		assertEquals(
			"521234XXX XXX1234",
			CreditCard.maskCreditCardNumber("521234567 8901234")
		);
		assertEquals(
			"5 2 1 2 3 4 X X X X X X 1 2 3 4",
			CreditCard.maskCreditCardNumber("5 2 1 2 3 4 5 6 7 8 9 0 1 2 3 4")
		);
	}

	@Test
	public void testMaskCreditCardNumberDigitsWithOtherCharactersInside() {
		assertEquals(
			"5212-34XX-XXXX-1234",
			CreditCard.maskCreditCardNumber("5212-3456-7890-1234")
		);
		assertEquals(
			"5212-34XX-XXPXX-1234",
			CreditCard.maskCreditCardNumber("5212-3456-78P90-1234")
		);
		assertEquals(
			"52HELP12-34XX-XXPXX-1ME234",
			CreditCard.maskCreditCardNumber("52HELP12-3456-78P90-1ME234")
		);
	}
	// </editor-fold>
}
