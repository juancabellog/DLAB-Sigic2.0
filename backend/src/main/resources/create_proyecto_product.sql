-- Catalog of project types (multi-select serialized as comma-separated IDs in proyecto.projectTypes).
-- Ensure table `proyecto` already exists with FK to producto.

CREATE TABLE IF NOT EXISTS `tipoproyecto` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `idDescripcion` varchar(200) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;

INSERT INTO tipoproyecto (idDescripcion)
SELECT 'Basic Research' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM tipoproyecto WHERE idDescripcion = 'Basic Research');

INSERT INTO tipoproyecto (idDescripcion)
SELECT 'Applied Research' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM tipoproyecto WHERE idDescripcion = 'Applied Research');

INSERT INTO tipoproyecto (idDescripcion)
SELECT 'Technological Development' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM tipoproyecto WHERE idDescripcion = 'Technological Development');

INSERT INTO tipoproyecto (idDescripcion)
SELECT 'Innovation' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM tipoproyecto WHERE idDescripcion = 'Innovation');

INSERT INTO tipoproyecto (idDescripcion)
SELECT 'Infrastructure' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM tipoproyecto WHERE idDescripcion = 'Infrastructure');

INSERT INTO tipoproyecto (idDescripcion)
SELECT 'Training / Human Capital' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM tipoproyecto WHERE idDescripcion = 'Training / Human Capital');

-- Ensure Fundingtype has Other (proyecto.idFundingtype is NOT NULL)
INSERT INTO fundingtype (idDescripcion)
SELECT 'Other' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM fundingtype WHERE LOWER(idDescripcion) = 'other');

-- Product type for scientific product Projects is fixed: idTipoProducto = 19.
-- No insert required if that row already exists in the product-type catalog.
