package it.gov.pagopa.node.cfgsync.exception;

import it.gov.pagopa.node.cfgsync.model.ProblemJson;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;


/**
 * All Exceptions are handled by this class
 */
@ControllerAdvice
@Slf4j
public class ErrorHandler extends ResponseEntityExceptionHandler {

  public static final String INTERNAL_SERVER_ERROR = "INTERNAL SERVER ERROR";

  /**
   * Handle if a {@link AppException} is raised
   *
   * @param ex      {@link AppException} exception raised
   * @param request from frontend
   * @return a {@link ProblemJson} as response with the cause and with an appropriated HTTP status
   */
  @ExceptionHandler({AppException.class})
  public ResponseEntity<ProblemJson> handleAppException(final AppException ex,
      final WebRequest request) {
    if (ex.getCause() != null) {
      log.warn("App Exception raised: " + ex.getMessage() + "\nCause of the App Exception: ",
          ex.getCause());
    } else {
      log.warn("App Exception raised: {}", ex.getMessage());
      log.debug("Trace error: ", ex);
    }
    var errorResponse = ProblemJson.builder()
        .status(ex.getHttpStatus().value())
        .title(ex.getTitle())
        .detail(ex.getMessage())
        .build();
    return new ResponseEntity<>(errorResponse, ex.getHttpStatus());
  }


  /**
   * Handle if a {@link Exception} is raised
   *
   * @param ex      {@link Exception} exception raised
   * @param request from frontend
   * @return a {@link ProblemJson} as response with the cause and with 500 as HTTP status
   */
  @ExceptionHandler({Exception.class})
  public ResponseEntity<ProblemJson> handleGenericException(final Exception ex,
      final WebRequest request) {
    log.error("Generic Exception raised:", ex);
    var errorResponse = ProblemJson.builder()
        .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
        .title(INTERNAL_SERVER_ERROR)
        .detail(ex.getMessage())
        .build();
    return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
  }
}
