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
 * Encapsulates the results of an authorization.
 *
 * @see  MerchantServicesProvider#authorize
 *
 * @author  AO Industries, Inc.
 */
public class AuthorizationResult extends TransactionResult implements Cloneable {

  // Matches src/main/sql/com/aoapps/payments/AuthorizationResult.ApprovalResult-type.sql
  /**
   * The set of supported approval results.
   */
  public enum ApprovalResult {
    APPROVED,
    DECLINED,
    HOLD;

    /**
     * Gets the display value.
     */
    @Override
    public String toString() {
      return PACKAGE_RESOURCES.getMessage("AuthorizationResult.ApprovalResult." + name());
    }
  }

  // Matches src/main/sql/com/aoapps/payments/AuthorizationResult.DeclineReason-type.sql
  /**
   * The set of supported decline reasons.
   */
  public enum DeclineReason {
    NO_SPECIFIC,
    EXPIRED_CARD,
    PICK_UP_CARD,
    AVS_MISMATCH,
    CVV2_MISMATCH,
    FRAUD_DETECTED,
    BLOCKED_IP,
    MANUAL_REVIEW,
    INSUFFICIENT_FUNDS,
    MAX_SALE_EXCEEDED,
    MIN_SALE_NOT_MET,
    VOLUME_EXCEEDED_1_DAY,
    USAGE_EXCEEDED_1_DAY,
    VOLUME_EXCEEDED_3_DAYS,
    USAGE_EXCEEDED_3_DAYS,
    VOLUME_EXCEEDED_15_DAYS,
    USAGE_EXCEEDED_15_DAYS,
    VOLUME_EXCEEDED_30_DAYS,
    USAGE_EXCEEDED_30_DAYS,
    STOLEN_OR_LOST_CARD,
    AVS_FAILURE,
    NOT_PROVIDED,
    UNKNOWN;

    /**
     * Gets the display value.
     */
    @Override
    public String toString() {
      return PACKAGE_RESOURCES.getMessage("AuthorizationResult.DeclineReason." + name());
    }
  }

  // Matches src/main/sql/com/aoapps/payments/AuthorizationResult.ReviewReason-type.sql
  /**
   * The set of supported review reasons.
   */
  public enum ReviewReason {
    RISK_MANAGEMENT,
    ACCEPTED_MERCHANT_REVIEW,
    ACCEPTED_AUTHORIZED_MERCHANT_REVIEW;

    /**
     * Gets the display value.
     */
    @Override
    public String toString() {
      return PACKAGE_RESOURCES.getMessage("AuthorizationResult.ReviewReason." + name());
    }
  }

  // Matches src/main/sql/com/aoapps/payments/AuthorizationResult.CvvResult-type.sql
  /**
   * The set of supported card verification results.
   */
  public enum CvvResult {
    MATCH,
    NO_MATCH,
    NOT_PROCESSED,
    CVV2_NOT_PROVIDED_BY_MERCHANT,
    NOT_SUPPORTED_BY_ISSUER,
    UNKNOWN;

    /**
     * Gets the display value.
     */
    @Override
    public String toString() {
      return PACKAGE_RESOURCES.getMessage("AuthorizationResult.CvvResult." + name());
    }
  }

  // Matches src/main/sql/com/aoapps/payments/AuthorizationResult.AvsResult-type.sql
  /**
   * The set of supported address verification results.
   */
  public enum AvsResult {
    ADDRESS_NOT_PROVIDED,
    ADDRESS_Y_ZIP_9,
    ADDRESS_Y_ZIP_5,
    ADDRESS_Y_ZIP_N,
    ADDRESS_N_ZIP_9,
    ADDRESS_N_ZIP_5,
    ADDRESS_N_ZIP_N,
    UNAVAILABLE,
    RETRY,
    ERROR,
    SERVICE_NOT_SUPPORTED,
    NON_US_CARD,
    NOT_APPLICABLE,
    UNKNOWN;

    /**
     * Gets the display value.
     */
    @Override
    public String toString() {
      return PACKAGE_RESOURCES.getMessage("AuthorizationResult.AvsResult." + name());
    }
  }

  private final TokenizedCreditCard tokenizedCreditCard;
  private final String providerApprovalResult;
  private final ApprovalResult approvalResult;
  private final String providerDeclineReason;
  private final DeclineReason declineReason;
  private final String providerReviewReason;
  private final ReviewReason reviewReason;
  private final String providerCvvResult;
  private final CvvResult cvvResult;
  private final String providerAvsResult;
  private final AvsResult avsResult;
  private final String approvalCode;

