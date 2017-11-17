CREATE TABLE statistics(
   id           INTEGER PRIMARY KEY NOT NULL,
   launch_count INTEGER NOT NULL,
   first_launch INTEGER NOT NULL, -- Unix time_t
   latest_launch INTEGER NOT NULL -- Unix time_t
);
