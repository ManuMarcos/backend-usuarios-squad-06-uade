CREATE TABLE IF NOT EXISTS permissions (
  id BIGSERIAL PRIMARY KEY,
  module_code VARCHAR(100) NOT NULL,
  permission_name VARCHAR(150) NOT NULL,
  description VARCHAR(255),
  active BOOLEAN NOT NULL DEFAULT true
);

ALTER TABLE permissions ADD CONSTRAINT permissions_module_permission_unique
    UNIQUE (module_code, permission_name);

CREATE TABLE IF NOT EXISTS role_permissions (
  role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
  permission_id BIGINT NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
  PRIMARY KEY (role_id, permission_id)
);