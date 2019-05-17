/*
 * ao-credit-cards-api - Credit card processing API supporting multiple payment gateways.
 * Copyright (C) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2019  AO Industries, Inc.
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

import static com.aoindustries.creditcards.ApplicationResourcesAccessor.accessor;
import com.aoindustries.io.FileUtils;
import com.aoindustries.sql.LocalizedSQLException;
import com.aoindustries.util.PropertiesUtils;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.security.Principal;
import java.security.acl.Group;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Currency;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Stores everything in a simple on-disk properties file.  This is not a scalable solution
 * and is intended for debugging purposes only.
 *
 * @author  AO Industries, Inc.
 */
public class PropertiesPersistenceMechanism implements PersistenceMechanism {

	private static final Logger logger = Logger.getLogger(PropertiesPersistenceMechanism.class.getName());

	private static final Map<String,PropertiesPersistenceMechanism> ppms = new HashMap<>();

	/**
	 * For intra-JVM reusability, only one instance is made per unique path.
	 * However, different paths resolving to the same file (symlink in Unix
	 * is one example) or multiple processes accessing the same file are
	 * not guaranteed to interoperate properly.  In fact, there are no mechanisms
	 * in this code to control that in any way.  Once again, this is intended
	 * for development and debugging use only.
	 */
	public static PropertiesPersistenceMechanism getInstance(String propertiesFilePath) {
		synchronized(ppms) {
			PropertiesPersistenceMechanism ppm = ppms.get(propertiesFilePath);
			if(ppm == null) {
				ppm = new PropertiesPersistenceMechanism(propertiesFilePath);
				ppms.put(propertiesFilePath, ppm);
			}
			return ppm;
		}
	}

	private static final String CC_PRE = "creditCards.";
	private static final String TRANS_PRE = "transactions.";
	private static final String TRANS_REQ_SUF = ".transactionRequest";
	private static final String CC_SUF = ".creditCard";
	private static final String AUTH_RES_SUF = ".authorizationResult";
	private static final String CAP_RES_SUF = ".captureResult";
	private static final String VOID_RES_SUF = ".voidResult";

	/**
	 * The properties file path is obtained at creation time just in case the configuration mapping
	 * changes values.  The properties file will remain the same.
	 */
	final protected String propertiesFilePath;

	/**
	 * The list of credit cards in the database.
	 */
	protected List<CreditCard> internalCreditCards;

	/**
	 * The list of transactions in the database.
	 */
	protected List<Transaction> internalTransactions;

	/**
	 * Creates a new properties persistence mechanism.
	 *
	 * @throws  IllegalArgumentException  if not properly configured
	 */
	private PropertiesPersistenceMechanism(String propertiesFilePath) {
		this.propertiesFilePath = propertiesFilePath;
	}

	private static String getProperty(Properties props, String prefix, long counter, String suffix) {
		return props.getProperty(prefix + counter + suffix);
	}

	private static Currency getPropertyCurrency(Properties props, String prefix, long counter, String suffix) {
		String currencyCode = getProperty(props, prefix, counter, suffix);
		return currencyCode == null ? null : Currency.getInstance(currencyCode);
	}

	private static BigDecimal getPropertyBigDecimal(Properties props, String prefix, long counter, String suffix) {
		String value = getProperty(props, prefix, counter, suffix);
		return value == null ? null : new BigDecimal(value);
	}

	private static <T extends Enum<T>> T getPropertyEnum(Properties props, String prefix, long counter, String suffix, Class<T> enumType) {
		String value = getProperty(props, prefix, counter, suffix);
		return value == null ? null : Enum.valueOf(enumType, value);
	}

	private static Long getPropertyLong(Properties props, String prefix, long counter, String suffix) {
		String value = getProperty(props, prefix, counter, suffix);
		return value == null ? null : Long.valueOf(value);
	}

	private static Boolean getPropertyBoolean(Properties props, String prefix, long counter, String suffix) {
		String value = getProperty(props, prefix, counter, suffix);
		return value == null ? null : Boolean.valueOf(value);
	}

	private static Integer getPropertyInteger(Properties props, String prefix, long counter, String suffix) {
		String value = getProperty(props, prefix, counter, suffix);
		return value == null ? null : Integer.valueOf(value);
	}

	private static long coalesce(Long value, long defaultValue) {
		return value != null ? value : defaultValue;
	}

