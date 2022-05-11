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

import static com.aoapps.payments.Resources.PACKAGE_RESOURCES;

import com.aoapps.lang.LocalizedIllegalArgumentException;
import com.aoapps.lang.math.SafeMath;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import org.apache.commons.validator.GenericValidator;

/**
 * Encapsulates the credit card details that are sent to the bank, retrieved from the database, or manipulated in any way.
 * The credit card details include card numbers and related billing address details.
 * <p>
 * If this card is retrieved from an encrypted/protected source, not all the information will necessarily be available.
 * </p>
 *
 * @author  AO Industries, Inc.
 */
public class CreditCard implements Cloneable {

  /**
   * The maximum number of starting digits kept when masked.
   */
  public static final int MASK_START_DIGITS = 6;

  /**
   * The maximum number of ending digits kept when masked.
   */
  public static final int MASK_END_DIGITS = 4;

  /**
   * The character used representing a masked digit.
   */
  // TODO: 3.0: Change to '*', document, adapt data as loaded
  public static final char MASK_CHARACTER = 'X';

  /**
   * The character used for an unknown digits.  Unknown digits would not be masked if available, but are simply not available.
   * This is used when converting from card type + last4 back to a masked card number.
   * In a future version of the API, when both card type and last4 are stored, this mechanism will be unnecessary.
   */
  public static final char UNKNOWN_DIGIT = '?';

  /**
   * When the number of digits is unknown, such as generating a possible masked card number from type + last4, this can be used
   * as a filler between beginning and end of the card number.
   */
  public static final char UNKNOWN_MIDDLE = '…';

  /**
   * Value used to represent an unknown expiration month.
   */
  public static final byte UNKNOWN_EXPIRATION_MONTH = -1;

  /**
   * Value used to represent an unknown expiration month.
   *
   * @deprecated  Please use {@link #UNKNOWN_EXPIRATION_MONTH} instead.
   */
  @Deprecated(forRemoval = true)
  public static final byte UNKNOWN_EXPRIATION_MONTH = UNKNOWN_EXPIRATION_MONTH;

  /**
   * Value used to represent an unknown expiration year.
   */
  // TODO: 3.0: Nullable expiration fields, this constant won't apply anymore
  public static final short UNKNOWN_EXPIRATION_YEAR = -1;

  /**
   * Value used to represent an unknown expiration year.
   *
   * @deprecated  Please use {@link #UNKNOWN_EXPIRATION_YEAR} instead.
   */
  @Deprecated(forRemoval = true)
  public static final short UNKNOWN_EXPRIATION_YEAR = UNKNOWN_EXPIRATION_YEAR;

  /**
   * The minimum expected expiration year.
   */
  public static final short MIN_EXPIRATION_YEAR = 1977;

  /**
   * The maximum number of years in the future expected for an expiration year, inclusive.
   * <ol>
   * <li><a href="https://dashboard.stripe.com/">Stripe Dashboard</a> allows +19 years</li>
   * <li><a href="https://stackoverflow.com/questions/2500588/maximum-year-in-expiry-date-of-credit-card">Stack Overflow - Maximum Year in Expiry Date of Credit Card</a>
   *   indicates +20 years for <a href="https://www.amazon.com/">Amazon</a></li>
   * </ol>
   */
  public static final short EXPIRATION_YEARS_FUTURE = 20;

  /**
   * The prefix used for {@link #getCardNumberDisplay(java.lang.String)}.
   */
  public static final String CARD_NUMBER_DISPLAY_PREFIX = "•••• ";

  /**
   * The middle separator used for {@link #getExpirationDisplay(java.lang.Byte, java.lang.Short)}.
   */
  public static final String EXPIRATION_DISPLAY_SEPARATOR = " / ";

