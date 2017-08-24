set FOREIGN_KEY_CHECKS = 0;
drop table if exists worlds;
drop table if exists plots;
drop table if exists added_local;
drop table if exists added_global;
set FOREIGN_KEY_CHECKS  = 1;

create table worlds (
  world_id INT(32) AUTO_INCREMENT NOT NULL PRIMARY KEY,
  name VARCHAR(50),
  uid BINARY(16),
  UNIQUE KEY (uid)
);

create table plots (
  plot_id INT(64) AUTO_INCREMENT NOT NULL PRIMARY KEY,
  world_id INT(32) NOT NULL,
  idx INT(32) NOT NULL,
  idz INT(32) NOT NULL,
  owner_name VARCHAR(16),
  owner_uuid BINARY(16),
  opt_interact_inventory BOOLEAN NOT NULL DEFAULT FALSE,
  opt_interact_inputs BOOLEAN NOT NULL DEFAULT FALSE,
  UNIQUE KEY (world_id, idx, idz),
  FOREIGN KEY (world_id) REFERENCES worlds(world_id)
);

create table added_local (
  plot_id INT(64) NOT NULL,
  uuid BINARY(16) NOT NULL,
  flag BOOLEAN NOT NULL,
  FOREIGN KEY (plot_id) REFERENCES plots(plot_id),
  UNIQUE KEY (plot_id, uuid)
);

create table added_global (
  owner_uuid BINARY(16) NOT NULL,
  uuid BINARY(16) NOT NULL,
  flag BOOLEAN NOT NULL,
  UNIQUE KEY (owner_uuid, uuid)
);

show columns from plots;


