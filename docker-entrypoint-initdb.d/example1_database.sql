CREATE DATABASE accounts OWNER user1;

\connect accounts ;

CREATE TABLE events (
      sequence BIGSERIAL NOT NULL PRIMARY KEY,
      event_type TEXT NOT NULL,
      event_payload JSON NOT NULL,
      is_private BOOLEAN, -- TODO lgpd, grpd
      state_type text NOT NULL,
      state_id UUID NOT NULL,
      version INTEGER NOT NULL,
      id UUID NOT NULL UNIQUE,
      causation_id UUID NOT NULL REFERENCES events (id),
      correlation_id UUID NOT NULL REFERENCES events (id),
      inserted_on TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
      UNIQUE (state_id, version)
     )
;

CREATE INDEX sequence_idx ON events using brin (sequence);
CREATE INDEX state_id ON events (state_id);
CREATE INDEX state_name ON events (state_type);
CREATE INDEX event_type ON events (event_type);

CREATE TABLE subscriptions (
   name TEXT PRIMARY KEY NOT NULL,
   sequence BIGINT
);

CREATE TABLE commands (
  state_id UUID NOT NULL,
  causation_id UUID NOT NULL,
  last_causation_id UUID NOT NULL,
  cmd_payload JSON NOT NULL,
  private_payload JSON -- TODO LGPD, GPDR
);

-- app data

INSERT INTO subscriptions (name, sequence) values ('accounts-view', 0);
INSERT INTO subscriptions (name, sequence) values ('transfers-view', 0);

-- read model

-- https://stackoverflow.com/questions/18169627/money-data-on-postgresql-using-java

CREATE TABLE accounts_view (
    id UUID NOT NULL PRIMARY KEY,
    cpf TEXT NOT NULL UNIQUE,
    name TEXT NOT NULL,
    balance NUMERIC DEFAULT 0.00
);

create index idx_cpf on accounts_view(cpf);

CREATE TABLE transfers_view (
    id UUID NOT NULL PRIMARY KEY,
    amount NUMERIC,
    from_acct_id UUID NOT NULL,
    to_acct_id UUID NOT NULL,
    pending BOOLEAN NOT NULL DEFAULT true,
    succeeded BOOLEAN,
    error_message TEXT
);

-- filtered index
create index idx_pending on transfers_view(pending) where pending;
