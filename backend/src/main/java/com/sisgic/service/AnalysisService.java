package com.sisgic.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AnalysisService {

    private static final int RRHH_GRAPH = 1;
    private static final int LINEAINV_GRAPH = 3;
    private static final int CLUSTER_PRODUCTS_GRAPH = 4;
    private static final int CLUSTER_PRODUCTS_GRAPH_2 = 5;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Método principal que se invoca desde el frontend para recuperar los datos del grafo
     * Adaptado del método consultar de RelationShipRRHHService
     */
    public Map<String, Object> queryGraph(Map<String, Object> input) throws Exception {
        Integer type = (Integer) input.get("type");
        
        if (type == null) {
            throw new IllegalArgumentException("Type is required");
        }
        
        // Convertir periods y rrhhTypes a ArrayList si vienen como List
        List<Integer> periodsList = convertToList(input.get("periods"));
        List<Integer> rrhhTypesList = convertToList(input.get("rrhhTypes"));
        
        ArrayList<Integer> periods = periodsList != null ? new ArrayList<>(periodsList) : new ArrayList<>();
        ArrayList<Integer> rrhhTypes = rrhhTypesList != null ? new ArrayList<>(rrhhTypesList) : new ArrayList<>();
        
        // Actualizar input con ArrayList
        input.put("periods", periods);
        input.put("rrhhTypes", rrhhTypes);
        System.err.println("type: " + type);
        if (type == RRHH_GRAPH) {
            return getRRHHGraph(input);
        } else if (type == LINEAINV_GRAPH) {
            return getResearchLinesGraphs(input);
        } else if (type == CLUSTER_PRODUCTS_GRAPH) {
            return getClusterProductsGraphs(input);
        } else if (type == CLUSTER_PRODUCTS_GRAPH_2) {
            return getClusterProductsGraphs2(input);
        } else {
            throw new Exception("Graph type " + type + " not yet implemented");
        }
    }

    /**
     * Convierte un objeto a List<Integer>
     */
    @SuppressWarnings("unchecked")
    private List<Integer> convertToList(Object obj) {
        if (obj == null) return null;
        if (obj instanceof List) {
            return (List<Integer>) obj;
        }
        if (obj instanceof ArrayList) {
            return (ArrayList<Integer>) obj;
        }
        return null;
    }

    /**
     * Construye la condición SQL para tipos de RRHH usando JSON_CONTAINS
     */
    private String getSqlRRHHTypes(ArrayList<Integer> rrhhTypes, String alias) {
        if (rrhhTypes == null || rrhhTypes.isEmpty()) {
            return "";
        }
        StringBuilder types = new StringBuilder();
        String or = "";
        for (Integer type : rrhhTypes) {
            types.append(or);
            types.append(" json_contains(").append(alias).append(".tiposRRHH, '").append(type).append("') = 1");
            or = " or ";
        }
        return " and (" + types + ")";
    }

    /**
     * Obtiene los nodos del grafo de RRHH
     * Basado en ConsultaGraphNodes: select r.idrrhh, f_getFullName(r.idRRHH), rh.iniciales, rh.tiposRRHH, rh.extranjero, ifnull(i.hindex, 0), ifnull(i.citations, 0), ifnull(i.i10Index, 0), ifnull(i.maxCitations, 0)
     * from rrhh_producto r, publicacion p, rrhh rh left join view_indexsrrhh i on rh.id = i.idRRHH   
     * where r.idproducto = p.id and r.idRRHH = rh.id group by r.idrrhh
     */
    @SuppressWarnings("unchecked")
    private ArrayList<Map<String, Object>> getGraphNodes(ArrayList<Integer> periods, ArrayList<Integer> rrhhTypes) throws Exception {
        StringBuilder sql = new StringBuilder(
            "SELECT r.idRRHH as id, f_getFullName(r.idRRHH) as name, " +
            "rh.iniciales as initials, " +
            "rh.tiposRRHH, " +
            "IFNULL(i.citations, 0) as citations, " +
            "IFNULL(i.hindex, 0) as hindex " +
            "FROM rrhh_producto r, publicacion p, rrhh rh " +
            "LEFT JOIN view_indexsrrhh i ON rh.id = i.idRRHH " +
            "WHERE r.idProducto = p.id AND r.idRRHH = rh.id"
        );

        if (!periods.isEmpty()) {
            sql.append(" AND EXISTS (SELECT 1 FROM producto pr WHERE pr.id = r.idProducto AND pr.progressReport IN (");
            sql.append(periods.stream().map(String::valueOf).collect(Collectors.joining(",")));
            sql.append("))");
        }

        if (!rrhhTypes.isEmpty()) {
            sql.append(getSqlRRHHTypes(rrhhTypes, "rh"));
        }

        sql.append(" GROUP BY r.idRRHH");

        Query query = entityManager.createNativeQuery(sql.toString());
        List<Object[]> results = query.getResultList();
        
        ArrayList<Map<String, Object>> nodes = new ArrayList<>();
        for (Object[] row : results) {
            Map<String, Object> node = new HashMap<>();
            // Orden: id, name, initials, tiposRRHH, citations, hindex
            node.put("id", ((Number) row[0]).longValue());
            node.put("name", row[1] != null ? row[1].toString() : "");
            node.put("initials", row[2] != null ? row[2].toString() : "");
            
            // Parsear tiposRRHH desde JSON
            List<Integer> tiposList = new ArrayList<>();
            if (row[3] != null) {
                String tiposStr = row[3].toString();
                // Si es un array JSON, parsearlo
                if (tiposStr.startsWith("[") && tiposStr.endsWith("]")) {
                    tiposStr = tiposStr.substring(1, tiposStr.length() - 1);
                    if (!tiposStr.trim().isEmpty()) {
                        String[] tipos = tiposStr.split(",");
                        for (String tipo : tipos) {
                            try {
                                tiposList.add(Integer.parseInt(tipo.trim()));
                            } catch (NumberFormatException e) {
                                // Ignorar valores no numéricos
                            }
                        }
                    }
                } else if (!tiposStr.trim().isEmpty()) {
                    // Si es una cadena simple, intentar parsearla
                    try {
                        tiposList.add(Integer.parseInt(tiposStr.trim()));
                    } catch (NumberFormatException e) {
                        // Ignorar
                    }
                }
            }
            node.put("tiposRRHH", tiposList);
            node.put("citations", row[4] != null ? ((Number) row[4]).longValue() : 0);
            node.put("hindex", row[5] != null ? ((Number) row[5]).intValue() : 0);
            nodes.add(node);
        }
        
        return nodes;
    }

    /**
     * Obtiene los enlaces del grafo de RRHH
     * Basado en ConsultaRelationShipRRHH: select a.idRRHHFrom, a.idRRHHTo, a.idRelationShipType, sum(a.weight)
     * from RelationShipRRHH a 
     * group by a.idRRHHFrom, a.idRRHHTo, a.idRelationShipType
     */
    @SuppressWarnings("unchecked")
    private ArrayList<Map<String, Object>> getGraphLink(ArrayList<Integer> periods, ArrayList<Integer> rrhhTypes) throws Exception {
        StringBuilder sql = new StringBuilder(
            "SELECT a.idRRHHFrom as source, a.idRRHHTo as target, SUM(a.weight) as weight " +
            "FROM RelationShipRRHH a " +
            "WHERE 1=1"
        );

        // Filtrar por períodos si se especifican (necesitamos unir con productos)
        if (!periods.isEmpty()) {
            sql.append(" AND EXISTS (SELECT 1 FROM rrhh_producto rp " +
                      "INNER JOIN producto pr ON rp.idProducto = pr.id " +
                      "WHERE (rp.idRRHH = a.idRRHHFrom OR rp.idRRHH = a.idRRHHTo) " +
                      "AND pr.progressReport IN (");
            sql.append(periods.stream().map(String::valueOf).collect(Collectors.joining(",")));
            sql.append("))");
        }

        // Filtrar por tipos de RRHH
        if (!rrhhTypes.isEmpty()) {
            sql.append(" AND EXISTS (SELECT 1 FROM rrhh r1 WHERE r1.id = a.idRRHHFrom ");
            sql.append(getSqlRRHHTypes(rrhhTypes, "r1"));
            sql.append(") AND EXISTS (SELECT 1 FROM rrhh r2 WHERE r2.id = a.idRRHHTo ");
            sql.append(getSqlRRHHTypes(rrhhTypes, "r2"));
            sql.append(")");
        }

        sql.append(" GROUP BY a.idRRHHFrom, a.idRRHHTo");

        Query query = entityManager.createNativeQuery(sql.toString());
        List<Object[]> results = query.getResultList();
        
        ArrayList<Map<String, Object>> links = new ArrayList<>();
        for (Object[] row : results) {
            Map<String, Object> link = new HashMap<>();
            link.put("source", ((Number) row[0]).longValue());
            link.put("target", ((Number) row[1]).longValue());
            link.put("weight", row[2] != null ? ((Number) row[2]).intValue() : 1);
            links.add(link);
        }
        
        return links;
    }

    /**
     * Genera un grafo de colaboración entre investigadores (RRHH)
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> getRRHHGraph(Map<String, Object> input) throws Exception {
        ArrayList<Integer> periods = (ArrayList<Integer>) input.get("periods");
        ArrayList<Integer> rrhhTypes = (ArrayList<Integer>) input.get("rrhhTypes");
        
        if (periods == null) periods = new ArrayList<>();
        if (rrhhTypes == null) rrhhTypes = new ArrayList<>();
        
        Map<String, Object> result = new HashMap<>();
        ArrayList<Map<String, Object>> nodes = getGraphNodes(periods, rrhhTypes);
        ArrayList<Map<String, Object>> links = getGraphLink(periods, rrhhTypes);
        
        // Mapear IDs de nodos a índices
        Map<Long, Integer> idToIndex = new HashMap<>();
        int index = 0;
        for (Map<String, Object> node : nodes) {
            Long id = ((Number) node.get("id")).longValue();
            idToIndex.put(id, index++);
        }
        
        // Actualizar índices en los enlaces
        for (Map<String, Object> link : links) {
            Long sourceId = ((Number) link.get("source")).longValue();
            Long targetId = ((Number) link.get("target")).longValue();
            link.put("source", idToIndex.get(sourceId));
            link.put("target", idToIndex.get(targetId));
        }
        
        // Actualizar índices en los nodos (guardar el ID original en _id antes de cambiar id)
        index = 0;
        for (Map<String, Object> node : nodes) {
            Long originalId = ((Number) node.get("id")).longValue();
            node.put("_id", originalId);  // Guardar ID original
            node.put("id", index);        // Usar índice para el grafo
            index++;
        }
        
        result.put("nodes", nodes);
        result.put("links", links);
        
        return result;
    }

    /**
     * Crea una nueva categoría
     */
    private Map<String, Object> newCategory(int id, String name) {
        Map<String, Object> category = new HashMap<>();
        category.put("id", id);
        category.put("name", name);
        return category;
    }

    /**
     * Genera grafo de líneas de investigación
     * Basado en getResearchLinesGraphs de RelationShipRRHHService
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> getResearchLinesGraphs(Map<String, Object> input) throws Exception {
        String language = "us"; // Idioma por defecto
        String codigoCentro = "SIGIC"; // Código de centro
        
        System.out.println("=== getResearchLinesGraphs ===");
        System.out.println("codigoCentro: " + codigoCentro);
        System.out.println("language: " + language);
        
        // Verificar si hay datos en las tablas (sin filtro)
        try {
            Query testQuery = entityManager.createNativeQuery("SELECT COUNT(*) FROM LineaInvestigacion");
            Long countLineas = ((Number) testQuery.getSingleResult()).longValue();
            System.out.println("Total líneas de investigación en BD: " + countLineas);
            
            Query testQuery2 = entityManager.createNativeQuery("SELECT COUNT(*) FROM LineaInvestigacion WHERE codigoCentro = :codigoCentro");
            testQuery2.setParameter("codigoCentro", codigoCentro);
            Long countLineasFiltradas = ((Number) testQuery2.getSingleResult()).longValue();
            System.out.println("Líneas de investigación con codigoCentro='" + codigoCentro + "': " + countLineasFiltradas);
            
            // Verificar códigos de centro disponibles
            Query codigosQuery = entityManager.createNativeQuery("SELECT DISTINCT codigoCentro FROM LineaInvestigacion");
            List<Object> codigos = codigosQuery.getResultList();
            System.out.println("Códigos de centro disponibles: " + codigos);
        } catch (Exception e) {
            System.out.println("Error verificando datos: " + e.getMessage());
            e.printStackTrace();
        }
        
        // Consultar líneas de investigación
        // ConsultaLineaInvestigacion: select a.codigoCentro, a.idArea, a.id, a.idPadre, a.idDescripcion, a.idComentario, a.color, b.valor, c.valor
        // from LineaInvestigacion a left join Textos c on c.idTipoTexto = 2 and a.idComentario = c.codigoTexto and c.lenguaje = :lenguaje1
        // , Textos b where a.codigoCentro = :codigoCentro and a.idDescripcion = b.codigoTexto and b.idTipoTexto = 2 and b.lenguaje = :lenguaje2 order by a.id
        String sqlLineas = "SELECT a.codigoCentro, a.idArea, a.id, a.idPadre, a.idDescripcion, a.idComentario, a.color, b.valor as descripcion, c.valor as comentario " +
                           "FROM LineaInvestigacion a " +
                           "LEFT JOIN Textos c ON c.idTipoTexto = 2 AND a.idComentario = c.codigoTexto AND c.lenguaje = :language1 " +
                           ", Textos b " +
                           "WHERE a.codigoCentro = :codigoCentro AND a.idDescripcion = b.codigoTexto AND b.idTipoTexto = 2 AND b.lenguaje = :language2 " +
                           "ORDER BY a.id";
        
        Query queryLineas = entityManager.createNativeQuery(sqlLineas);
        queryLineas.setParameter("language1", language);
        queryLineas.setParameter("language2", language);
        queryLineas.setParameter("codigoCentro", codigoCentro);
        List<Object[]> lineasResults = queryLineas.getResultList();
        System.out.println("Lineas de investigación encontradas: " + lineasResults.size());
        
        // Consultar áreas
        // ConsultaArea: select a.codigoCentro, a.id, a.idDescripcion, a.idComentario, a.color, b.valor, c.valor
        // from Area a left join Textos c on c.idTipoTexto = 2 and a.idComentario = c.codigoTexto and c.lenguaje = :lenguaje1
        // , Textos b where a.codigoCentro = :codigoCentro and a.idDescripcion = b.codigoTexto and b.idTipoTexto = 2 and b.lenguaje = :lenguaje2 order by b.valor
        String sqlAreas = "SELECT a.codigoCentro, a.id, a.idDescripcion, a.idComentario, a.color, b.valor as descripcion, c.valor as comentario " +
                          "FROM Area a " +
                          "LEFT JOIN Textos c ON c.idTipoTexto = 2 AND a.idComentario = c.codigoTexto AND c.lenguaje = :language1 " +
                          ", Textos b " +
                          "WHERE a.codigoCentro = :codigoCentro AND a.idDescripcion = b.codigoTexto AND b.idTipoTexto = 2 AND b.lenguaje = :language2 " +
                          "ORDER BY b.valor";
        
        Query queryAreas = entityManager.createNativeQuery(sqlAreas);
        queryAreas.setParameter("language1", language);
        queryAreas.setParameter("language2", language);
        queryAreas.setParameter("codigoCentro", codigoCentro);
        List<Object[]> areasResults = queryAreas.getResultList();
        System.out.println("Áreas encontradas: " + areasResults.size());
        
        // Consultar keywords
        // Keyword: id, descripcion
        String sqlKeywords = "SELECT id, descripcion FROM Keyword ORDER BY id";
        Query queryKeywords = entityManager.createNativeQuery(sqlKeywords);
        List<Object[]> keywordsResults = queryKeywords.getResultList();
        System.out.println("Keywords encontrados: " + keywordsResults.size());
        
        // Consultar keyword-línea de investigación
        // keyword_lineainvestigacion: codigoCentro, idLineaInvestigacion, idKeyword
        String sqlKeywordLinea = "SELECT idKeyword, idLineaInvestigacion FROM keyword_lineainvestigacion WHERE codigoCentro = :codigoCentro";
        Query queryKeywordLinea = entityManager.createNativeQuery(sqlKeywordLinea);
        queryKeywordLinea.setParameter("codigoCentro", codigoCentro);
        List<Object[]> keywordLineaResults = queryKeywordLinea.getResultList();
        System.out.println("Links keyword-línea encontrados: " + keywordLineaResults.size());
        
        Map<String, Object> result = new HashMap<>();
        ArrayList<Map<String, Object>> nodes = new ArrayList<>();
        ArrayList<Map<String, Object>> links = new ArrayList<>();
        ArrayList<Map<String, Object>> categories = new ArrayList<>();
        
        // Agregar categoría "Keyword" (id=0)
        categories.add(newCategory(0, "Keyword"));
        
        // Procesar líneas de investigación
        Map<String, Integer> ids = new HashMap<>();
        int id = 0;
        
        for (Object[] row : lineasResults) {
            Map<String, Object> line = new HashMap<>();
            Long lineId = ((Number) row[2]).longValue(); // a.id
            Integer idArea = row[1] != null ? ((Number) row[1]).intValue() : null;
            
            line.put("_id", lineId);
            line.put("id", id);
            line.put("name", "C" + idArea + "L" + lineId);
            line.put("category", idArea);
            line.put("idArea", idArea);
            line.put("descripcion", row[7] != null ? row[7].toString() : ""); // b.valor
            line.put("color", row[6] != null ? row[6].toString() : null); // a.color
            
            ids.put("L" + lineId, id);
            nodes.add(line);
            id++;
        }
        
        // Procesar keywords
        // Keyword: id, descripcion
        Map<Integer, Map<String, Object>> allKeywords = new HashMap<>();
        for (Object[] row : keywordsResults) {
            Map<String, Object> keyword = new HashMap<>();
            Long keywordId = ((Number) row[0]).longValue();
            String descripcion = row[1] != null ? row[1].toString() : "Keyword " + keywordId;
            
            keyword.put("_id", keywordId);
            keyword.put("name", descripcion);
            keyword.put("category", 0);
            keyword.put("descripcion", descripcion);
            
            nodes.add(keyword);
            allKeywords.put(keywordId.intValue(), keyword);
        }
        
        // Procesar links keyword-línea
        // keyword_lineainvestigacion: idKeyword (bigint), idLineaInvestigacion (bigint)
        for (Object[] row : keywordLineaResults) {
            Long idKeyword = ((Number) row[0]).longValue();
            Long idLineaInvestigacion = ((Number) row[1]).longValue();
            
            // Verificar que la línea de investigación existe
            if (!ids.containsKey("L" + idLineaInvestigacion)) {
                continue; // Saltar si la línea no existe
            }
            
            Map<String, Object> link = new HashMap<>();
            link.put("source", ids.get("L" + idLineaInvestigacion));
            
            String key = "K" + idKeyword;
            if (!ids.containsKey(key)) {
                ids.put(key, id);
                Map<String, Object> keyword = allKeywords.remove(idKeyword.intValue());
                if (keyword != null) {
                    keyword.put("id", id);
                    link.put("target", id);
                    id++;
                } else {
                    // Si el keyword no existe en allKeywords, crear uno nuevo
                    Map<String, Object> newKeyword = new HashMap<>();
                    newKeyword.put("_id", idKeyword);
                    newKeyword.put("id", id);
                    newKeyword.put("name", "Keyword " + idKeyword);
                    newKeyword.put("category", 0);
                    newKeyword.put("descripcion", newKeyword.get("name"));
                    nodes.add(newKeyword);
                    link.put("target", id);
                    id++;
                }
            } else {
                link.put("target", ids.get(key));
            }
            
            links.add(link);
        }
        
        // Remover keywords no usados
        for (Map<String, Object> keyword : allKeywords.values()) {
            nodes.remove(keyword);
        }
        
        // Procesar áreas como categorías
        for (Object[] row : areasResults) {
            Map<String, Object> area = new HashMap<>();
            Integer areaId = ((Number) row[1]).intValue(); // a.id
            area.put("id", areaId);
            area.put("name", row[5] != null ? row[5].toString() : ""); // b.valor
            area.put("descripcion", area.get("name"));
            area.put("color", row[4] != null ? row[4].toString() : null); // a.color
            categories.add(area);
        }
        
        result.put("nodes", nodes);
        result.put("links", links);
        result.put("categories", categories);
        
        System.out.println("Resultado final - nodes: " + nodes.size() + ", links: " + links.size() + ", categories: " + categories.size());
        
        return result;
    }

    /**
     * Genera grafo de clusters de productos (placeholder - necesita implementación completa)
     */
    private Map<String, Object> getClusterProductsGraphs(Map<String, Object> input) throws Exception {
        // TODO: Implementar consulta completa de clusters de productos
        Map<String, Object> result = new HashMap<>();
        result.put("nodes", new ArrayList<>());
        result.put("links", new ArrayList<>());
        result.put("categories", new ArrayList<>());
        return result;
    }

    /**
     * Genera grafo de clusters de productos v2 (placeholder - necesita implementación completa)
     */
    private Map<String, Object> getClusterProductsGraphs2(Map<String, Object> input) throws Exception {
        // TODO: Implementar consulta completa de clusters de productos v2
        Map<String, Object> result = new HashMap<>();
        result.put("nodes", new ArrayList<>());
        result.put("links", new ArrayList<>());
        result.put("categories", new ArrayList<>());
        return result;
    }

    /**
     * Consulta publicaciones relacionadas a un enlace del grafo
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> queryPublications(Map<String, Object> filter) throws Exception {
        List<Integer> periods = convertToList(filter.get("periods"));
        Long from = filter.get("from") != null ? ((Number) filter.get("from")).longValue() : null;
        Long to = filter.get("to") != null ? ((Number) filter.get("to")).longValue() : null;
        
        if (from == null || to == null) {
            return new ArrayList<>();
        }
        
        StringBuilder sql = new StringBuilder(
            "SELECT DISTINCT p.id, pr.idDescripcion as descripcion, pr.progressReport, " +
            "j.descripcion as journal, p.doi " +
            "FROM publicacion p " +
            "INNER JOIN producto pr ON p.id = pr.id " +
            "LEFT JOIN journal j ON p.idJournal = j.id " +
            "INNER JOIN afiliacion a ON a.idProducto = p.id " +
            "WHERE (a.idRRHHFrom = :fromId AND a.idRRHHTo = :toId) " +
            "OR (a.idRRHHFrom = :toId AND a.idRRHHTo = :fromId)"
        );
        
        if (periods != null && !periods.isEmpty()) {
            sql.append(" AND pr.progressReport IN (");
            sql.append(periods.stream().map(String::valueOf).collect(Collectors.joining(",")));
            sql.append(")");
        }
        
        Query query = entityManager.createNativeQuery(sql.toString());
        query.setParameter("fromId", from);
        query.setParameter("toId", to);
        
        List<Object[]> results = query.getResultList();
        List<Map<String, Object>> publications = new ArrayList<>();
        
        for (Object[] row : results) {
            Map<String, Object> pub = new HashMap<>();
            pub.put("id", ((Number) row[0]).longValue());
            pub.put("descripcion", row[1]);
            pub.put("progressReport", row[2] != null ? ((Number) row[2]).intValue() : null);
            pub.put("journal", row[3]);
            pub.put("doi", row[4]);
            publications.add(pub);
        }
        
        return publications;
    }
}
