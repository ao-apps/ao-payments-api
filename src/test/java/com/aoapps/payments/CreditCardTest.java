/*
 * ao-payments-api - Payment processing API supporting multiple payment gateways.
 * Copyright (C) 2019, 2021  AO Industries, Inc.
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

import com.aoapps.lang.LocalizedIllegalArgumentException;
import com.aoapps.lang.math.SafeMath;
import java.util.Calendar;
import java.util.GregorianCalendar;
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

	// <editor-fold defaultstate="collapsed" desc="Test validateExpirationMonth">
	@Test
	public void testValidateExpirationMonthValidAllowUnknown() {
		assertEquals(
			(byte)1,
			CreditCard.validateExpirationMonth((byte)1, true)
		);
		assertEquals(
			(byte)12,
			CreditCard.validateExpirationMonth((byte)12, true)
		);
	}

	@Test(expected = LocalizedIllegalArgumentException.class)
	public void testValidateExpirationMonthTooLowAllowUnknown() {
		CreditCard.validateExpirationMonth((byte)0, true);
	}

	@Test(expected = LocalizedIllegalArgumentException.class)
	public void testValidateExpirationMonthTooHighAllowUnknown() {
		CreditCard.validateExpirationMonth((byte)13, true);
	}

	@Test
	public void testValidateExpirationMonthUnknownAllowUnknown() {
		assertEquals(
			CreditCard.UNKNOWN_EXPIRATION_MONTH,
			CreditCard.validateExpirationMonth(CreditCard.UNKNOWN_EXPIRATION_MONTH, true)
		);
	}

	@Test
	public void testValidateExpirationMonthValidDisallowUnknown() {
		assertEquals(
			(byte)1,
			CreditCard.validateExpirationMonth((byte)1, false)
		);
		assertEquals(
			(byte)12,
			CreditCard.validateExpirationMonth((byte)12, false)
		);
	}

	@Test(expected = LocalizedIllegalArgumentException.class)
	public void testValidateExpirationMonthTooLowDisallowUnknown() {
		CreditCard.validateExpirationMonth((byte)0, false);
	}

	@Test(expected = LocalizedIllegalArgumentException.class)
	public void testValidateExpirationMonthTooHighDisllowUnknown() {
		CreditCard.validateExpirationMonth((byte)13, false);
	}

	@Test(expected = LocalizedIllegalArgumentException.class)
	public void testValidateExpirationMonthUnknownDisallowUnknown() {
		CreditCard.validateExpirationMonth(CreditCard.UNKNOWN_EXPIRATION_MONTH, false);
	}
	// </editor-fold>

	// <editor-fold defaultstate="collapsed" desc="Test validateExpirationYear">
	@Test
	public void testValidateExpirationYearValidAllowUnknown() {
		assertEquals(
			CreditCard.MIN_EXPIRATION_YEAR,
			CreditCard.validateExpirationYear(CreditCard.MIN_EXPIRATION_YEAR, true)
		);
		int max = new GregorianCalendar().get(Calendar.YEAR) + CreditCard.EXPIRATION_YEARS_FUTURE;
		assertEquals(
			max,
			CreditCard.validateExpirationYear(SafeMath.castShort(max), true)
		);
	}

	@Test(expected = LocalizedIllegalArgumentException.class)
	public void testValidateExpirationYearTooLowAllowUnknown() {
		CreditCard.validateExpirationYear(SafeMath.castShort(CreditCard.MIN_EXPIRATION_YEAR - 1), true);
	}

	@Test(expected = LocalizedIllegalArgumentException.class)
	public void testValidateExpirationYearTooHighAllowUnknown() {
		int max = new GregorianCalendar().get(Calendar.YEAR) + CreditCard.EXPIRATION_YEARS_FUTURE;
		CreditCard.validateExpirationYear(SafeMath.castShort(max + 1), true);
	}

	@Test
	public void testValidateExpirationYearUnknownAllowUnknown() {
		assertEquals(
			CreditCard.UNKNOWN_EXPIRATION_YEAR,
			CreditCard.validateExpirationYear(CreditCard.UNKNOWN_EXPIRATION_YEAR, true)
		);
	}

	@Test
	public void testValidateExpirationYearValidDisallowUnknown() {
		assertEquals(
			CreditCard.MIN_EXPIRATION_YEAR,
			CreditCard.validateExpirationYear(CreditCard.MIN_EXPIRATION_YEAR, false)
		);
		int max = new GregorianCalendar().get(Calendar.YEAR) + CreditCard.EXPIRATION_YEARS_FUTURE;
		assertEquals(
			max,
			CreditCard.validateExpirationYear(SafeMath.castShort(max), false)
		);
	}

	@Test(expected = LocalizedIllegalArgumentException.class)
	public void testValidateExpirationYearTooLowDisallowUnknown() {
		CreditCard.validateExpirationYear(SafeMath.castShort(CreditCard.MIN_EXPIRATION_YEAR - 1), false);
	}

	@Test(expected = LocalizedIllegalArgumentException.class)
	public void testValidateExpirationYearTooHighDisllowUnknown() {
		int max = new GregorianCalendar().get(Calendar.YEAR) + CreditCard.EXPIRATION_YEARS_FUTURE;
		CreditCard.validateExpirationYear(SafeMath.castShort(max + 1), false);
	}

	@Test(expected = LocalizedIllegalArgumentException.class)
	public void testValidateExpirationYearUnknownDisallowUnknown() {
		CreditCard.validateExpirationYear(CreditCard.UNKNOWN_EXPIRATION_YEAR, false);
	}
	// </editor-fold>
}
