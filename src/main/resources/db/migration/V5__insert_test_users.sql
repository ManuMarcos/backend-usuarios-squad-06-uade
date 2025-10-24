INSERT INTO users (
    user_id, email, first_name, last_name, phone_number, dni, role_id,
    active, created_at, updated_at
) VALUES
(1, 'prestador_test@gmail.com', 'Test', 'Prestador', '1123456789', '12345678', 2,
    TRUE, '2025-10-20T23:46:30.125154', '2025-10-20T23:46:30.125234'),

(2, 'cliente_test@gmail.com', 'Test', 'Cliente', '1134567890', '12345678', 1,
    TRUE, '2025-10-20T23:46:45.040525', '2025-10-20T23:46:45.040548'),

(3, 'cliente_admin@gmail.com', 'Test', 'Cliente Admin', '1145678901', '12345678', 3,
    TRUE, '2025-10-20T23:47:05.103141', '2025-10-20T23:47:05.103166'),

(4, 'prestador_admin@gmail.com', 'Test', 'Prestador Admin', '1156789012', '12345678', 3,
    TRUE, '2025-10-20T23:47:17.932639', '2025-10-20T23:47:17.932669');