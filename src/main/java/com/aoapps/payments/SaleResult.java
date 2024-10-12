/*
 * ao-payments-api - Payment processing API supporting multiple payment gateways.
 * Copyright (C) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2016, 2019, 2021, 2022, 2024  AO Industries, Inc.
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

/**
 * Encapsulates the results of a sale.
 *
 * @see  MerchantServicesProvider#sale
 *
 * @author  AO Industries, Inc.
 */
public class SaleResult {

  private final AuthorizationResult authorizationResult;
  private final CaptureResult captureResult;

  /**
   * Creates a new {@link SaleResult}.
   */
  public SaleResult(
      AuthorizationResult authorizationResult,
      CaptureResult captureResult
  ) {
    this.authorizationResult = authorizationResult;
    this.captureResult = captureResult;
  }

  public AuthorizationResult getAuthorizationResult() {
    return authorizationResult;
  }

  public CaptureResult getCaptureResult() {
    return captureResult;
  }
}