  /**
   * Only keeps the first {@link #MASK_START_DIGITS} and last {@link #MASK_END_DIGITS} digits of a card number after trimming.
   * Other digits are replaced with {@link #MASK_CHARACTER}.
   * All non-digit characters are left intact.
   * If the number is {@code null}, returns an empty string.
   */
  public static String maskCreditCardNumber(String cardNumber) {
    if (cardNumber == null) {
      return "";
    }
    cardNumber = cardNumber.trim();
    int len = cardNumber.length();
    if (len == 0) {
      return "";
    }
    char[] chars = cardNumber.toCharArray();
    // Find the start digits
    int startDigitCount = 0;
    int startDigitPos;
    for (startDigitPos = 0; startDigitPos < len; startDigitPos++) {
      char ch = chars[startDigitPos];
      if (ch >= '0' && ch <= '9') {
        startDigitCount++;
        if (startDigitCount >= MASK_START_DIGITS) {
          break;
        }
      }
    }
    // Find the end digits
    int endDigitCount = 0;
    int endDigitPos;
    for (endDigitPos = len - 1; endDigitPos > startDigitPos; endDigitPos--) {
      char ch = chars[endDigitPos];
      if (ch >= '0' && ch <= '9') {
        endDigitCount++;
        if (endDigitCount >= MASK_END_DIGITS) {
          break;
        }
      }
    }
    // Replace all between start and end
    boolean replaced = false;
    for (int i = startDigitPos + 1; i < endDigitPos; i++) {
      char ch = chars[i];
      if (ch >= '0' && ch <= '9') {
        chars[i] = MASK_CHARACTER;
        replaced = true;
      }
    }
    return replaced ? new String(chars) : cardNumber;
  }

  /**
   * Gets the numbers out of a String.
   *
   * @param  value  the value to extract numbers from
   * @param  allowUnknownDigit  selects inclusion of {@link #UNKNOWN_DIGIT} in the result
   */
  public static String numbersOnly(String value, boolean allowUnknownDigit) {
    if (value == null) {
      return null;
    }
    int len = value.length();
    if (len == 0) {
      return value;
    }
    StringBuilder sb = new StringBuilder(len);
    for (int c = 0; c < len; c++) {
      char ch = value.charAt(c);
      if (
          ch >= '0' && ch <= '9'
              || (allowUnknownDigit && ch == UNKNOWN_DIGIT)
      ) {
        sb.append(ch);
      }
    }
    return sb.length() == len ? value : sb.toString();
  }

  /**
   * Gets the numbers out of a String, not including
   * any {@link #UNKNOWN_DIGIT}.
   */
  public static String numbersOnly(String value) {
    return numbersOnly(value, false);
  }

  /**
   * See {@link #CARD_NUMBER_DISPLAY_PREFIX} and {@link #getCardNumberDisplay()}.
   */
  public static String getCardNumberDisplay(String cardNumber) {
    if (cardNumber == null) {
      return null;
    }
    cardNumber = cardNumber.trim();
    if (cardNumber.isEmpty()) {
      return "";
    }
    String digits = numbersOnly(cardNumber, true);
    StringBuilder result = new StringBuilder(CARD_NUMBER_DISPLAY_PREFIX.length() + MASK_END_DIGITS);
    result.append(CARD_NUMBER_DISPLAY_PREFIX);
    int digLen = digits.length();
    if (digLen < MASK_END_DIGITS) {
      for (int i = digLen; i < MASK_END_DIGITS; i++) {
        result.append(UNKNOWN_DIGIT);
      }
      result.append(digits);
    } else {
      result.append(digits.substring(digLen - MASK_END_DIGITS));
    }
    return result.toString();
  }

  /**
   * Validates an expiration month.
   */
  public static byte validateExpirationMonth(byte expirationMonth, boolean allowUnknownDate) throws IllegalArgumentException {
    if (
        (
            !allowUnknownDate
                || expirationMonth != UNKNOWN_EXPIRATION_MONTH
        ) && (
            expirationMonth < 1
                || expirationMonth > 12
        )
    ) {
      throw new LocalizedIllegalArgumentException(PACKAGE_RESOURCES, "CreditCard.validateExpirationMonth.expirationMonth.invalid");
    }
    return expirationMonth;
  }

