CREATE SCHEMA IF NOT EXISTS logistica_demo;

CREATE TABLE logistica_demo.usuarios (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(120) NOT NULL,
    full_name VARCHAR(120) NOT NULL,
    role VARCHAR(20) NOT NULL,
    active BOOLEAN NOT NULL,
    created_by VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_by VARCHAR(50) NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE logistica_demo.proveedores (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    active BOOLEAN NOT NULL,
    created_by VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_by VARCHAR(50) NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE logistica_demo.almacenes (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    active BOOLEAN NOT NULL,
    created_by VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_by VARCHAR(50) NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE logistica_demo.items (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    unit_measure VARCHAR(20) NOT NULL,
    active BOOLEAN NOT NULL,
    created_by VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_by VARCHAR(50) NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE logistica_demo.requerimientos (
    id BIGSERIAL PRIMARY KEY,
    numero VARCHAR(20) UNIQUE,
    descripcion VARCHAR(250) NOT NULL,
    proveedor_id BIGINT NOT NULL,
    moneda VARCHAR(20) NOT NULL,
    estado VARCHAR(20) NOT NULL,
    created_by VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_by VARCHAR(50) NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_requerimientos_proveedor
        FOREIGN KEY (proveedor_id) REFERENCES logistica_demo.proveedores(id)
);

CREATE TABLE logistica_demo.requerimiento_detalles (
    id BIGSERIAL PRIMARY KEY,
    requerimiento_id BIGINT NOT NULL,
    item_id BIGINT NOT NULL,
    almacen_id BIGINT NOT NULL,
    cantidad INT NOT NULL,
    precio_unitario_estimado DECIMAL(18, 2) NOT NULL,
    subtotal_linea DECIMAL(18, 2) NOT NULL,
    CONSTRAINT fk_req_det_requerimiento
        FOREIGN KEY (requerimiento_id) REFERENCES logistica_demo.requerimientos(id),
    CONSTRAINT fk_req_det_item
        FOREIGN KEY (item_id) REFERENCES logistica_demo.items(id),
    CONSTRAINT fk_req_det_almacen
        FOREIGN KEY (almacen_id) REFERENCES logistica_demo.almacenes(id)
);

CREATE TABLE logistica_demo.aprobaciones (
    id BIGSERIAL PRIMARY KEY,
    requerimiento_id BIGINT NOT NULL,
    accion VARCHAR(20) NOT NULL,
    comentario VARCHAR(250),
    decision_at TIMESTAMP NOT NULL,
    created_by VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_by VARCHAR(50) NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_aprobacion_requerimiento
        FOREIGN KEY (requerimiento_id) REFERENCES logistica_demo.requerimientos(id)
);

CREATE TABLE logistica_demo.ordenes_compra (
    id BIGSERIAL PRIMARY KEY,
    numero VARCHAR(20) UNIQUE,
    requerimiento_id BIGINT NOT NULL UNIQUE,
    proveedor_id BIGINT NOT NULL,
    moneda VARCHAR(3) NOT NULL,
    tipo_cambio DECIMAL(18, 4) NOT NULL,
    generated_at TIMESTAMP NOT NULL,
    subtotal DECIMAL(18, 2) NOT NULL,
    igv DECIMAL(18, 2) NOT NULL,
    total DECIMAL(18, 2) NOT NULL,
    created_by VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_by VARCHAR(50) NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_oc_requerimiento
        FOREIGN KEY (requerimiento_id) REFERENCES logistica_demo.requerimientos(id),
    CONSTRAINT fk_oc_proveedor
        FOREIGN KEY (proveedor_id) REFERENCES logistica_demo.proveedores(id)
);

CREATE TABLE logistica_demo.orden_compra_detalles (
    id BIGSERIAL PRIMARY KEY,
    orden_compra_id BIGINT NOT NULL,
    item_id BIGINT NOT NULL,
    almacen_id BIGINT NOT NULL,
    cantidad INT NOT NULL,
    precio_unitario DECIMAL(18, 2) NOT NULL,
    subtotal_linea DECIMAL(18, 2) NOT NULL,
    CONSTRAINT fk_oc_det_orden
        FOREIGN KEY (orden_compra_id) REFERENCES logistica_demo.ordenes_compra(id),
    CONSTRAINT fk_oc_det_item
        FOREIGN KEY (item_id) REFERENCES logistica_demo.items(id),
    CONSTRAINT fk_oc_det_almacen
        FOREIGN KEY (almacen_id) REFERENCES logistica_demo.almacenes(id)
);
