
\connect crabzilla ;

-- app data

INSERT INTO subscriptions (name, sequence) values ('accounts-view', 0);
INSERT INTO subscriptions (name, sequence) values ('transfers-view', 0);

-- read model

-- https://stackoverflow.com/questions/18169627/money-data-on-postgresql-using-java

CREATE TABLE accounts_view (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    cpf CHAR(36) NOT NULL UNIQUE,
    name VARCHAR(70) NOT NULL,
    balance NUMERIC DEFAULT 0.00
);

create index idx_cpf on accounts_view(cpf);

CREATE TABLE transfers_view (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    amount NUMERIC,
    from_acct_id VARCHAR(36) NOT NULL,
    to_acct_id VARCHAR(36) NOT NULL,
    pending BOOLEAN NOT NULL DEFAULT true,
    succeeded BOOLEAN,
    error_message TEXT
);

-- filtered index
create index idx_pending on transfers_view(pending) where pending;