  private static class CurrentYearLock {
    // Empty lock class to help heap profile
  }

  private static final CurrentYearLock currentYearLock = new CurrentYearLock();
  private static long currentYearMillis = Long.MIN_VALUE;
  private static short currentYear;

  /**
   * Gets the current year, cached for up to a second to avoid the overhead of repeated Calendar creation.
   */
  private static short getCurrentYear() {
    synchronized (currentYearLock) {
      long currentTime = System.currentTimeMillis();
      if (
          currentYearMillis == Long.MIN_VALUE
              || currentTime <= (currentYearMillis - 1000)
              || currentTime >= (currentYearMillis + 1000)
      ) {
        currentYear = SafeMath.castShort(new GregorianCalendar().get(Calendar.YEAR));
        currentYearMillis = currentTime;
      }
      return currentYear;
    }
  }

  /**
   * Validates an expiration year.
   */
  public static short validateExpirationYear(short expirationYear, boolean allowUnknownDate) throws IllegalArgumentException {
    if (
        (
            !allowUnknownDate
                || expirationYear != UNKNOWN_EXPIRATION_YEAR
        ) && (
            expirationYear < MIN_EXPIRATION_YEAR
                || expirationYear > (getCurrentYear() + EXPIRATION_YEARS_FUTURE)
        )
    ) {
      throw new LocalizedIllegalArgumentException(PACKAGE_RESOURCES, "CreditCard.validateExpirationYear.expirationYear.invalid");
    }
    return expirationYear;
  }

  /**
   * Gets an expiration date in MMYY format.
   *
   * @param  expirationMonth  the month or {@link #UNKNOWN_EXPIRATION_MONTH} when unknown
   * @param  expirationYear  the year or {@link #UNKNOWN_EXPIRATION_YEAR} when unknown
   * @param  allowUnknownDate  selects inclusion of {@link #UNKNOWN_DIGIT} in the result
   *
   * @throws  IllegalArgumentException  if invalid date
   */
  // TODO: 3.0: Allow Nullable expirationMonth and expirationDate
  public static String getExpirationDateMMYY(byte expirationMonth, short expirationYear, boolean allowUnknownDate) throws IllegalArgumentException {
    validateExpirationMonth(expirationMonth, allowUnknownDate);
    validateExpirationYear(expirationYear, allowUnknownDate);
    StringBuilder sb = new StringBuilder(4);
    if (expirationMonth == UNKNOWN_EXPIRATION_MONTH) {
      sb.append(UNKNOWN_DIGIT).append(UNKNOWN_DIGIT);
    } else {
      if (expirationMonth < 10) {
        sb.append('0');
      }
      sb.append(expirationMonth);
    }
    if (expirationYear == UNKNOWN_EXPIRATION_YEAR) {
      sb.append(UNKNOWN_DIGIT).append(UNKNOWN_DIGIT);
    } else {
      int modYear = expirationYear % 100;
      if (modYear < 10) {
        sb.append('0');
      }
      sb.append(modYear);
    }
    return sb.toString();
  }

  /**
   * Gets an expiration date in MMYY format,
   * not including any {@link #UNKNOWN_DIGIT}.
   *
   * @deprecated  Please use {@link #getExpirationDateMMYY(byte, short, boolean)} allowing for unknown expirations
   */
  @Deprecated(forRemoval = true)
  public static String getExpirationDateMMYY(byte expirationMonth, short expirationYear) {
    return getExpirationDateMMYY(expirationMonth, expirationYear, false);
  }

