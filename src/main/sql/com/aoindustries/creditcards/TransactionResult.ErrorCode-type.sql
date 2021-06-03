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
CREATE TYPE "com.aoindustries.creditcards"."TransactionResult.ErrorCode" AS ENUM (
  -- ALL
  'UNKNOWN',
  -- LOCAL_ERROR
  'HASH_CHECK_FAILED',
  -- IO_ERROR
  -- GATEWAY_ERROR
  'RATE_LIMIT',
  'INVALID_TRANSACTION_TYPE',
  'VOICE_AUTHORIZATION_REQUIRED',
  'INSUFFICIENT_PERMISSIONS',
  'INVALID_AMOUNT',
  'INVALID_CARD_NUMBER',
  'INVALID_EXPIRATION_DATE',
  'CARD_EXPIRED',
  'DUPLICATE',
  'APPROVAL_CODE_REQUIRED',
  'INVALID_MERCHANT_ID',
  'INVALID_PARTNER',
  'INVALID_PROVIDER_UNIQUE_ID',
  'TRANSACTION_NOT_FOUND',
  'CARD_TYPE_NOT_SUPPORTED',
  'ERROR_TRY_AGAIN',
  'ERROR_TRY_AGAIN_5_MINUTES',
  'PROVIDER_CONFIGURATION_ERROR',
  'APPROVED_BUT_SETTLEMENT_FAILED',
  'INVALID_CURRENCY_CODE',
  'MUST_BE_ENCRYPTED',
  'NO_SESSION',
  'CAPTURE_AMOUNT_LESS_THAN_AUTHORIZATION',
  'CAPTURE_AMOUNT_GREATER_THAN_AUTHORIZATION',
  'AMOUNT_TOO_HIGH',
  'TRANSACTION_NOT_SETTLED',
  'SUM_OF_CREDITS_TOO_HIGH',
  'AUTHORIZED_NOTIFICATION_FAILED',
  'CREDIT_CRITERIA_NOT_MET',
  'ACH_ONLY',
  'GATEWAY_SECURITY_GUIDELINES_NOT_MET',
  'INVALID_APPROVAL_CODE',
  'INVALID_DUTY_AMOUNT',
  'INVALID_SHIPPING_AMOUNT',
  'INVALID_TAX_AMOUNT',
  'INVALID_CUSTOMER_TAX_ID',
  'INVALID_CARD_CODE',
  'CUSTOMER_ACCOUNT_DISABLED',
  'INVALID_INVOICE_NUMBER',
  'INVALID_ORDER_NUMBER',
  'INVALID_CARD_NAME',
  'INVALID_CARD_ADDRESS',
  'INVALID_CARD_CITY',
  'INVALID_CARD_STATE',
  'INVALID_CARD_POSTAL_CODE',
  'INVALID_CARD_COUNTRY_CODE',
  'INVALID_CARD_PHONE',
  'INVALID_CARD_FAX',
  'INVALID_CARD_EMAIL',
  'INVALID_SHIPPING_NAME',
  'INVALID_SHIPPING_ADDRESS',
  'INVALID_SHIPPING_CITY',
  'INVALID_SHIPPING_STATE',
  'INVALID_SHIPPING_POSTAL_CODE',
  'INVALID_SHIPPING_COUNTRY_CODE',
  'CURRENCY_NOT_SUPPORTED'
);
COMMENT ON TYPE "com.aoindustries.creditcards"."TransactionResult.ErrorCode" IS
'Matches enum com.aoindustries.creditcards.TransactionResult.ErrorCode';