	private synchronized void loadIfNeeded() throws SQLException {
		if(internalCreditCards == null || internalTransactions == null) {
			try {
				File file = new File(propertiesFilePath);
				if(file.exists()) {
					List<CreditCard> newCreditCards = new ArrayList<>();
					List<Transaction> newTransactions = new ArrayList<>();
					Properties props = PropertiesUtils.loadFromFile(file);
					for(long counter = 1; counter < Long.MAX_VALUE; counter++) {
						String persistenceUniqueId = getProperty(props, CC_PRE, counter, ".persistenceUniqueId");
						if(persistenceUniqueId == null) break;
						CreditCard newCreditCard = new CreditCard(
							persistenceUniqueId,
							getProperty(props, CC_PRE, counter, ".principalName"),
							getProperty(props, CC_PRE, counter, ".groupName"),
							getProperty(props, CC_PRE, counter, ".providerId"),
							getProperty(props, CC_PRE, counter, ".providerUniqueId"),
							null, // cardNumber
							// TODO: 2.0: Store separate type and masked card numbers
							getProperty(props, CC_PRE, counter, ".maskedCardNumber"),
							(byte)-1, // expirationMonth // TODO: 2.0: Make nullable Byte
							(short)-1, // expirationYear // TODO: 2.0: Make nullable Short
							null, // cardCode
							getProperty(props, CC_PRE, counter, ".firstName"),
							getProperty(props, CC_PRE, counter, ".lastName"),
							getProperty(props, CC_PRE, counter, ".companyName"),
							getProperty(props, CC_PRE, counter, ".email"),
							getProperty(props, CC_PRE, counter, ".phone"),
							getProperty(props, CC_PRE, counter, ".fax"),
							getProperty(props, CC_PRE, counter, ".customerId"),
							getProperty(props, CC_PRE, counter, ".customerTaxId"),
							getProperty(props, CC_PRE, counter, ".streetAddress1"),
							getProperty(props, CC_PRE, counter, ".streetAddress2"),
							getProperty(props, CC_PRE, counter, ".city"),
							getProperty(props, CC_PRE, counter, ".state"),
							getProperty(props, CC_PRE, counter, ".postalCode"),
							getProperty(props, CC_PRE, counter, ".countryCode"),
							getProperty(props, CC_PRE, counter, ".comments")
						);
						newCreditCards.add(newCreditCard);
					}
					for(long counter = 1; counter < Long.MAX_VALUE; counter++) {
						String persistenceUniqueId = getProperty(props, TRANS_PRE, counter, ".persistenceUniqueId");
						if(persistenceUniqueId == null) break;
						Transaction newTransaction = new Transaction(
							getProperty(props, TRANS_PRE, counter, ".providerId"),
							persistenceUniqueId,
							getProperty(props, TRANS_PRE, counter, ".groupName"),
							new TransactionRequest(
								getPropertyBoolean   (props, TRANS_PRE, counter, TRANS_REQ_SUF + ".testMode"),
								getProperty          (props, TRANS_PRE, counter, TRANS_REQ_SUF + ".customerIp"),
								getPropertyInteger   (props, TRANS_PRE, counter, TRANS_REQ_SUF + ".duplicateWindow"),
								getProperty          (props, TRANS_PRE, counter, TRANS_REQ_SUF + ".orderNumber"),
								getPropertyCurrency  (props, TRANS_PRE, counter, TRANS_REQ_SUF + ".currencyCode"),
								getPropertyBigDecimal(props, TRANS_PRE, counter, TRANS_REQ_SUF + ".amount"),
								getPropertyBigDecimal(props, TRANS_PRE, counter, TRANS_REQ_SUF + ".taxAmount"),
								getPropertyBoolean   (props, TRANS_PRE, counter, TRANS_REQ_SUF + ".taxExempt"),
								getPropertyBigDecimal(props, TRANS_PRE, counter, TRANS_REQ_SUF + ".shippingAmount"),
								getPropertyBigDecimal(props, TRANS_PRE, counter, TRANS_REQ_SUF + ".dutyAmount"),
								getProperty          (props, TRANS_PRE, counter, TRANS_REQ_SUF + ".shippingFirstName"),
								getProperty          (props, TRANS_PRE, counter, TRANS_REQ_SUF + ".shippingLastName"),
								getProperty          (props, TRANS_PRE, counter, TRANS_REQ_SUF + ".shippingCompanyName"),
								getProperty          (props, TRANS_PRE, counter, TRANS_REQ_SUF + ".shippingStreetAddress1"),
								getProperty          (props, TRANS_PRE, counter, TRANS_REQ_SUF + ".shippingStreetAddress2"),
								getProperty          (props, TRANS_PRE, counter, TRANS_REQ_SUF + ".shippingCity"),
								getProperty          (props, TRANS_PRE, counter, TRANS_REQ_SUF + ".shippingState"),
								getProperty          (props, TRANS_PRE, counter, TRANS_REQ_SUF + ".shippingPostalCode"),
								getProperty          (props, TRANS_PRE, counter, TRANS_REQ_SUF + ".shippingCountryCode"),
								getPropertyBoolean   (props, TRANS_PRE, counter, TRANS_REQ_SUF + ".emailCustomer"),
								getProperty          (props, TRANS_PRE, counter, TRANS_REQ_SUF + ".merchantEmail"),
								getProperty          (props, TRANS_PRE, counter, TRANS_REQ_SUF + ".invoiceNumber"),
								getProperty          (props, TRANS_PRE, counter, TRANS_REQ_SUF + ".purchaseOrderNumber"),
								getProperty          (props, TRANS_PRE, counter, TRANS_REQ_SUF + ".description")
							),
							new CreditCard(
								null, // persistenceUniqueId
								getProperty(props, TRANS_PRE, counter, CC_SUF + ".principalName"),
								getProperty(props, TRANS_PRE, counter, CC_SUF + ".groupName"),
								getProperty(props, TRANS_PRE, counter, ".providerId"),
								getProperty(props, TRANS_PRE, counter, CC_SUF + ".providerUniqueId"),
								null, // cardNumber
								// TODO: 2.0: Store separate type and masked card numbers
								getProperty(props, TRANS_PRE, counter, CC_SUF + ".maskedCardNumber"),
								(byte)-1, // expirationMonth // TODO: 2.0: Make nullable Byte
								(short)-1, // expirationYear // TODO: 2.0: Make nullable Short
								null, // cardCode
								getProperty(props, TRANS_PRE, counter, CC_SUF + ".firstName"),
								getProperty(props, TRANS_PRE, counter, CC_SUF + ".lastName"),
								getProperty(props, TRANS_PRE, counter, CC_SUF + ".companyName"),
								getProperty(props, TRANS_PRE, counter, CC_SUF + ".email"),
								getProperty(props, TRANS_PRE, counter, CC_SUF + ".phone"),
								getProperty(props, TRANS_PRE, counter, CC_SUF + ".fax"),
								getProperty(props, TRANS_PRE, counter, CC_SUF + ".customerId"),
								getProperty(props, TRANS_PRE, counter, CC_SUF + ".customerTaxId"),
								getProperty(props, TRANS_PRE, counter, CC_SUF + ".streetAddress1"),
								getProperty(props, TRANS_PRE, counter, CC_SUF + ".streetAddress2"),
								getProperty(props, TRANS_PRE, counter, CC_SUF + ".city"),
								getProperty(props, TRANS_PRE, counter, CC_SUF + ".state"),
								getProperty(props, TRANS_PRE, counter, CC_SUF + ".postalCode"),
								getProperty(props, TRANS_PRE, counter, CC_SUF + ".countryCode"),
								getProperty(props, TRANS_PRE, counter, CC_SUF + ".comments")
							),
							Long.parseLong(getProperty(props, TRANS_PRE, counter, ".authorizationTime")),
							getProperty(props, TRANS_PRE, counter, ".authorizationPrincipalName"),
							new AuthorizationResult(
								getProperty    (props, TRANS_PRE, counter, ".providerId"),
								getPropertyEnum(props, TRANS_PRE, counter, AUTH_RES_SUF + ".communicationResult", TransactionResult.CommunicationResult.class),
								getProperty    (props, TRANS_PRE, counter, AUTH_RES_SUF + ".providerErrorCode"),
								getPropertyEnum(props, TRANS_PRE, counter, AUTH_RES_SUF + ".errorCode", TransactionResult.ErrorCode.class),
								getProperty    (props, TRANS_PRE, counter, AUTH_RES_SUF + ".providerErrorMessage"),
								getProperty    (props, TRANS_PRE, counter, AUTH_RES_SUF + ".providerUniqueId"),
								getProperty    (props, TRANS_PRE, counter, AUTH_RES_SUF + ".providerReplacementMaskedCardNumber"),
								// TODO: 2.0: Store separate type and masked card numbers
								getProperty    (props, TRANS_PRE, counter, AUTH_RES_SUF + ".replacementMaskedCardNumber"),
								getProperty    (props, TRANS_PRE, counter, AUTH_RES_SUF + ".providerApprovalResult"),
								getPropertyEnum(props, TRANS_PRE, counter, AUTH_RES_SUF + ".approvalResult", AuthorizationResult.ApprovalResult.class),
								getProperty    (props, TRANS_PRE, counter, AUTH_RES_SUF + ".providerDeclineReason"),
								getPropertyEnum(props, TRANS_PRE, counter, AUTH_RES_SUF + ".declineReason", AuthorizationResult.DeclineReason.class),
								getProperty    (props, TRANS_PRE, counter, AUTH_RES_SUF + ".providerReviewReason"),
								getPropertyEnum(props, TRANS_PRE, counter, AUTH_RES_SUF + ".reviewReason", AuthorizationResult.ReviewReason.class),
								getProperty    (props, TRANS_PRE, counter, AUTH_RES_SUF + ".providerCvvResult"),
								getPropertyEnum(props, TRANS_PRE, counter, AUTH_RES_SUF + ".cvvResult", AuthorizationResult.CvvResult.class),
								getProperty    (props, TRANS_PRE, counter, AUTH_RES_SUF + ".providerAvsResult"),
								getPropertyEnum(props, TRANS_PRE, counter, AUTH_RES_SUF + ".avsResult", AuthorizationResult.AvsResult.class),
								getProperty    (props, TRANS_PRE, counter, AUTH_RES_SUF + ".approvalCode")
							),
							coalesce(getPropertyLong(props, TRANS_PRE, counter, ".captureTime"), -1), // TODO: 2.0: Make nullable Long
							getProperty(props, TRANS_PRE, counter, ".capturePrincipalName"),
							new CaptureResult(
								getProperty    (props, TRANS_PRE, counter, ".providerId"),
								getPropertyEnum(props, TRANS_PRE, counter, CAP_RES_SUF + ".communicationResult", TransactionResult.CommunicationResult.class),
								getProperty    (props, TRANS_PRE, counter, CAP_RES_SUF + ".providerErrorCode"),
								getPropertyEnum(props, TRANS_PRE, counter, CAP_RES_SUF + ".errorCode", TransactionResult.ErrorCode.class),
								getProperty    (props, TRANS_PRE, counter, CAP_RES_SUF + ".providerErrorMessage"),
								getProperty    (props, TRANS_PRE, counter, CAP_RES_SUF + ".providerUniqueId")
							),
							coalesce(getPropertyLong(props, TRANS_PRE, counter, ".voidTime"), -1), // TODO: 2.0: Make nullable Long
							getProperty(props, TRANS_PRE, counter, ".voidPrincipalName"),
							new VoidResult(
								getProperty    (props, TRANS_PRE, counter, ".providerId"),
								getPropertyEnum(props, TRANS_PRE, counter, VOID_RES_SUF + ".communicationResult", TransactionResult.CommunicationResult.class),
								getProperty    (props, TRANS_PRE, counter, VOID_RES_SUF + ".providerErrorCode"),
								getPropertyEnum(props, TRANS_PRE, counter, VOID_RES_SUF + ".errorCode", TransactionResult.ErrorCode.class),
								getProperty    (props, TRANS_PRE, counter, VOID_RES_SUF + ".providerErrorMessage"),
								getProperty    (props, TRANS_PRE, counter, VOID_RES_SUF + ".providerUniqueId")
							),
							getPropertyEnum(props, TRANS_PRE, counter, ".status", Transaction.Status.class)
						);
						newTransactions.add(newTransaction);
					}
					internalCreditCards = newCreditCards;
					internalTransactions = newTransactions;
				} else {
					internalCreditCards = new ArrayList<>();
					internalTransactions = new ArrayList<>();
				}
			} catch(IOException err) {
				throw new SQLException(err);
			}
		}
	}

