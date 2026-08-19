-- Books scientific product (producto + book). idTipoProducto = 20.
-- Note: firstPage/lastPage/year use INT/SMALLINT (not TINYINT) so years and page numbers fit.

CREATE TABLE IF NOT EXISTS `BookType` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `idDescripcion` varchar(200) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;

INSERT INTO BookType (id, idDescripcion)
SELECT 1, 'Whole' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM BookType WHERE id = 1);

INSERT INTO BookType (id, idDescripcion)
SELECT 2, 'Chapter' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM BookType WHERE id = 2);

CREATE TABLE IF NOT EXISTS `book` (
  `id` bigint NOT NULL,
  `idBookType` bigint NOT NULL,
  `chapterTitle` tinytext,
  `firstPage` int NOT NULL,
  `lastPage` int NOT NULL,
  `editorialCityCountry` tinytext,
  `year` smallint NOT NULL,
  `ISBN` tinytext,
  PRIMARY KEY (`id`),
  UNIQUE KEY `XPKBook` (`id`),
  KEY `BookType_Book` (`idBookType`),
  CONSTRAINT `Producto_Book` FOREIGN KEY (`id`) REFERENCES `producto` (`id`),
  CONSTRAINT `BookType_Book` FOREIGN KEY (`idBookType`) REFERENCES `BookType` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;

-- Product type for scientific product Books is fixed: idTipoProducto = 20.
-- Ensure a row with id=20 exists in the product-type catalog / view your DB uses
-- (v_tipo_producto / tipoproducto). Insert with your environment's standard text-code process if needed.
