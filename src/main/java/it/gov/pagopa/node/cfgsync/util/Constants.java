package it.gov.pagopa.node.cfgsync.util;

public class Constants {

  private Constants() {
    throw new IllegalStateException("Constants class");
  }

  public static final String HEADER_REQUEST_ID = "X-Request-Id";

  public static final String HEADER_CACHE_ID = "X-CACHE-ID";
  public static final String HEADER_CACHE_TIMESTAMP = "X-CACHE-TIMESTAMP";
  public static final String HEADER_CACHE_VERSION = "X-CACHE-VERSION";
  public static final String PAGOPA_POSTGRES = "pagopa-postgres";
  public static final String NEXI_ORACLE = "nexi-oracle";
  public static final String NEXI_POSTGRES = "nexi-postgres";

}