  /**
   * Gets the expiration display in "MM / YYYY" format or {@code null} when both month and year are unknown.
   *
   * @see  #EXPIRATION_DISPLAY_SEPARATOR
   * @see  #getExpirationDisplay()
   *
   * @throws  IllegalArgumentException  if invalid date
   */
  public static String getExpirationDisplay(Byte expirationMonth, Short expirationYear) throws IllegalArgumentException {
    // Validate
    if (expirationMonth != null) {
      validateExpirationMonth(expirationMonth, true);
    }
    if (expirationYear != null) {
      validateExpirationYear(expirationYear, true);
    }
    final int monthDigits = 2;
    final int yearDigits = 4;
    if (
        (expirationMonth == null || expirationMonth == UNKNOWN_EXPIRATION_MONTH)
            && (expirationYear == null || expirationYear == UNKNOWN_EXPIRATION_YEAR)
    ) {
      return null;
    }
    StringBuilder result = new StringBuilder(monthDigits + EXPIRATION_DISPLAY_SEPARATOR.length() + yearDigits);
    if (expirationMonth == null || expirationMonth == UNKNOWN_EXPIRATION_MONTH) {
      for (int i = 0; i < monthDigits; i++) {
        result.append(UNKNOWN_DIGIT);
      }
    } else {
      String monthStr = expirationMonth.toString();
      for (int i = monthStr.length(); i < monthDigits; i++) {
        result.append('0');
      }
      result.append(monthStr);
    }
    result.append(EXPIRATION_DISPLAY_SEPARATOR);
    if (expirationYear == null || expirationYear == UNKNOWN_EXPIRATION_YEAR) {
      for (int i = 0; i < yearDigits; i++) {
        result.append(UNKNOWN_DIGIT);
      }
    } else {
      String yearStr = expirationYear.toString();
      for (int i = yearStr.length(); i < yearDigits; i++) {
        result.append('0');
      }
      result.append(yearStr);
    }
    return result.toString();
  }

  /**
   * Combines the first and last names into a single name String.
   */
  public static String getFullName(String firstName, String lastName) {
    if (firstName == null) {
      if (lastName == null) {
        return "";
      }
      return lastName.trim();
    } else {
      firstName = firstName.trim();
      if (lastName == null) {
        return firstName;
      }
      lastName = lastName.trim();
      return (firstName + " " + lastName).trim();
    }
  }

  private String persistenceUniqueId;
  private String principalName;
  private String groupName;
  private String providerId;
  private String providerUniqueId;
  private String cardNumber;
  // TODO: 3.0: Store separate type and masked card numbers
  private String maskedCardNumber;
  // TODO: 3.0: A value type to encapsulate Month and Year here and other parts of the API
  private byte expirationMonth = UNKNOWN_EXPIRATION_MONTH; // TODO: 3.0: Make nullable Byte
  private short expirationYear = UNKNOWN_EXPIRATION_YEAR; // TODO: 3.0: Make nullable Short
  private String cardCode;
  private String firstName;
  private String lastName;
  private String companyName;
  private String email;
  private String phone;
  private String fax;
  private String customerId;
  private String customerTaxId;
  private String streetAddress1;
  private String streetAddress2;
  private String city;
  private String state;
  private String postalCode;
  private String countryCode;
  private String comments;

  /**
   * Creates an empty CreditCard.  The values should be set using the appropriate setter methods.
   */
  public CreditCard() {
    // Do nothing
  }

