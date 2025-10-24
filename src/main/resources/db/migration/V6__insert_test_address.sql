INSERT INTO addresses (state, city, street, number, floor, apartment, user_id) VALUES
-- Usuario 1 (PRESTADOR)
('Buenos Aires', 'La Plata', 'Calle 12', '845', '1', 'A', 1),
('Buenos Aires', 'Mar del Plata', 'Avenida Colón', '1550', NULL, NULL, 1),

-- Usuario 2 (CLIENTE)
('Córdoba', 'Córdoba Capital', 'Bv. San Juan', '920', '2', 'B', 2),
('Córdoba', 'Villa Carlos Paz', 'San Martín', '178', NULL, NULL, 2),

-- Usuario 3 (ADMIN)
('Santa Fe', 'Rosario', 'Bv. Oroño', '2456', '3', 'C', 3),
('Santa Fe', 'Santa Fe', 'Avenida Freyre', '1630', NULL, NULL, 3),

-- Usuario 4 (ADMIN)
('Mendoza', 'Mendoza', 'San Martín', '620', '4', 'D', 4),
('Mendoza', 'San Rafael', 'Hipólito Yrigoyen', '1580', NULL, NULL, 4)
ON CONFLICT DO NOTHING;