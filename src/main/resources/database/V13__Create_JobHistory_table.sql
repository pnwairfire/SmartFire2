CREATE TABLE job_history
(
   "name" character varying(100) NOT NULL,
   status character varying(100) NOT NULL,
   id integer NOT NULL,
   source_id integer NOT NULL,
   method character varying(100) NOT NULL,
   data_start_date timestamp without time zone NOT NULL,
   data_end_date timestamp without time zone NOT NULL,
   start_date timestamp without time zone NOT NULL,
   end_date timestamp without time zone NOT NULL,
   CONSTRAINT job_history_pkey PRIMARY KEY (id)
)
WITH (
  OIDS = FALSE
)
;