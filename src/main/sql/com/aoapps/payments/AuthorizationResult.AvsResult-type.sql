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
 * along with ao-payments-api.  If not, see <https://www.gnu.org/licenses/>.
 */
CREATE TYPE "com.aoapps.payments"."AuthorizationResult.AvsResult" AS ENUM (
  'ADDRESS_NOT_PROVIDED',
  'ADDRESS_Y_ZIP_9',
  'ADDRESS_Y_ZIP_5',
  'ADDRESS_Y_ZIP_N',
  'ADDRESS_N_ZIP_9',
  'ADDRESS_N_ZIP_5',
  'ADDRESS_N_ZIP_N',
  'UNAVAILABLE',
  'RETRY',
  'ERROR',
  'SERVICE_NOT_SUPPORTED',
  'NON_US_CARD',
  'NOT_APPLICABLE',
  'UNKNOWN'
);
COMMENT ON TYPE "com.aoapps.payments"."AuthorizationResult.AvsResult" IS
'Matches enum com.aoapps.payments.AuthorizationResult.AvsResult';
