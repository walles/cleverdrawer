CREATE TABLE statistics(
   id           INTEGER PRIMARY KEY NOT NULL,
   launch_count INTEGER DEFAULT 0,
   first_launch DATETIME NOT NULL,
   most_recent_launch DATETIME NOT NULL
);
