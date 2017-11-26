CREATE TABLE statistics(
   id            TEXT    PRIMARY KEY NOT NULL,
   launch_count  INTEGER NOT NULL,
   first_launch  INTEGER NOT NULL, -- Unix time_t
   latest_launch INTEGER NOT NULL -- Unix time_t
);

CREATE TABLE names_cache(
   id    TEXT  PRIMARY KEY NOT NULL,
   name  TEXT  NOT NULL
);
