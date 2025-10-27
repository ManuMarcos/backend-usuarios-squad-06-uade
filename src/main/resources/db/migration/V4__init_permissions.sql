-- =====================================================
-- MIGRACIÓN COMPLETA DE PERMISOS POR MÓDULOS
-- =====================================================

-- Insertar permisos para módulo CATÁLOGO PRESTADORES
INSERT INTO permissions (module_code, permission_name, description, active) VALUES
('module_catalog', 'ZONA_VER', 'Ver zonas', true),
('module_catalog', 'ZONA_EDITAR', 'Editar zonas', true),
('module_catalog', 'ZONA_ELIMINAR', 'Eliminar zonas', true),
('module_catalog', 'HABILIDAD_VER', 'Ver habilidades', true),
('module_catalog', 'HABILIDAD_EDITAR', 'Editar habilidades', true),
('module_catalog', 'HABILIDAD_ELIMINAR', 'Eliminar habilidades', true),
('module_catalog', 'HABILIDAD_CREAR', 'Crear habilidades', true),
('module_catalog', 'PEDIDO_VER', 'Ver pedidos', true),
('module_catalog', 'PEDIDO_EDITAR', 'Editar pedidos', true),
('module_catalog', 'PEDIDO_CREAR', 'Crear pedidos', true),
('module_catalog', 'PEDIDO_ELIMINAR', 'Eliminar pedidos', true),
('module_catalog', 'RUBRO_EDITAR', 'Editar rubros', true),
('module_catalog', 'RUBRO_CREAR', 'Crear rubros', true),
('module_catalog', 'COTIZACION_CREAR', 'Crear cotizaciones', true),
('module_catalog', 'CALIFICACION_VER', 'Ver calificaciones', true),
('module_catalog', 'NOTIFICACION_VER', 'Ver notificaciones', true),
('module_catalog', 'USUARIO_EDITAR', 'Editar usuarios', true),
('module_catalog', 'USUARIO_CREAR', 'Crear usuarios', true);

-- Insertar permisos para módulo PAGOS
INSERT INTO permissions (module_code, permission_name, description, active) VALUES
('module_payments', 'PAGO_ACEPTAR', 'Aceptar pagos', true),
('module_payments', 'PAGO_RECHAZAR', 'Rechazar pagos', true),
('module_payments', 'PAGO_VER', 'Ver pagos', true);

-- Insertar permisos para módulo BÚSQUEDAS
INSERT INTO permissions (module_code, permission_name, description, active) VALUES
('module_search', 'USUARIO_CREAR', 'Crear usuarios', true), -- ✅ Mismo nombre, diferente módulo
('module_search', 'USUARIO_VER', 'Ver usuarios', true),
('module_search', 'USUARIO_MODIFICAR', 'Modificar usuarios', true),
('module_search', 'USUARIO_ELIMINAR', 'Eliminar usuarios', true),
('module_search', 'SOLICITUD_CREAR', 'Crear solicitudes', true),
('module_search', 'SOLICITUD_ACTUALIZAR', 'Actualizar solicitudes', true),
('module_search', 'SOLICITUD_CANCELAR', 'Cancelar solicitudes', true),
('module_search', 'COTIZACION_ACEPTAR', 'Aceptar cotizaciones', true),
('module_search', 'COTIZACION_RECHAZAR', 'Rechazar cotizaciones', true),
('module_search', 'REVIEW_CREAR', 'Crear reseñas', true);

-- Insertar permisos para módulo AGENDA
INSERT INTO permissions (module_code, permission_name, description, active) VALUES
('module_agenda', 'SOLICITUD_VER', 'Ver solicitudes', true),
('module_agenda', 'COTIZACION_VER', 'Ver cotizaciones', true),
('module_agenda', 'SOLICITUD_CREAR', 'Crear solicitudes', true);

-- Insertar permisos para módulo MÉTRICAS
INSERT INTO permissions (module_code, permission_name, description, active) VALUES
('module_metrics', 'METRICS_VER', 'Ver métricas', true),
('module_metrics', 'METRICS_EXPORTAR', 'Exportar métricas', true),
('module_metrics', 'REPORTES_GENERAR', 'Generar reportes', true);

-- =====================================================
-- ASIGNACIÓN DE PERMISOS A ROLES
-- =====================================================

-- CLIENTE: Solo permisos de lectura básicos
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'CLIENTE'
AND p.permission_name IN (
    'ZONA_VER', 'HABILIDAD_VER', 'PEDIDO_VER', 'CALIFICACION_VER', 'NOTIFICACION_VER',
    'PAGO_VER', 'SOLICITUD_VER', 'COTIZACION_VER', 'METRICS_VER'
);

-- PRESTADOR: Permisos de gestión en catálogo, lectura en otros módulos
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'PRESTADOR'
AND p.permission_name IN (
    -- Catálogo: gestión completa
    'ZONA_VER', 'ZONA_EDITAR', 'ZONA_ELIMINAR',
    'HABILIDAD_VER', 'HABILIDAD_EDITAR', 'HABILIDAD_ELIMINAR', 'HABILIDAD_CREAR',
    'PEDIDO_VER', 'PEDIDO_EDITAR', 'PEDIDO_CREAR', 'PEDIDO_ELIMINAR',
    'RUBRO_EDITAR', 'RUBRO_CREAR',
    'COTIZACION_CREAR',
    'CALIFICACION_VER', 'NOTIFICACION_VER',
    'USUARIO_EDITAR', 'USUARIO_CREAR',
    -- Pagos: solo ver
    'PAGO_VER',
    -- Búsquedas: gestión de solicitudes y cotizaciones
    'SOLICITUD_CREAR', 'SOLICITUD_ACTUALIZAR', 'SOLICITUD_CANCELAR',
    'COTIZACION_ACEPTAR', 'COTIZACION_RECHAZAR',
    'REVIEW_CREAR',
    -- Agenda: gestión de solicitudes
    'SOLICITUD_VER', 'COTIZACION_VER', 'SOLICITUD_CREAR',
    -- Métricas: solo lectura
    'METRICS_VER'
);

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'ADMIN'
AND p.active = true;