  /**
   * Creates a CreditCard providing all of the details.
   *
   * @throws  IllegalArgumentException  if anything not valid
   */
  @SuppressWarnings("OverridableMethodCallInConstructor")
  public CreditCard(
      String persistenceUniqueId,
      String principalName,
      String groupName,
      String providerId,
      String providerUniqueId,
      String cardNumber,
      // TODO: 3.0: Store separate type and masked card numbers
      String maskedCardNumber,
      byte expirationMonth, // TODO: 3.0: Make nullable Byte
      short expirationYear, // TODO: 3.0: Make nullable Short
      String cardCode,
      String firstName,
      String lastName,
      String companyName,
      String email,
      String phone,
      String fax,
      String customerId,
      String customerTaxId,
      String streetAddress1,
      String streetAddress2,
      String city,
      String state,
      String postalCode,
      String countryCode,
      String comments
  ) {
    setPersistenceUniqueId(persistenceUniqueId);
    setPrincipalName(principalName);
    setGroupName(groupName);
    setProviderId(providerId);
    setProviderUniqueId(providerUniqueId);
    setCardNumber(cardNumber);
    // TODO: 3.0: Store separate type and masked card numbers
    if (maskedCardNumber != null) {
      maskedCardNumber = maskedCardNumber.trim();
      if (!maskedCardNumber.isEmpty()) {
        setMaskedCardNumber(maskedCardNumber);
      }
    }
    setExpirationMonth(expirationMonth);
    setExpirationYear(expirationYear);
    setCardCode(cardCode);
    setFirstName(firstName);
    setLastName(lastName);
    setCompanyName(companyName);
    setEmail(email);
    setPhone(phone);
    setFax(fax);
    setCustomerId(customerId);
    setCustomerTaxId(customerTaxId);
    setStreetAddress1(streetAddress1);
    setStreetAddress2(streetAddress2);
    setCity(city);
    setState(state);
    setPostalCode(postalCode);
    setCountryCode(countryCode);
    setComments(comments);
  }

  @Override
  public CreditCard clone() throws CloneNotSupportedException {
    return (CreditCard) super.clone();
  }

  /**
   * Gets the persistence unique identifier.
   */
  public String getPersistenceUniqueId() {
    return persistenceUniqueId;
  }

  /**
   * Sets the persistence unique identifier.
   */
  public void setPersistenceUniqueId(String persistenceUniqueId) {
    this.persistenceUniqueId = persistenceUniqueId;
  }

  /**
   * Gets the name of the principal who added the card.
   */
  public String getPrincipalName() {
    return principalName;
  }

  /**
   * Sets the name of the principal who added the card.
   */
  public void setPrincipalName(String principalName) {
    this.principalName = principalName;
  }

  /**
   * Gets the name of the group this card belongs to.
   */
  public String getGroupName() {
    return groupName;
  }

  /**
   * Sets the name of the group this card belongs to.
   */
  public void setGroupName(String groupName) {
    this.groupName = groupName;
  }

  /**
   * Gets the id of the merchant services provider that is storing this card.
   */
  public String getProviderId() {
    return providerId;
  }

  /**
   * Sets the id of the merchant services provider that is storing this card.
   */
  public void setProviderId(String providerId) {
    this.providerId = providerId;
  }

  /**
   * Gets the provider-specific unique id representing this card.
   */
  public String getProviderUniqueId() {
    return providerUniqueId;
  }

  /**
   * Sets the provider-specific unique id representing this card.
   */
  public void setProviderUniqueId(String providerUniqueId) {
    this.providerUniqueId = providerUniqueId;
  }

  /**
   * Gets the full credit card number.  This may return <code>null</code> if the full card number is not available.
   */
  public String getCardNumber() {
    return cardNumber;
  }

  /**
   * Trims and sets the full credit card number.  Also sets the masked card number if cardNumber is not null.
   *
   * @throws  IllegalArgumentException  if invalid credit card number
   */
  public void setCardNumber(String cardNumber) {
    if (cardNumber != null && !(cardNumber = cardNumber.trim()).isEmpty()) {
      cardNumber = numbersOnly(cardNumber);
      if (
          //!"4222222222222222".equals(cardNumber)
          !GenericValidator.isCreditCard(cardNumber)
      ) {
        throw new LocalizedIllegalArgumentException(PACKAGE_RESOURCES, "CreditCard.setCardNumber.cardNumber.invalid");
      }
      this.cardNumber = cardNumber;
      // TODO: 3.0: Store separate type and masked card numbers
      this.maskedCardNumber = maskCreditCardNumber(cardNumber);
    } else {
      this.cardNumber = null;
    }
  }

