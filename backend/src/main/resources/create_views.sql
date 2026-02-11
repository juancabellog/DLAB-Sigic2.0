-- Vista para proyectos con descripción traducida
CREATE OR REPLACE VIEW v_proyectos AS
SELECT 
    p.codigo,
    p.idDescripcion,
    COALESCE(t_us.valor, t_es.valor, p.idDescripcion) as descripcion,
    p.fechaInicio,
    p.fechaTermino,
    p.codigoExterno,
    p.tipoFinanciamiento,
    p.realizaCon,
    COALESCE(COUNT(pp.producto_id), 0) as total_productos
FROM proyectos p
LEFT JOIN textos t_us ON p.idDescripcion = t_us.id AND t_us.idioma = 'us' AND t_us.idTipoTexto = 2
LEFT JOIN textos t_es ON p.idDescripcion = t_es.id AND t_es.idioma = 'es' AND t_es.idTipoTexto = 2
LEFT JOIN producto_proyecto pp ON p.codigo = pp.proyecto_codigo
GROUP BY p.codigo, p.idDescripcion, t_us.valor, t_es.valor, p.fechaInicio, p.fechaTermino, 
         p.codigoExterno, p.tipoFinanciamiento, p.realizaCon;

-- Vista para tipo_participacion con descripción traducida
CREATE OR REPLACE VIEW v_tipo_participacion AS
SELECT 
    tp.id,
    tp.idDescripcion,
    COALESCE(t_us.valor, t_es.valor, tp.idDescripcion) as descripcion,
    tp.idTipoProducto,
    COALESCE(tp_prod_us.valor, tp_prod_es.valor, 'Not specified') as tipo_producto_nombre
FROM tipo_participacion tp
LEFT JOIN textos t_us ON tp.idDescripcion = t_us.id AND t_us.idioma = 'us' AND t_us.idTipoTexto = 2
LEFT JOIN textos t_es ON tp.idDescripcion = t_es.id AND t_es.idioma = 'es' AND t_es.idTipoTexto = 2
LEFT JOIN tipo_producto tp_prod ON tp.idTipoProducto = tp_prod.id
LEFT JOIN textos tp_prod_us ON tp_prod.idDescripcion = tp_prod_us.id AND tp_prod_us.idioma = 'us' AND tp_prod_us.idTipoTexto = 2
LEFT JOIN textos tp_prod_es ON tp_prod.idDescripcion = tp_prod_es.id AND tp_prod_es.idioma = 'es' AND tp_prod_es.idTipoTexto = 2;

-- Vista para tipo_producto con descripción traducida
CREATE OR REPLACE VIEW v_tipo_producto AS
SELECT 
    tp.id,
    tp.idDescripcion,
    COALESCE(t_us.valor, t_es.valor, tp.idDescripcion) as descripcion
FROM tipo_producto tp
LEFT JOIN textos t_us ON tp.idDescripcion = t_us.id AND t_us.idioma = 'us' AND t_us.idTipoTexto = 2
LEFT JOIN textos t_es ON tp.idDescripcion = t_es.id AND t_es.idioma = 'es' AND t_es.idTipoTexto = 2;