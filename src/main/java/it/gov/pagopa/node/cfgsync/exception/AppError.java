package it.gov.pagopa.node.cfgsync.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;


@Getter
public enum AppError {
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
            "Something was wrong"),
    CACHE_UNPROCESSABLE(HttpStatus.UNPROCESSABLE_ENTITY, "Cache not ready to be saved",
        "Problem to retrieve cache"),
    SERVICE_DISABLED(
          HttpStatus.BAD_REQUEST,
      "Target service disabled",
              "Target service %s disabled");

  public final HttpStatus httpStatus;
  public final String title;
  public final String details;


  AppError(HttpStatus httpStatus, String title, String details) {
    this.httpStatus = httpStatus;
    this.title = title;
    this.details = details;
  }
}


