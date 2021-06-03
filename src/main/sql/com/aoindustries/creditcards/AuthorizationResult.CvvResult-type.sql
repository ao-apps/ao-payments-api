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
CREATE TYPE "com.aoindustries.creditcards"."AuthorizationResult.CvvResult" AS ENUM (
  'MATCH',
  'NO_MATCH',
  'NOT_PROCESSED',
  'CVV2_NOT_PROVIDED_BY_MERCHANT',
  'NOT_SUPPORTED_BY_ISSUER',
  'UNKNOWN'
);
COMMENT ON TYPE "com.aoindustries.creditcards"."AuthorizationResult.CvvResult" IS
'Matches enum com.aoindustries.creditcards.AuthorizationResult.CvvResult';