  /**
   * Creates a new authorization result.
   */
  public AuthorizationResult(
      String providerId,
      CommunicationResult communicationResult,
      String providerErrorCode,
      ErrorCode errorCode,
      String providerErrorMessage,
      String providerUniqueId,
      TokenizedCreditCard tokenizedCreditCard,
      String providerApprovalResult,
      ApprovalResult approvalResult,
      String providerDeclineReason,
      DeclineReason declineReason,
      String providerReviewReason,
      ReviewReason reviewReason,
      String providerCvvResult,
      CvvResult cvvResult,
      String providerAvsResult,
      AvsResult avsResult,
      String approvalCode
  ) {
    super(
        providerId,
        communicationResult,
        providerErrorCode,
        errorCode,
        providerErrorMessage,
        providerUniqueId
    );
    this.tokenizedCreditCard = tokenizedCreditCard;
    this.providerApprovalResult = providerApprovalResult;
    this.approvalResult = approvalResult;
    this.providerDeclineReason = providerDeclineReason;
    this.declineReason = declineReason;
    this.providerReviewReason = providerReviewReason;
    this.reviewReason = reviewReason;
    this.providerCvvResult = providerCvvResult;
    this.cvvResult = cvvResult;
    this.providerAvsResult = providerAvsResult;
    this.avsResult = avsResult;
    this.approvalCode = approvalCode;
  }

  /**
   * Creates a new authorization result.
   *
   * @deprecated  Please use {@link #AuthorizationResult(java.lang.String, com.aoapps.payments.TransactionResult.CommunicationResult, java.lang.String, com.aoapps.payments.TransactionResult.ErrorCode, java.lang.String, java.lang.String, com.aoapps.payments.TokenizedCreditCard, java.lang.String, com.aoapps.payments.AuthorizationResult.ApprovalResult, java.lang.String, com.aoapps.payments.AuthorizationResult.DeclineReason, java.lang.String, com.aoapps.payments.AuthorizationResult.ReviewReason, java.lang.String, com.aoapps.payments.AuthorizationResult.CvvResult, java.lang.String, com.aoapps.payments.AuthorizationResult.AvsResult, java.lang.String)}
   */
  @Deprecated(forRemoval = true)
  public AuthorizationResult(
      String providerId,
      CommunicationResult communicationResult,
      String providerErrorCode,
      ErrorCode errorCode,
      String providerErrorMessage,
      String providerUniqueId,
      String providerApprovalResult,
      ApprovalResult approvalResult,
      String providerDeclineReason,
      DeclineReason declineReason,
      String providerReviewReason,
      ReviewReason reviewReason,
      String providerCvvResult,
      CvvResult cvvResult,
      String providerAvsResult,
      AvsResult avsResult,
      String approvalCode
  ) {
    this(
        providerId,
        communicationResult,
        providerErrorCode,
        errorCode,
        providerErrorMessage,
        providerUniqueId,
        null, // tokenizedCreditCard
        providerApprovalResult,
        approvalResult,
        providerDeclineReason,
        declineReason,
        providerReviewReason,
        reviewReason,
        providerCvvResult,
        cvvResult,
        providerAvsResult,
        avsResult,
        approvalCode
    );
  }

  @Override
  public AuthorizationResult clone() throws CloneNotSupportedException {
    return (AuthorizationResult) super.clone();
  }

  /**
   * Gets the tokenized card used for this authorization.
   *
   * @return  The tokenized card or {@code null} when replacement not supported
   */
  public TokenizedCreditCard getTokenizedCreditCard() {
    return tokenizedCreditCard;
  }

  /**
   * Gets the provider-specific approval response.
   */
  public String getProviderApprovalResult() {
    return providerApprovalResult;
  }

  /**
   * Gets the provider-neutral approval response code.
   */
  public ApprovalResult getApprovalResult() {
    return approvalResult;
  }

  /**
   * Gets the provider-specific decline reason.
   */
  public String getProviderDeclineReason() {
    return providerDeclineReason;
  }

  /**
   * Gets the provider-neutral decline reason.
   */
  public DeclineReason getDeclineReason() {
    return declineReason;
  }

  /**
   * Gets the provider-specific review reason.
   */
  public String getProviderReviewReason() {
    return providerReviewReason;
  }

  /**
   * Gets the provider-neutral review reason.
   */
  public ReviewReason getReviewReason() {
    return reviewReason;
  }

  /**
   * Gets the provider-specific CVV result.
   */
  public String getProviderCvvResult() {
    return providerCvvResult;
  }

  /**
   * Gets the provider-neutral CVV result.
   */
  public CvvResult getCvvResult() {
    return cvvResult;
  }

  /**
   * Gets the provider-specific AVS result.
   */
  public String getProviderAvsResult() {
    return providerAvsResult;
  }

  /**
   * Gets the provider-neutral AVS result.
   */
  public AvsResult getAvsResult() {
    return avsResult;
  }

  /**
   * Gets the approval code.
   */
  public String getApprovalCode() {
    return approvalCode;
  }
}
