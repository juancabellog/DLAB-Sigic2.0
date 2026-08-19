-- Awards scientific product (producto + award). idTipoProducto = 21.
-- Table is expected to exist; this script documents structure and product type note.

CREATE TABLE IF NOT EXISTS `award` (
  `id` bigint NOT NULL,
  `year` smallint NOT NULL,
  `idinstitucion` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `XPKAward` (`id`),
  KEY `Institucion_Award` (`idinstitucion`),
  CONSTRAINT `Producto_Award` FOREIGN KEY (`id`) REFERENCES `producto` (`id`),
  CONSTRAINT `Institucion_Award` FOREIGN KEY (`idinstitucion`) REFERENCES `Institucion` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;

-- Product type for Awards is fixed: idTipoProducto = 21.
-- Ensure a row with id=21 exists in the product-type catalog / view.
