-- Fix Organization of Scientific Events products incorrectly stored with idTipoProducto = 5.
-- Correct value is always 15 (ORGANIZACION_EVENTOS_CIENTIFICOS).

UPDATE producto p
INNER JOIN organizacioneventoscientificos o ON o.id = p.id
SET p.idTipoProducto = 15
WHERE p.idTipoProducto IS NULL OR p.idTipoProducto <> 15;