  // TODO: 3.0: Store separate type and masked card numbers

  /**
   * Gets the masked card number.  This contains the first two and last four digits of the card number.
   * This value will usually be available while the card number will is only available for new transactions.
   */
  public String getMaskedCardNumber() {
    return maskedCardNumber;
  }

  /**
   * Sets the masked card number.
   */
  public void setMaskedCardNumber(String maskedCardNumber) {
    this.maskedCardNumber = (maskedCardNumber == null) ? null : maskedCardNumber.trim();
  }

  /**
   * See {@link #getCardNumberDisplay(java.lang.String)}.
   */
  public String getCardNumberDisplay() {
    return getCardNumberDisplay(maskedCardNumber);
  }

  /**
   * Gets the expiration month, where 1 is January and 12 is December.
   *
   * @see  #UNKNOWN_EXPIRATION_MONTH
   */
  // TODO: 3.0: Make nullable Byte
  public byte getExpirationMonth() {
    return expirationMonth;
  }

  /**
   * Sets the expiration month, where 1 is January and 12 is December.
   *
   * @throws  IllegalArgumentException  if out of range.
   *
   * @see  #UNKNOWN_EXPIRATION_MONTH
   */
  // TODO: 3.0: Make nullable Byte
  public void setExpirationMonth(byte expirationMonth) {
    this.expirationMonth = validateExpirationMonth(expirationMonth, true);
  }

  /**
   * Gets the expiration year, such as {@code 2007}.
   *
   * @see  #UNKNOWN_EXPIRATION_YEAR
   */
  // TODO: 3.0: Make nullable Short
  public short getExpirationYear() {
    return expirationYear;
  }

  /**
   * Sets the expiration year, such as {@code 2007}.
   * It also accepts values 0 &lt;= year &lt;= 99.  These values will be automatically
   * added to the current century.
   *
   * @throws  IllegalArgumentException  if the resolved year is &lt; {@link #MIN_EXPIRATION_YEAR} or &gt; (current year + {@link #EXPIRATION_YEARS_FUTURE})
   *
   * @see  #UNKNOWN_EXPIRATION_YEAR
   */
  // TODO: 3.0: Make nullable Short
  public void setExpirationYear(short expirationYear) {
    // Allow 0 - 99 moved to current century
    if (expirationYear >= 0 && expirationYear <= 99) {
      expirationYear = SafeMath.castShort((getCurrentYear() / 100) * 100 + expirationYear);
    }
    this.expirationYear = validateExpirationYear(expirationYear, true);
  }

  /**
   * Gets the expiration date in MMYY format,
   * not including any {@link #UNKNOWN_DIGIT}.
   *
   * @param  allowUnknownDate  selects inclusion of {@link #UNKNOWN_DIGIT} in the result
   *
   * @throws  IllegalArgumentException  if invalid date
   */
  public String getExpirationDateMMYY(boolean allowUnknownDate) {
    return getExpirationDateMMYY(getExpirationMonth(), getExpirationYear(), allowUnknownDate);
  }

  /**
   * Gets the expiration date in MMYY format,
   * not including any {@link #UNKNOWN_DIGIT}.
   *
   * @throws  IllegalArgumentException  if invalid date
   *
   * @deprecated  Please use {@link #getExpirationDateMMYY(boolean)} allowing for unknown expirations
   */
  @Deprecated(forRemoval = true)
  public String getExpirationDateMMYY() {
    return getExpirationDateMMYY(false);
  }

  /**
   * See {@link #getExpirationDisplay(java.lang.Byte, java.lang.Short)}.
   */
  public String getExpirationDisplay() {
    return getExpirationDisplay(expirationMonth, expirationYear);
  }

  /**
   * Gets the three or four digit card security code.  This value is never stored and is therefore only
   * available for a new card.
   */
  public String getCardCode() {
    return cardCode;
  }

