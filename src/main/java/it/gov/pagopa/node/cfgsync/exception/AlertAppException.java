package it.gov.pagopa.node.cfgsync.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/** Custom exception with trigger details and last retry info */
@Getter
public class AlertAppException extends RuntimeException {

  private static final long serialVersionUID = -8908282614564857184L;

  public static final String EXCEPTION_ERROR_CODE = "errorCode";
  public static final String EXCEPTION_BLOB_CONTAINER = "blobContainer";
  public static final String EXCEPTION_BLOB_NAME = "blobName";
  public static final String EXCEPTION_BLOB_SESSION_ID = "blobSessionId";
  public static final String EXCEPTION_RETRY_INDEX = "retryIndex";
  private final String details;

  public AlertAppException(String message, Throwable cause, String details) {
    super(message, cause);
    this.details = details;
  }

  @Override
  public String toString() {
    String exceptionPrefix = "[ALERT][NODE_CFG_SYNC]";
    return String.format(
        "%s[%s]:details=%s,%nmessage=%s",
        exceptionPrefix, this.getClass(), details, super.toString());
  }

//  public static String getExceptionDetails(
//      String errorCode, String container, String blob, String sessionId, int retry) {
//    JsonMapper jsonMapper = new JsonMapper();
//
//    Map<String, Object> jsonDetails = new HashMap<>();
//    jsonDetails.put(EXCEPTION_ERROR_CODE, errorCode);
//    jsonDetails.put(EXCEPTION_BLOB_CONTAINER, container);
//    jsonDetails.put(EXCEPTION_BLOB_NAME, blob);
//    jsonDetails.put(EXCEPTION_BLOB_SESSION_ID, sessionId);
//    jsonDetails.put(EXCEPTION_RETRY_INDEX, retry);
//
//    try {
//      return jsonMapper.writeValueAsString(jsonDetails);
//    } catch (JsonProcessingException e) {
//      return "DETAILS_NOT_FOUND";
//    }
//  }
}