	private static void setProperty(Properties props, String prefix, long counter, String suffix, String value) {
		if(value != null) {
			props.setProperty(prefix + counter + suffix, value);
		}
	}

	private static void setProperty(Properties props, String prefix, long counter, String suffix, Currency value) {
		if(value != null) {
			props.setProperty(prefix + counter + suffix, value.getCurrencyCode());
		}
	}

	private static void setProperty(Properties props, String prefix, long counter, String suffix, BigDecimal value) {
		if(value != null) {
			props.setProperty(prefix + counter + suffix, value.toString());
		}
	}

	private static void setProperty(Properties props, String prefix, long counter, String suffix, Boolean value) {
		if(value != null) {
			props.setProperty(prefix + counter + suffix, value.toString());
		}
	}

	private static void setProperty(Properties props, String prefix, long counter, String suffix, Enum<?> value) {
		if(value != null) {
			props.setProperty(prefix + counter + suffix, value.name());
		}
	}

	private static void setProperty(Properties props, String prefix, long counter, String suffix, Long value) {
		if(value != null) {
			props.setProperty(prefix + counter + suffix, value.toString());
		}
	}

	private synchronized void save() throws SQLException {
		try {
			File newFile = new File(propertiesFilePath+".new");
			File file = new File(propertiesFilePath);
			File backupFile = new File(propertiesFilePath+".backup");
			Properties props = new Properties();
			// Add the credit cards
			long counter = 1;
			for(CreditCard internalCreditCard : internalCreditCards) {
				setProperty(props, CC_PRE, counter, ".persistenceUniqueId", internalCreditCard.getPersistenceUniqueId());
				setProperty(props, CC_PRE, counter, ".principalName", internalCreditCard.getPrincipalName());
				setProperty(props, CC_PRE, counter, ".groupName", internalCreditCard.getGroupName());
				setProperty(props, CC_PRE, counter, ".providerId", internalCreditCard.getProviderId());
				setProperty(props, CC_PRE, counter, ".providerUniqueId", internalCreditCard.getProviderUniqueId());
				// TODO: 2.0: Store separate type and masked card numbers
				setProperty(props, CC_PRE, counter, ".maskedCardNumber", internalCreditCard.getMaskedCardNumber());
				setProperty(props, CC_PRE, counter, ".firstName", internalCreditCard.getFirstName());
				setProperty(props, CC_PRE, counter, ".lastName", internalCreditCard.getLastName());
				setProperty(props, CC_PRE, counter, ".companyName", internalCreditCard.getCompanyName());
				setProperty(props, CC_PRE, counter, ".email", internalCreditCard.getEmail());
				setProperty(props, CC_PRE, counter, ".phone", internalCreditCard.getPhone());
				setProperty(props, CC_PRE, counter, ".fax", internalCreditCard.getFax());
				setProperty(props, CC_PRE, counter, ".customerId", internalCreditCard.getCustomerId());
				setProperty(props, CC_PRE, counter, ".customerTaxId", internalCreditCard.getCustomerTaxId());
				setProperty(props, CC_PRE, counter, ".streetAddress1", internalCreditCard.getStreetAddress1());
				setProperty(props, CC_PRE, counter, ".streetAddress2", internalCreditCard.getStreetAddress2());
				setProperty(props, CC_PRE, counter, ".city", internalCreditCard.getCity());
				setProperty(props, CC_PRE, counter, ".state", internalCreditCard.getState());
				setProperty(props, CC_PRE, counter, ".postalCode", internalCreditCard.getPostalCode());
				setProperty(props, CC_PRE, counter, ".countryCode", internalCreditCard.getCountryCode());
				setProperty(props, CC_PRE, counter, ".comments", internalCreditCard.getComments());
				counter++;
			}
			// Add the transactions
			counter = 1;
			for(Transaction internalTransaction : internalTransactions) {
				setProperty(props, TRANS_PRE, counter, ".providerId", internalTransaction.getProviderId());
				setProperty(props, TRANS_PRE, counter, ".persistenceUniqueId", internalTransaction.getPersistenceUniqueId());
				setProperty(props, TRANS_PRE, counter, ".groupName", internalTransaction.getGroupName());
				TransactionRequest transactionRequest = internalTransaction.getTransactionRequest();
				if(transactionRequest != null) {
					setProperty(props, TRANS_PRE, counter, TRANS_REQ_SUF + ".testMode", transactionRequest.getTestMode());
					setProperty(props, TRANS_PRE, counter, TRANS_REQ_SUF + ".customerIp", transactionRequest.getCustomerIp());
					setProperty(props, TRANS_PRE, counter, TRANS_REQ_SUF + ".duplicateWindow", Integer.toString(transactionRequest.getDuplicateWindow()));
					setProperty(props, TRANS_PRE, counter, TRANS_REQ_SUF + ".orderNumber", transactionRequest.getOrderNumber());
					setProperty(props, TRANS_PRE, counter, TRANS_REQ_SUF + ".currencyCode", transactionRequest.getCurrency());
					setProperty(props, TRANS_PRE, counter, TRANS_REQ_SUF + ".amount", transactionRequest.getAmount());
					setProperty(props, TRANS_PRE, counter, TRANS_REQ_SUF + ".taxAmount", transactionRequest.getTaxAmount());
					setProperty(props, TRANS_PRE, counter, TRANS_REQ_SUF + ".taxExempt", transactionRequest.getTaxExempt());
					setProperty(props, TRANS_PRE, counter, TRANS_REQ_SUF + ".shippingAmount", transactionRequest.getShippingAmount());
					setProperty(props, TRANS_PRE, counter, TRANS_REQ_SUF + ".dutyAmount", transactionRequest.getDutyAmount());
					setProperty(props, TRANS_PRE, counter, TRANS_REQ_SUF + ".shippingFirstName", transactionRequest.getShippingFirstName());
					setProperty(props, TRANS_PRE, counter, TRANS_REQ_SUF + ".shippingLastName", transactionRequest.getShippingLastName());
					setProperty(props, TRANS_PRE, counter, TRANS_REQ_SUF + ".shippingCompanyName", transactionRequest.getShippingCompanyName());
					setProperty(props, TRANS_PRE, counter, TRANS_REQ_SUF + ".shippingStreetAddress1", transactionRequest.getShippingStreetAddress1());
					setProperty(props, TRANS_PRE, counter, TRANS_REQ_SUF + ".shippingStreetAddress2", transactionRequest.getShippingStreetAddress2());
					setProperty(props, TRANS_PRE, counter, TRANS_REQ_SUF + ".shippingCity", transactionRequest.getShippingCity());
					setProperty(props, TRANS_PRE, counter, TRANS_REQ_SUF + ".shippingState", transactionRequest.getShippingState());
					setProperty(props, TRANS_PRE, counter, TRANS_REQ_SUF + ".shippingPostalCode", transactionRequest.getShippingPostalCode());
					setProperty(props, TRANS_PRE, counter, TRANS_REQ_SUF + ".shippingCountryCode", transactionRequest.getShippingCountryCode());
					setProperty(props, TRANS_PRE, counter, TRANS_REQ_SUF + ".emailCustomer", transactionRequest.getEmailCustomer());
					setProperty(props, TRANS_PRE, counter, TRANS_REQ_SUF + ".merchantEmail", transactionRequest.getMerchantEmail());
					setProperty(props, TRANS_PRE, counter, TRANS_REQ_SUF + ".invoiceNumber", transactionRequest.getInvoiceNumber());
					setProperty(props, TRANS_PRE, counter, TRANS_REQ_SUF + ".purchaseOrderNumber", transactionRequest.getPurchaseOrderNumber());
					setProperty(props, TRANS_PRE, counter, TRANS_REQ_SUF + ".description", transactionRequest.getDescription());
				}
				CreditCard creditCard = internalTransaction.getCreditCard();
				if(creditCard != null) {
					setProperty(props, TRANS_PRE, counter, CC_SUF + ".principalName", creditCard.getPrincipalName());
					setProperty(props, TRANS_PRE, counter, CC_SUF + ".groupName", creditCard.getGroupName());
					setProperty(props, TRANS_PRE, counter, CC_SUF + ".providerUniqueId", creditCard.getProviderUniqueId());
					// TODO: 2.0: Store separate type and masked card numbers
					setProperty(props, TRANS_PRE, counter, CC_SUF + ".maskedCardNumber", creditCard.getMaskedCardNumber());
					setProperty(props, TRANS_PRE, counter, CC_SUF + ".firstName", creditCard.getFirstName());
					setProperty(props, TRANS_PRE, counter, CC_SUF + ".lastName", creditCard.getLastName());
					setProperty(props, TRANS_PRE, counter, CC_SUF + ".companyName", creditCard.getCompanyName());
					setProperty(props, TRANS_PRE, counter, CC_SUF + ".email", creditCard.getEmail());
					setProperty(props, TRANS_PRE, counter, CC_SUF + ".phone", creditCard.getPhone());
					setProperty(props, TRANS_PRE, counter, CC_SUF + ".fax", creditCard.getFax());
					setProperty(props, TRANS_PRE, counter, CC_SUF + ".customerId", creditCard.getCustomerId());
					setProperty(props, TRANS_PRE, counter, CC_SUF + ".customerTaxId", creditCard.getCustomerTaxId());
					setProperty(props, TRANS_PRE, counter, CC_SUF + ".streetAddress1", creditCard.getStreetAddress1());
					setProperty(props, TRANS_PRE, counter, CC_SUF + ".streetAddress2", creditCard.getStreetAddress2());
					setProperty(props, TRANS_PRE, counter, CC_SUF + ".city", creditCard.getCity());
					setProperty(props, TRANS_PRE, counter, CC_SUF + ".state", creditCard.getState());
					setProperty(props, TRANS_PRE, counter, CC_SUF + ".postalCode", creditCard.getPostalCode());
					setProperty(props, TRANS_PRE, counter, CC_SUF + ".countryCode", creditCard.getCountryCode());
					setProperty(props, TRANS_PRE, counter, CC_SUF + ".comments", creditCard.getComments());
				}
				setProperty(props, TRANS_PRE, counter, ".authorizationTime", Long.toString(internalTransaction.getAuthorizationTime()));
				setProperty(props, TRANS_PRE, counter, ".authorizationPrincipalName", internalTransaction.getAuthorizationPrincipalName());
				AuthorizationResult authorizationResult = internalTransaction.getAuthorizationResult();
				if(authorizationResult != null) {
					setProperty(props, TRANS_PRE, counter, AUTH_RES_SUF + ".communicationResult", authorizationResult.getCommunicationResult());
					setProperty(props, TRANS_PRE, counter, AUTH_RES_SUF + ".providerErrorCode", authorizationResult.getProviderErrorCode());
					setProperty(props, TRANS_PRE, counter, AUTH_RES_SUF + ".errorCode", authorizationResult.getErrorCode());
					setProperty(props, TRANS_PRE, counter, AUTH_RES_SUF + ".providerErrorMessage", authorizationResult.getProviderErrorMessage());
					setProperty(props, TRANS_PRE, counter, AUTH_RES_SUF + ".providerUniqueId", authorizationResult.getProviderUniqueId());
					setProperty(props, TRANS_PRE, counter, AUTH_RES_SUF + ".providerReplacementMaskedCardNumber", authorizationResult.getProviderReplacementMaskedCardNumber());
					// TODO: 2.0: Store separate type and masked card numbers
					setProperty(props, TRANS_PRE, counter, AUTH_RES_SUF + ".replacementMaskedCardNumber", authorizationResult.getReplacementMaskedCardNumber());
					setProperty(props, TRANS_PRE, counter, AUTH_RES_SUF + ".providerApprovalResult", authorizationResult.getProviderApprovalResult());
					setProperty(props, TRANS_PRE, counter, AUTH_RES_SUF + ".approvalResult", authorizationResult.getApprovalResult());
					setProperty(props, TRANS_PRE, counter, AUTH_RES_SUF + ".providerDeclineReason", authorizationResult.getProviderDeclineReason());
					setProperty(props, TRANS_PRE, counter, AUTH_RES_SUF + ".declineReason", authorizationResult.getDeclineReason());
					setProperty(props, TRANS_PRE, counter, AUTH_RES_SUF + ".providerReviewReason", authorizationResult.getProviderReviewReason());
					setProperty(props, TRANS_PRE, counter, AUTH_RES_SUF + ".reviewReason", authorizationResult.getReviewReason());
					setProperty(props, TRANS_PRE, counter, AUTH_RES_SUF + ".providerCvvResult", authorizationResult.getProviderCvvResult());
					setProperty(props, TRANS_PRE, counter, AUTH_RES_SUF + ".cvvResult", authorizationResult.getCvvResult());
					setProperty(props, TRANS_PRE, counter, AUTH_RES_SUF + ".providerAvsResult", authorizationResult.getProviderAvsResult());
					setProperty(props, TRANS_PRE, counter, AUTH_RES_SUF + ".avsResult", authorizationResult.getAvsResult());
					setProperty(props, TRANS_PRE, counter, AUTH_RES_SUF + ".approvalCode", authorizationResult.getApprovalCode());
				}
				setProperty(props, TRANS_PRE, counter, ".captureTime", internalTransaction.getCaptureTime());
				setProperty(props, TRANS_PRE, counter, ".capturePrincipalName", internalTransaction.getCapturePrincipalName());
				CaptureResult captureResult = internalTransaction.getCaptureResult();
				if(captureResult != null) {
					setProperty(props, TRANS_PRE, counter, CAP_RES_SUF + ".communicationResult", captureResult.getCommunicationResult());
					setProperty(props, TRANS_PRE, counter, CAP_RES_SUF + ".providerErrorCode", captureResult.getProviderErrorCode());
					setProperty(props, TRANS_PRE, counter, CAP_RES_SUF + ".errorCode", captureResult.getErrorCode());
					setProperty(props, TRANS_PRE, counter, CAP_RES_SUF + ".providerErrorMessage", captureResult.getProviderErrorMessage());
					setProperty(props, TRANS_PRE, counter, CAP_RES_SUF + ".providerUniqueId", captureResult.getProviderUniqueId());
				}
				setProperty(props, TRANS_PRE, counter, ".voidTime", internalTransaction.getVoidTime());
				setProperty(props, TRANS_PRE, counter, ".voidPrincipalName", internalTransaction.getVoidPrincipalName());
				VoidResult voidResult = internalTransaction.getVoidResult();
				if(voidResult != null) {
					setProperty(props, TRANS_PRE, counter, VOID_RES_SUF + ".communicationResult", voidResult.getCommunicationResult());
					setProperty(props, TRANS_PRE, counter, VOID_RES_SUF + ".providerErrorCode", voidResult.getProviderErrorCode());
					setProperty(props, TRANS_PRE, counter, VOID_RES_SUF + ".errorCode", voidResult.getErrorCode());
					setProperty(props, TRANS_PRE, counter, VOID_RES_SUF + ".providerErrorMessage", voidResult.getProviderErrorMessage());
					setProperty(props, TRANS_PRE, counter, VOID_RES_SUF + ".providerUniqueId", voidResult.getProviderUniqueId());
				}
				setProperty(props, TRANS_PRE, counter, ".status", internalTransaction.getStatus());
				counter++;
			}
			// Store the a new file
			if(newFile.exists()) FileUtils.delete(newFile);
			try (OutputStream out = new BufferedOutputStream(new FileOutputStream(newFile))) {
				// TODO: Add a version number from a Maven.properties
				props.store(out, "Generated by " + PropertiesPersistenceMechanism.class.getName());
			}
			// Move the file into place
			if(backupFile.exists()) FileUtils.delete(backupFile);
			if(file.exists()) FileUtils.rename(file, backupFile);
			FileUtils.rename(newFile, file);
		} catch(IOException err) {
			throw new SQLException(err);
		}
	}

