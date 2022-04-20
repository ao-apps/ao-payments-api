/*
 * ao-payments-api - Payment processing API supporting multiple payment gateways.
 * Copyright (C) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2016, 2018, 2019, 2020, 2021, 2022  AO Industries, Inc.
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

/**
 * Encapsulates all the details of one transaction, including request, credit card details, and response.
 *
 * @author  AO Industries, Inc.
 */
public class Transaction implements Cloneable {

  // Matches src/main/sql/com/aoapps/payments/Transaction.Status-type.sql
  public enum Status {
    PROCESSING,
    LOCAL_ERROR,
    IO_ERROR,
    GATEWAY_ERROR,
    AUTHORIZED,
    CAPTURED,
    DECLINED,
    HOLD,
    VOID,
    CHARGEBACK
    ;

    @Override
    public String toString() {
      return PACKAGE_RESOURCES.getMessage("Transaction.Status."+name());
    }
  }

  private String providerId;
  private String persistenceUniqueId;
  private String groupName;
  private TransactionRequest transactionRequest;
  private CreditCard creditCard;
  private long authorizationTime;
  private String authorizationPrincipalName;
  private AuthorizationResult authorizationResult;
  private long captureTime; // TODO: 3.0: Make nullable Long
  private String capturePrincipalName;
  private CaptureResult captureResult;
  private long voidTime; // TODO: 3.0: Make nullable Long
  private String voidPrincipalName;
  private VoidResult voidResult;
  private Status status;

  /**
   * Creates an empty Transaction.  The values should be set using the appropriate setter methods.
   */
  public Transaction() {
    // Do nothing
  }

  /**
   * Creates a Transaction providing all of the details.
   */
  @SuppressWarnings("OverridableMethodCallInConstructor")
  public Transaction(
    String providerId,
    String persistenceUniqueId,
    String groupName,
    TransactionRequest transactionRequest,
    CreditCard creditCard,
    long authorizationTime,
    String authorizationPrincipalName,
    AuthorizationResult authorizationResult,
    long captureTime, // TODO: 3.0: Make nullable Long
    String capturePrincipalName,
    CaptureResult captureResult,
    long voidTime, // TODO: 3.0: Make nullable Long
    String voidPrincipalName,
    VoidResult voidResult,
    Status status
  ) {
    setProviderId(providerId);
    setPersistenceUniqueId(persistenceUniqueId);
    setGroupName(groupName);
    setTransactionRequest(transactionRequest);
    setCreditCard(creditCard);
    setAuthorizationTime(authorizationTime);
    setAuthorizationPrincipalName(authorizationPrincipalName);
    setAuthorizationResult(authorizationResult);
    setCaptureTime(captureTime);
    setCapturePrincipalName(capturePrincipalName);
    setCaptureResult(captureResult);
    setVoidTime(voidTime);
    setVoidPrincipalName(voidPrincipalName);
    setVoidResult(voidResult);
    setStatus(status);
  }

  @Override
  @SuppressWarnings("CloneDoesntCallSuperClone")
  public Transaction clone() throws CloneNotSupportedException {
    return new Transaction(
      providerId,
      persistenceUniqueId,
      groupName,
      transactionRequest == null ? null : transactionRequest.clone(),
      creditCard == null ? null : creditCard.clone(),
      authorizationTime,
      authorizationPrincipalName,
      authorizationResult == null ? null : authorizationResult.clone(),
      captureTime,
      capturePrincipalName,
      captureResult == null ? null : captureResult.clone(),
      voidTime,
      voidPrincipalName,
      voidResult == null ? null : voidResult.clone(),
      status
    );
  }

  public String getProviderId() {
    return providerId;
  }

  public void setProviderId(String providerId) {
    this.providerId = providerId;
  }

  public String getPersistenceUniqueId() {
    return persistenceUniqueId;
  }

  public void setPersistenceUniqueId(String persistenceUniqueId) {
    this.persistenceUniqueId = persistenceUniqueId;
  }

  public String getGroupName() {
    return groupName;
  }

  public void setGroupName(String groupName) {
    this.groupName = groupName;
  }

  public TransactionRequest getTransactionRequest() {
    return transactionRequest;
  }

  public void setTransactionRequest(TransactionRequest transactionRequest) {
    this.transactionRequest = transactionRequest;
  }

  public CreditCard getCreditCard() {
    return creditCard;
  }

  public void setCreditCard(CreditCard creditCard) {
    this.creditCard = creditCard;
  }

  public long getAuthorizationTime() {
    return authorizationTime;
  }

  public void setAuthorizationTime(long authorizationTime) {
    this.authorizationTime = authorizationTime;
  }

  public String getAuthorizationPrincipalName() {
    return authorizationPrincipalName;
  }

  public void setAuthorizationPrincipalName(String authorizationPrincipalName) {
    this.authorizationPrincipalName = authorizationPrincipalName;
  }

  public AuthorizationResult getAuthorizationResult() {
    return authorizationResult;
  }

  public void setAuthorizationResult(AuthorizationResult authorizationResult) {
    this.authorizationResult = authorizationResult;
  }

  // TODO: 3.0: Make nullable Long
  public long getCaptureTime() {
    return captureTime;
  }

  // TODO: 3.0: Make nullable Long
  public void setCaptureTime(long captureTime) {
    this.captureTime = captureTime;
  }

  public String getCapturePrincipalName() {
    return capturePrincipalName;
  }

  public void setCapturePrincipalName(String capturePrincipalName) {
    this.capturePrincipalName = capturePrincipalName;
  }

  public CaptureResult getCaptureResult() {
    return captureResult;
  }

  public void setCaptureResult(CaptureResult captureResult) {
    this.captureResult = captureResult;
  }

  // TODO: 3.0: Make nullable Long
  public long getVoidTime() {
    return voidTime;
  }

  // TODO: 3.0: Make nullable Long
  public void setVoidTime(long voidTime) {
    this.voidTime = voidTime;
  }

  public String getVoidPrincipalName() {
    return voidPrincipalName;
  }

  public void setVoidPrincipalName(String voidPrincipalName) {
    this.voidPrincipalName = voidPrincipalName;
  }

  public VoidResult getVoidResult() {
    return voidResult;
  }

  public void setVoidResult(VoidResult voidResult) {
    this.voidResult = voidResult;
  }

  public Status getStatus() {
    return status;
  }

  public void setStatus(Status status) {
    this.status = status;
  }
}
