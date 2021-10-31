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
CREATE TYPE "com.aoapps.payments"."AuthorizationResult.DeclineReason" AS ENUM (
  'NO_SPECIFIC',
  'EXPIRED_CARD',
  'PICK_UP_CARD',
  'AVS_MISMATCH',
  'CVV2_MISMATCH',
  'FRAUD_DETECTED',
  'BLOCKED_IP',
  'MANUAL_REVIEW',
  'INSUFFICIENT_FUNDS',
  'MAX_SALE_EXCEEDED',
  'MIN_SALE_NOT_MET',
  'VOLUME_EXCEEDED_1_DAY',
  'USAGE_EXCEEDED_1_DAY',
  'VOLUME_EXCEEDED_3_DAYS',
  'USAGE_EXCEEDED_3_DAYS',
  'VOLUME_EXCEEDED_15_DAYS',
  'USAGE_EXCEEDED_15_DAYS',
  'VOLUME_EXCEEDED_30_DAYS',
  'USAGE_EXCEEDED_30_DAYS',
  'STOLEN_OR_LOST_CARD',
  'AVS_FAILURE',
  'NOT_PROVIDED',
  'UNKNOWN'
);
COMMENT ON TYPE "com.aoapps.payments"."AuthorizationResult.DeclineReason" IS
'Matches enum com.aoapps.payments.AuthorizationResult.DeclineReason';