	@Override
	public synchronized String storeCreditCard(Principal principal, CreditCard creditCard) throws SQLException {
		loadIfNeeded();
		long highest = 0;
		for(CreditCard internalCreditCard : internalCreditCards) {
			try {
				long id = Long.parseLong(internalCreditCard.getPersistenceUniqueId());
				if(id>highest) highest = id;
			} catch(NumberFormatException err) {
				// This should not happen, but is not critical
				logger.log(Level.WARNING, null, err);
			}
		}
		String uniqueId = Long.toString(highest + 1);
		CreditCard internalCreditCard = creditCard.clone();
		internalCreditCard.setPersistenceUniqueId(uniqueId);
		internalCreditCards.add(internalCreditCard);
		save();
		return uniqueId;
	}

	/**
	 * Card numbers and expiration dates are not persisted to the properties files - encrypted local storage not supported.
	 */
	@Override
	synchronized public void updateCreditCard(Principal principal, CreditCard creditCard) throws SQLException {
		loadIfNeeded();
		// Find the card with matching persistence id
		CreditCard internalCreditCard = getCreditCard(creditCard.getPersistenceUniqueId());
		if(internalCreditCard == null) throw new LocalizedSQLException(accessor, "PersistenceMechanism.updateCardNumber.unableToFindCard", creditCard.getPersistenceUniqueId());
		internalCreditCard.setMaskedCardNumber(creditCard.getMaskedCardNumber());
		internalCreditCard.setFirstName(creditCard.getFirstName());
		internalCreditCard.setLastName(creditCard.getLastName());
		internalCreditCard.setCompanyName(creditCard.getCompanyName());
		internalCreditCard.setEmail(creditCard.getEmail());
		internalCreditCard.setPhone(creditCard.getPhone());
		internalCreditCard.setFax(creditCard.getFax());
		internalCreditCard.setCustomerId(creditCard.getCustomerId());
		internalCreditCard.setCustomerTaxId(creditCard.getCustomerTaxId());
		internalCreditCard.setStreetAddress1(creditCard.getStreetAddress1());
		internalCreditCard.setStreetAddress2(creditCard.getStreetAddress2());
		internalCreditCard.setCity(creditCard.getCity());
		internalCreditCard.setState(creditCard.getState());
		internalCreditCard.setPostalCode(creditCard.getPostalCode());
		internalCreditCard.setCountryCode(creditCard.getCountryCode());
		internalCreditCard.setComments(creditCard.getComments());
		save();
	}

