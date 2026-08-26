CREATE TABLE logistica_demo.requerimiento_estado_historial (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    requerimiento_id BIGINT NOT NULL,
    estado_anterior VARCHAR(20),
    estado_nuevo VARCHAR(20) NOT NULL,
    comentario VARCHAR(250),
    created_by VARCHAR(50) NOT NULL,
    created_at DATETIME2 NOT NULL,
    updated_by VARCHAR(50) NOT NULL,
    updated_at DATETIME2 NOT NULL,
    CONSTRAINT fk_historial_requerimiento
        FOREIGN KEY (requerimiento_id) REFERENCES logistica_demo.requerimientos(id)
);
