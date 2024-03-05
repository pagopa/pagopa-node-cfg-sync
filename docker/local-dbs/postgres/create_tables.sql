CREATE TABLE "cache" (
	id varchar(20) NOT NULL,
	"time" timestamp NOT NULL,
	"cache" bytea NOT NULL,
	"version" varchar(32) NULL
);