	/**
	 * Card numbers and expiration dates are not persisted to the properties files - encrypted local storage not supported.
	 */
	@Override
	synchronized public void updateCardNumber(
		Principal principal,
		CreditCard creditCard,
		String cardNumber,
		byte expirationMonth, // TODO: 2.0: Make nullable Byte
		short expirationYear // TODO: 2.0: Make nullable Short
	) throws SQLException {
		loadIfNeeded();
		// Find the card with matching persistence id
		CreditCard internalCreditCard = getCreditCard(creditCard.getPersistenceUniqueId());
		if(internalCreditCard == null) throw new LocalizedSQLException(accessor, "PersistenceMechanism.updateCardNumber.unableToFindCard", creditCard.getPersistenceUniqueId());
		// TODO: 2.0: Store separate type and masked card numbers
		internalCreditCard.setMaskedCardNumber(CreditCard.maskCreditCardNumber(cardNumber));
		save();
	}

	/**
	 * Expiration dates are not persisted to the properties files - encrypted local storage not supported.
	 */
	@Override
	public void updateExpiration(
		Principal principal,
		CreditCard creditCard,
		byte expirationMonth, // TODO: 2.0: Make nullable Byte
		short expirationYear // TODO: 2.0: Make nullable Short
	) throws SQLException {
	}

