INSERT INTO roles (name, description, active) VALUES
('CLIENTE', 'Rol para usuarios clientes', true),
('PRESTADOR', 'Rol para prestadores de servicios', true),
('ADMIN', 'Rol para administradores del sistema', true)
ON CONFLICT (name) DO NOTHING;