  /**
   * Checks a card code format.
   *
   * @param cardCode The card code to check
   *
   * @return The card code to use
   *
   * @throws LocalizedIllegalArgumentException if card code invalid
   */
  public static String validateCardCode(String cardCode) throws LocalizedIllegalArgumentException {
    if (cardCode == null) {
      return null;
    }
    cardCode = cardCode.trim();
    if (cardCode.length() == 0) {
      return null;
    }

    if (cardCode.length() != 3 && cardCode.length() != 4) {
      throw new LocalizedIllegalArgumentException(PACKAGE_RESOURCES, "CreditCard.validateCardCode.cardCode.mustBe3Or4Digits");
    }
    // Each digit must be a number
    for (int c = 0; c < cardCode.length(); c++) {
      char ch = cardCode.charAt(c);
      if (ch < '0' || ch > '9') {
        throw new LocalizedIllegalArgumentException(PACKAGE_RESOURCES, "CreditCard.validateCardCode.cardCode.mustBeDigits");
      }
    }
    return cardCode;
  }

  /**
   * Trims and sets the three or four digit card security code.
   *
   * @throws  IllegalArgumentException  if the value is not either <code>null</code> or purely digits (after trimming) and 3 or 4 digits long.
   */
  public void setCardCode(String cardCode) throws LocalizedIllegalArgumentException {
    this.cardCode = validateCardCode(cardCode);
  }

  /**
   * Gets the first name of the card holder.
   */
  public String getFirstName() {
    return firstName;
  }

  /**
   * Trims and sets the first name of the card holder.
   */
  public void setFirstName(String firstName) {
    this.firstName = firstName == null || firstName.length() == 0 ? null : firstName.trim();
  }

  /**
   * Gets the last name of the card holder.
   */
  public String getLastName() {
    return lastName;
  }

  /**
   * Trims and sets the last name of the card holder.
   */
  public void setLastName(String lastName) {
    this.lastName = lastName == null || lastName.length() == 0 ? null : lastName.trim();
  }

  /**
   * Gets the company name of the card holder.
   */
  public String getCompanyName() {
    return companyName;
  }

  /**
   * Trims and sets the company name of the card holder.
   */
  public void setCompanyName(String companyName) {
    this.companyName = companyName == null || companyName.length() == 0 ? null : companyName.trim();
  }

  /**
   * Gets the card holder's email address.
   */
  public String getEmail() {
    return email;
  }

  /**
   * Trims and sets the card holder's email address.
   *
   * @throws  IllegalArgumentException  if the address does is not in the proper format
   */
  public void setEmail(String email) {
    if (email == null || email.length() == 0) {
      this.email = null;
    } else {
      email = email.trim();
      if (!GenericValidator.isEmail(email)) {
        throw new LocalizedIllegalArgumentException(PACKAGE_RESOURCES, "CreditCard.setEmail.email.invalid");
      }
      this.email = email;
    }
  }

  /**
   * Gets the card holder's telephone number.
   */
  public String getPhone() {
    return phone;
  }

  /**
   * Trims and sets the card holder's telephone number.
   */
  public void setPhone(String phone) {
    this.phone = phone == null || phone.length() == 0 ? null : phone.trim();
  }

  /**
   * Gets the card holder's fax number.
   */
  public String getFax() {
    return fax;
  }

  /**
   * Trims and sets the card holder's fax number.
   */
  public void setFax(String fax) {
    this.fax = fax == null || fax.length() == 0 ? null : fax.trim();
  }

  /**
   * Gets the merchant-specific unique customer ID.
   */
  public String getCustomerId() {
    return customerId;
  }

  /**
   * Sets the merchant-specific unique customer ID.
   */
  public void setCustomerId(String customerId) {
    this.customerId = customerId == null || customerId.length() == 0 ? null : customerId;
  }

  /**
   * Gets the customer SSN or Tax ID.
   */
  public String getCustomerTaxId() {
    return customerTaxId;
  }