	synchronized private CreditCard getCreditCard(String persistenceUniqueId) throws SQLException {
		loadIfNeeded();
		for(CreditCard internalCreditCard : internalCreditCards) {
			if(persistenceUniqueId.equals(internalCreditCard.getPersistenceUniqueId())) return internalCreditCard.clone();
		}
		return null;
	}

	@Override
	synchronized public void deleteCreditCard(Principal principal, CreditCard creditCard) throws SQLException {
		if(creditCard.getPersistenceUniqueId() != null) {
			loadIfNeeded();
			boolean modified=false;
			Iterator<CreditCard> I = internalCreditCards.iterator();
			while(I.hasNext()) {
				CreditCard internalCreditCard = I.next();
				if(creditCard.getPersistenceUniqueId().equals(internalCreditCard.getPersistenceUniqueId())) {
					I.remove();
					modified = true;
				}
			}
			if(modified) save();
		}
	}

	@Override
	synchronized public String insertTransaction(Principal principal, Group group, Transaction transaction) throws SQLException {
		loadIfNeeded();
		long highest = 0;
		for(Transaction internalTransaction : internalTransactions) {
			try {
				long id = Long.parseLong(internalTransaction.getPersistenceUniqueId());
				if(id>highest) highest = id;
			} catch(NumberFormatException err) {
				// This should not happen, but is not critical
				logger.log(Level.WARNING, null, err);
			}
		}
		String uniqueId = Long.toString(highest + 1);
		Transaction internalTransaction = transaction.clone();
		internalTransaction.setPersistenceUniqueId(uniqueId);
		internalTransactions.add(internalTransaction);
		save();
		return uniqueId;
	}

	@Override
	public void saleCompleted(Principal principal, Transaction transaction) throws SQLException {
		updateTransaction(principal, transaction);
	}

	@Override
	public void authorizeCompleted(Principal principal, Transaction transaction) throws SQLException {
		updateTransaction(principal, transaction);
	}

	@Override
	public void voidCompleted(Principal principal, Transaction transaction) throws SQLException {
		updateTransaction(principal, transaction);
	}

	synchronized private void updateTransaction(Principal principal, Transaction transaction) throws SQLException {
		loadIfNeeded();
		// Find the transaction with the matching persistence unique ID
		for(int c=0;c<internalTransactions.size();c++) {
			Transaction internalTransaction = internalTransactions.get(c);
			if(internalTransaction.getPersistenceUniqueId().equals(transaction.getPersistenceUniqueId())) {
				internalTransactions.set(c, transaction.clone());
				save();
				return;
			}
		}
		throw new LocalizedSQLException(accessor, "PersistenceMechanism.updateTransaction.unableToFindTransaction", transaction.getPersistenceUniqueId());
	}
}