  /**
   * Trims and sets the customer SSN or Tax ID, removes any spaces and hyphens.
   *
   * @throws  IllegalArgumentException  if not a nine digit number after trimming.
   */
  public void setCustomerTaxId(String customerTaxId) {
    if (customerTaxId == null) {
      this.customerTaxId = null;
    } else {
      customerTaxId = customerTaxId.trim();
      if (customerTaxId.isEmpty()) {
        this.customerTaxId = null;
      } else {
        customerTaxId = numbersOnly(customerTaxId);
        if (customerTaxId.length() != 9) {
          throw new IllegalArgumentException("Invalid customerTaxId, customerTaxId.length() != 9: " + customerTaxId);
        }
        this.customerTaxId = customerTaxId;
      }
    }
  }

  /**
   * Gets the card holder's street address (first line).
   */
  public String getStreetAddress1() {
    return streetAddress1;
  }

  /**
   * Trims and sets the card holder's street address (first line).
   */
  public void setStreetAddress1(String streetAddress1) {
    this.streetAddress1 = streetAddress1 == null || streetAddress1.length() == 0 ? null : streetAddress1.trim();
  }

  /**
   * Gets the card holder's street address (second line).
   */
  public String getStreetAddress2() {
    return streetAddress2;
  }

  /**
   * Trims and sets the card holder's street address (second line).
   */
  public void setStreetAddress2(String streetAddress2) {
    this.streetAddress2 = streetAddress2 == null || streetAddress2.length() == 0 ? null : streetAddress2.trim();
  }

  /**
   * Gets the card holder's city.
   */
  public String getCity() {
    return city;
  }

  /**
   * Trims and sets the card holder's city.
   */
  public void setCity(String city) {
    this.city = city == null || city.length() == 0 ? null : city.trim();
  }

  /**
   * Gets the card holder's state/province/prefecture.
   */
  public String getState() {
    return state;
  }

  /**
   * Trims and sets the card holder's state/province/prefecture.
   */
  public void setState(String state) {
    this.state = state == null || state.length() == 0 ? null : state.trim();
  }

  /**
   * Gets the card holder's postal code.
   */
  public String getPostalCode() {
    return postalCode;
  }

  /**
   * Trims and sets the card holder's postal code.
   */
  public void setPostalCode(String postalCode) {
    this.postalCode = postalCode == null || postalCode.length() == 0 ? null : postalCode.trim();
  }

  /**
   * Gets the card holder's two-digit ISO 3166-1 alpha-2 country code.
   * <p>
   * See <a href="https://wikipedia.org/wiki/ISO_3166-1_alpha-2">https://wikipedia.org/wiki/ISO_3166-1_alpha-2</a>
   * </p>
   */
  public String getCountryCode() {
    return countryCode;
  }

  /**
   * Trims, converts to upper case, and sets the card holder's two-digit ISO 3166-1 alpha-2 country code.
   * <p>
   * See <a href="https://wikipedia.org/wiki/ISO_3166-1_alpha-2">https://wikipedia.org/wiki/ISO_3166-1_alpha-2</a>
   * </p>
   *
   * @throws  IllegalArgumentException  if not a two-character code (after trimming).
   */
  public void setCountryCode(String countryCode) {
    if (countryCode == null || countryCode.length() == 0) {
      this.countryCode = null;
    } else {
      countryCode = countryCode.trim().toUpperCase(Locale.ENGLISH);
      if (countryCode.length() != 2) {
        throw new LocalizedIllegalArgumentException(PACKAGE_RESOURCES, "CreditCard.setCountryCode.countryCode.mustBe2Digits");
      }
      this.countryCode = countryCode;
    }
  }

  /**
   * Gets the comments associated with this card.
   */
  public String getComments() {
    return comments;
  }

  /**
   * Sets the comments associated with this card.
   */
  public void setComments(String comments) {
    this.comments = comments == null || comments.length() == 0 ? null : comments;
  }
}
