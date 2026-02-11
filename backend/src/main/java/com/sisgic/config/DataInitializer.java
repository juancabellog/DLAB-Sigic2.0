package com.sisgic.config;

import com.sisgic.entity.*;
import com.sisgic.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TipoParticipacionRepository tipoParticipacionRepository;

    @Autowired
    private TipoProductoRepository tipoProductoRepository;

    @Autowired
    private JournalRepository journalRepository;

    @Autowired
    private TipoRRHHRepository tipoRRHHRepository;

    @Autowired
    private RRHHRepository rrhhRepository;

    @Autowired
    private TipoEventoRepository tipoEventoRepository;

    @Autowired
    private PaisRepository paisRepository;

    @Autowired
    private InstitucionRepository institucionRepository;

    @Autowired
    private GradoAcademicoRepository gradoAcademicoRepository;

    @Autowired
    private EstadoTesisRepository estadoTesisRepository;

    @Autowired
    private TipoSectorRepository tipoSectorRepository;

    @Autowired
    private TipoTransferenciaRepository tipoTransferenciaRepository;

    @Autowired
    private CategoriaTransferenciaRepository categoriaTransferenciaRepository;

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private FundingTypeRepository fundingTypeRepository;
    
    @Autowired
    private TipoDifusionRepository tipoDifusionRepository;
    
    @Autowired
    private PublicoObjetivoRepository publicoObjetivoRepository;
    
    @Autowired
    private TipoColaboracionRepository tipoColaboracionRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private com.sisgic.service.TextosService textosService;

    @Override
    public void run(String... args) throws Exception {
        initializeRolesAndUsers();
        initializeProductTypes();
        // Inicializaciones que no dependen de TextosService
        initializeRRHH();
        initializeEventTypes();
        initializeCountries();
        initializeInstitutions();
        initializeSectorTypes();
        initializeAcademicDegrees();
        initializeThesisStatuses();
        initializeTransferTypes();
        initializeTransferCategories();
        initializeResources();
        initializeFundingTypes();
        initializeTipoDifusion();
        initializePublicoObjetivo();
        initializeTipoColaboracion();
        // Inicializaciones que ahora usan idDescripcion directamente (sin TextosService)
        initializeParticipationTypes();
        initializeJournals();
        // initializeProyectos(); // Eliminado - los proyectos no tienen lista de productos asociados
    }

    private void initializeRolesAndUsers() {
        if (roleRepository.count() == 0) {
            Role adminRole = new Role("ROLE_ADMIN", "Administrator role");
            adminRole.setCreatedAt(LocalDateTime.now());
            adminRole.setUpdatedAt(LocalDateTime.now());
            roleRepository.save(adminRole);

            Role userRole = new Role("ROLE_USER", "User role");
            userRole.setCreatedAt(LocalDateTime.now());
            userRole.setUpdatedAt(LocalDateTime.now());
            roleRepository.save(userRole);
        }

        if (userRepository.count() == 0) {
            var adminRole = roleRepository.findByName("ROLE_ADMIN").orElseThrow();
            var userRole = roleRepository.findByName("ROLE_USER").orElseThrow();

            User admin = new User("admin", "admin@scientific.com", "Admin", "User", passwordEncoder.encode("admin123"));
            admin.setCreatedAt(LocalDateTime.now());
            admin.setUpdatedAt(LocalDateTime.now());
            admin.setRoles(new HashSet<>(Set.of(adminRole)));
            userRepository.save(admin);

            User user = new User("user", "user@scientific.com", "Regular", "User", passwordEncoder.encode("user123"));
            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());
            user.setRoles(new HashSet<>(Set.of(userRole)));
            userRepository.save(user);
        }
    }

    private void initializeProductTypes() {
        System.out.println("🔍 Initializing product types...");
        
        if (tipoProductoRepository.count() == 0) {
            System.out.println("📦 Creating product types...");
            // Crear tipos de producto con los valores por defecto especificados
            // idDescripcion se trata como descripción directa (no hay join con textos)
            createProductType("DIFUSION");
            createProductType("CURSO");
            createProductType("PUBLICACION");
            createProductType("TECNOLOGIA");
            createProductType("CONGRESO");
            createProductType("PAPER");
            createProductType("PATENTE");
            createProductType("POSTER");
            createProductType("PREMIOS");
            createProductType("TRANSF_TECNOLOGICA");
            createProductType("TESIS");
            createProductType("COLLABORATIONS");
            createProductType("CONEXION");
            createProductType("BECAPOSTDOCTO");
            createProductType("ORGANIZACION_EVENTOS_CIENTIFICOS");
            createProductType("PARTICIPACION_EVENTOS_CIENTIFICOS");
            System.out.println("✅ Product types created successfully");
        } else {
            System.out.println("📦 Product types already exist: " + tipoProductoRepository.count() + " items");
        }
    }

    private void createProductType(String idDescripcion) {
        System.out.println("  Creating product type: " + idDescripcion);
        try {
            // Crear tipo de producto con idDescripcion como descripción directa
            // No usar TextosService, idDescripcion es el texto directo
            TipoProducto tipo = new TipoProducto(idDescripcion, idDescripcion);
            TipoProducto saved = tipoProductoRepository.save(tipo);
            System.out.println("  Saved product type with ID: " + saved.getId() + ", idDescripcion: " + saved.getIdDescripcion());
        } catch (Exception e) {
            System.err.println("  Error creating product type '" + idDescripcion + "': " + e.getMessage());
            e.printStackTrace();
            // Continuar con el siguiente tipo de producto
        }
    }

    private void initializeParticipationTypes() {
        if (tipoParticipacionRepository.count() == 0) {
            // Obtener el primer tipo de producto (asumiendo que existe)
            Long defaultProductTypeId = 1L; // Default a tipo 1
            
            // Tipos para publicaciones
            createParticipationType("Author", defaultProductTypeId);
            createParticipationType("Co-Author", defaultProductTypeId);
            createParticipationType("First Author", defaultProductTypeId);
            createParticipationType("Last Author", defaultProductTypeId);
            
            // Tipos para todos los productos
            createParticipationType("Principal Investigator", defaultProductTypeId);
            createParticipationType("Co-Investigator", defaultProductTypeId);
            createParticipationType("Researcher", defaultProductTypeId);
            createParticipationType("Student", defaultProductTypeId);
            createParticipationType("Supervisor", defaultProductTypeId);
            createParticipationType("Presenter", defaultProductTypeId);
            createParticipationType("Chair", defaultProductTypeId);
        }
    }

    private void createParticipationType(String description, Long productTypeId) {
        // Usar description directamente como idDescripcion (no usar TextosService)
        String idDescripcion = description;
        
        // Obtener nombre del tipo de producto (idDescripcion es la descripción directa)
        String tipoProductoNombre = tipoProductoRepository.findById(productTypeId)
            .map(tipo -> tipo.getIdDescripcion() != null ? tipo.getIdDescripcion() : tipo.getDescripcion())
            .orElse("Unknown");
        
        // Crear tipo de participación con idDescripcion como descripción directa
        TipoParticipacion tipo = new TipoParticipacion(idDescripcion, productTypeId, description, tipoProductoNombre);
        tipoParticipacionRepository.save(tipo);
    }

    private void initializeRRHH() {
        if (tipoRRHHRepository.count() == 0) {
            tipoRRHHRepository.save(new TipoRRHH("Professor"));
            tipoRRHHRepository.save(new TipoRRHH("Associate Professor"));
            tipoRRHHRepository.save(new TipoRRHH("Assistant Professor"));
            tipoRRHHRepository.save(new TipoRRHH("Postdoc"));
            tipoRRHHRepository.save(new TipoRRHH("PhD Student"));
            tipoRRHHRepository.save(new TipoRRHH("Master Student"));
            tipoRRHHRepository.save(new TipoRRHH("Research Assistant"));
        }

        if (rrhhRepository.count() == 0) {
            var tipoProf = tipoRRHHRepository.findAll().get(0);
            var tipoPostdoc = tipoRRHHRepository.findAll().get(3);
            var tipoPhD = tipoRRHHRepository.findAll().get(4);

            // Ana Pérez (sin título)
            rrhhRepository.save(new RRHH("12.345.678-9", "Ana", null, "Pérez", null, tipoProf, "+56912345678", "ana.perez@university.edu", "AP", "0000-0002-1825-0097", "F"));
            // Juan García (sin título)
            rrhhRepository.save(new RRHH("98.765.432-1", "Juan", null, "García", null, tipoProf, "+56987654321", "juan.garcia@university.edu", "JG", "0000-0003-1415-9265", "M"));
            // María López (sin título)
            rrhhRepository.save(new RRHH("11.222.333-4", "María", null, "López", null, tipoPostdoc, "+56911223334", "maria.lopez@university.edu", "ML", "0000-0001-2345-6789", "F"));
            // Carlos Rodríguez
            rrhhRepository.save(new RRHH("55.666.777-8", "Carlos", null, "Rodríguez", null, tipoPhD, "+56955666777", "carlos.rodriguez@university.edu", "CR", "0000-0004-5678-9012", "M"));
        }
    }

    private void initializeJournals() {
        System.out.println("🔍 Initializing journals...");
        if (journalRepository.count() == 0) {
            System.out.println("📦 Creating journals...");
            // Crear journals básicos
            createJournal("Nature", "Nature", "0028-0836");
            createJournal("Science", "Science", "0036-8075");
            createJournal("Cell", "Cell", "0092-8674");
            createJournal("The Lancet", "Lancet", "0140-6736");
            createJournal("New England Journal of Medicine", "NEJM", "0028-4793");
            System.out.println("✅ Journals created successfully");
        } else {
            System.out.println("📦 Journals already exist: " + journalRepository.count() + " items");
        }
    }

    private void createJournal(String description, String abbreviation, String issn) {
        System.out.println("  Creating journal: " + description);
        // Crear texto en ambos idiomas (us y es) usando TextosService
        String codigoTexto = textosService.createTextInBothLanguages(description, 2);
        
        // Crear el journal con idDescripcion como String (código de texto)
        Journal journal = new Journal(codigoTexto, abbreviation, issn);
        Journal saved = journalRepository.save(journal);
        System.out.println("  Saved journal with ID: " + saved.getId() + ", idDescripcion: " + saved.getIdDescripcion());
    }


    private void initializeEventTypes() {
        System.out.println("🔍 Initializing event types...");
        if (tipoEventoRepository.count() == 0) {
            System.out.println("📦 Creating event types...");
            // Crear tipos de eventos con los valores especificados (texto directo como idDescripcion)
            tipoEventoRepository.save(new TipoEvento("International congress", "International congress"));
            tipoEventoRepository.save(new TipoEvento("National congress", "National congress"));
            tipoEventoRepository.save(new TipoEvento("Workshop", "Workshop"));
            tipoEventoRepository.save(new TipoEvento("Course", "Course"));
            tipoEventoRepository.save(new TipoEvento("Conference", "Conference"));
            System.out.println("✅ Event types created successfully");
        } else {
            System.out.println("📦 Event types already exist: " + tipoEventoRepository.count() + " items");
        }
    }

    private void initializeCountries() {
        System.out.println("🔍 Initializing countries...");
        if (paisRepository.count() == 0) {
            System.out.println("📦 Creating countries...");
            // Crear países con los valores especificados
            paisRepository.save(new Pais("CHL", "Chile"));
            paisRepository.save(new Pais("USA", "UNITED STATES"));
            paisRepository.save(new Pais("ESP", "SPAIN"));
            paisRepository.save(new Pais("JPN", "JAPAN"));
            paisRepository.save(new Pais("UK", "UK"));
            System.out.println("✅ Countries created successfully");
        } else {
            System.out.println("📦 Countries already exist: " + paisRepository.count() + " items");
        }
    }

    private void initializeInstitutions() {
        System.out.println("🔍 Initializing institutions...");
        if (institucionRepository.count() == 0) {
            System.out.println("📦 Creating institutions...");
            // Crear instituciones con los valores especificados
            institucionRepository.save(new Institucion("Alberoni University"));
            institucionRepository.save(new Institucion("UNIVERSIDAD DE CHILE"));
            institucionRepository.save(new Institucion("P. Universidad Católica de Chile"));
            institucionRepository.save(new Institucion("Sociedad Chilena de Infectologia"));
            institucionRepository.save(new Institucion("Sociedad de Biologia Celular de Chile"));
            institucionRepository.save(new Institucion("Sociedad de Microbiologia de Chile"));
            institucionRepository.save(new Institucion("Sociedad de Bioquimica y Biologia Molecular de Chile"));
            institucionRepository.save(new Institucion("Universidad Bernardo O Higgins"));
            System.out.println("✅ Institutions created successfully");
        } else {
            System.out.println("📦 Institutions already exist: " + institucionRepository.count() + " items");
        }
    }

    private void initializeSectorTypes() {
        System.out.println("🔍 Initializing sector types...");
        if (tipoSectorRepository.count() == 0) {
            System.out.println("📦 Creating sector types...");
            // Crear tipos de sector con los valores especificados
            tipoSectorRepository.save(new TipoSector("Private Sector"));
            tipoSectorRepository.save(new TipoSector("Public Sector"));
            tipoSectorRepository.save(new TipoSector("Social"));
            tipoSectorRepository.save(new TipoSector("ONG"));
            tipoSectorRepository.save(new TipoSector("Center Sector"));
            System.out.println("✅ Sector types created successfully");
        } else {
            System.out.println("📦 Sector types already exist: " + tipoSectorRepository.count() + " items");
        }
    }

    private void initializeAcademicDegrees() {
        System.out.println("🔍 Initializing academic degrees...");
        if (gradoAcademicoRepository.count() == 0) {
            System.out.println("📦 Creating academic degrees...");
            // Crear grados académicos con los valores especificados
            gradoAcademicoRepository.save(new GradoAcademico("Undergraduate degree or professional title", "Undergraduate degree or professional title"));
            gradoAcademicoRepository.save(new GradoAcademico("Master equivalent", "Master equivalent"));
            gradoAcademicoRepository.save(new GradoAcademico("PhD degree", "PhD degree"));
            System.out.println("✅ Academic degrees created successfully");
        } else {
            System.out.println("📦 Academic degrees already exist: " + gradoAcademicoRepository.count() + " items");
        }
    }

    private void initializeThesisStatuses() {
        System.out.println("🔍 Initializing thesis statuses...");
        if (estadoTesisRepository.count() == 0) {
            System.out.println("📦 Creating thesis statuses...");
            // Crear estados de tesis con los valores especificados
            estadoTesisRepository.save(new EstadoTesis("Finished", "Finished"));
            estadoTesisRepository.save(new EstadoTesis("In Progress", "In Progress"));
            System.out.println("✅ Thesis statuses created successfully");
        } else {
            System.out.println("📦 Thesis statuses already exist: " + estadoTesisRepository.count() + " items");
        }
    }

    private void initializeTransferTypes() {
        System.out.println("🔍 Initializing transfer types...");
        if (tipoTransferenciaRepository.count() == 0) {
            System.out.println("📦 Creating transfer types...");
            // Crear tipos de transferencia con los valores especificados
            tipoTransferenciaRepository.save(new TipoTransferencia("Spin-Off"));
            tipoTransferenciaRepository.save(new TipoTransferencia("Technology Transfer Agreement"));
            tipoTransferenciaRepository.save(new TipoTransferencia("Start Up"));
            tipoTransferenciaRepository.save(new TipoTransferencia("Other"));
            tipoTransferenciaRepository.save(new TipoTransferencia("Others: Consulting and business advice"));
            tipoTransferenciaRepository.save(new TipoTransferencia("Others: Consulting and technical advice"));
            tipoTransferenciaRepository.save(new TipoTransferencia("Others: Knowledge Transfer"));
            tipoTransferenciaRepository.save(new TipoTransferencia("Technology Transfer Agreement"));
            tipoTransferenciaRepository.save(new TipoTransferencia("Others: MTA"));
            System.out.println("✅ Transfer types created successfully");
        } else {
            System.out.println("📦 Transfer types already exist: " + tipoTransferenciaRepository.count() + " items");
        }
    }

    private void initializeTransferCategories() {
        System.out.println("🔍 Initializing transfer categories...");
        if (categoriaTransferenciaRepository.count() == 0) {
            System.out.println("📦 Creating transfer categories...");
            // Crear categorías de transferencia con los valores especificados
            categoriaTransferenciaRepository.save(new CategoriaTransferencia("Technology Transfer"));
            categoriaTransferenciaRepository.save(new CategoriaTransferencia("Knowledge Transfer"));
            System.out.println("✅ Transfer categories created successfully");
        } else {
            System.out.println("📦 Transfer categories already exist: " + categoriaTransferenciaRepository.count() + " items");
        }
    }

    private void initializeResources() {
        System.out.println("🔍 Initializing resources...");
        if (resourceRepository.count() == 0) {
            System.out.println("📦 Creating resources...");
            resourceRepository.save(new Resource("Equipment"));
            resourceRepository.save(new Resource("Information"));
            resourceRepository.save(new Resource("Infrastructure"));
            System.out.println("✅ Resources created successfully");
        } else {
            System.out.println("📦 Resources already exist: " + resourceRepository.count() + " items");
        }
    }

    private void initializeFundingTypes() {
        System.out.println("🔍 Initializing funding types...");
        if (fundingTypeRepository.count() == 0) {
            System.out.println("📦 Creating funding types...");
            fundingTypeRepository.save(new FundingType("Fondecyt"));
            fundingTypeRepository.save(new FundingType("Fondap"));
            fundingTypeRepository.save(new FundingType("Fondequip"));
            fundingTypeRepository.save(new FundingType("Fondef"));
            fundingTypeRepository.save(new FundingType("Regional"));
            fundingTypeRepository.save(new FundingType("Pia"));
            fundingTypeRepository.save(new FundingType("Other"));
            System.out.println("✅ Funding types created successfully");
        } else {
            System.out.println("📦 Funding types already exist: " + fundingTypeRepository.count() + " items");
        }
    }
    
    private void initializeTipoDifusion() {
        System.out.println("🔍 Initializing diffusion types...");
        if (tipoDifusionRepository.count() == 0) {
            System.out.println("📦 Creating diffusion types...");
            tipoDifusionRepository.save(new TipoDifusion("Conference"));
            tipoDifusionRepository.save(new TipoDifusion("Seminar"));
            tipoDifusionRepository.save(new TipoDifusion("Forum"));
            tipoDifusionRepository.save(new TipoDifusion("Exhibition"));
            tipoDifusionRepository.save(new TipoDifusion("Workshop"));
            tipoDifusionRepository.save(new TipoDifusion("Competition"));
            tipoDifusionRepository.save(new TipoDifusion("Course"));
            tipoDifusionRepository.save(new TipoDifusion("Other"));
            tipoDifusionRepository.save(new TipoDifusion("Others: Audiovisual record"));
            tipoDifusionRepository.save(new TipoDifusion("Others: Digital newspaper appearance"));
            tipoDifusionRepository.save(new TipoDifusion("Others: Documentary"));
            tipoDifusionRepository.save(new TipoDifusion("Others: Educational activity with students"));
            tipoDifusionRepository.save(new TipoDifusion("Others: Estrella de Arica"));
            tipoDifusionRepository.save(new TipoDifusion("Others: Interview"));
            tipoDifusionRepository.save(new TipoDifusion("Others: Newsletter"));
            tipoDifusionRepository.save(new TipoDifusion("Others: Newspaper appareance"));
            tipoDifusionRepository.save(new TipoDifusion("Others: Newspaper interview"));
            tipoDifusionRepository.save(new TipoDifusion("Others: Radio appearance"));
            tipoDifusionRepository.save(new TipoDifusion("Others: TV Series"));
            tipoDifusionRepository.save(new TipoDifusion("Others: TV appearance"));
            tipoDifusionRepository.save(new TipoDifusion("Others: Website"));
            tipoDifusionRepository.save(new TipoDifusion("Outreach Material"));
            tipoDifusionRepository.save(new TipoDifusion("Social media"));
            tipoDifusionRepository.save(new TipoDifusion("Others: Website, Social media"));
            System.out.println("✅ Diffusion types created successfully");
        } else {
            System.out.println("📦 Diffusion types already exist: " + tipoDifusionRepository.count() + " items");
        }
    }
    
    private void initializePublicoObjetivo() {
        System.out.println("🔍 Initializing target audiences...");
        if (publicoObjetivoRepository.count() == 0) {
            System.out.println("📦 Creating target audiences...");
            publicoObjetivoRepository.save(new PublicoObjetivo("Undergraduate Students"));
            publicoObjetivoRepository.save(new PublicoObjetivo("Primary Education Students"));
            publicoObjetivoRepository.save(new PublicoObjetivo("Secondary Education Students"));
            publicoObjetivoRepository.save(new PublicoObjetivo("General Community"));
            publicoObjetivoRepository.save(new PublicoObjetivo("Companies, Industries, Services"));
            publicoObjetivoRepository.save(new PublicoObjetivo("School Teachers"));
            System.out.println("✅ Target audiences created successfully");
        } else {
            System.out.println("📦 Target audiences already exist: " + publicoObjetivoRepository.count() + " items");
        }
    }
    
    private void initializeTipoColaboracion() {
        System.out.println("🔍 Initializing collaboration types...");
        if (tipoColaboracionRepository.count() == 0) {
            System.out.println("📦 Creating collaboration types...");
            tipoColaboracionRepository.save(new TipoColaboracion("Visit in Chile"));
            tipoColaboracionRepository.save(new TipoColaboracion("Visit Abroad"));
            tipoColaboracionRepository.save(new TipoColaboracion("Research Stage"));
            tipoColaboracionRepository.save(new TipoColaboracion("Participation in R&D Projects directed by other Research Group"));
            System.out.println("✅ Collaboration types created successfully");
        } else {
            System.out.println("📦 Collaboration types already exist: " + tipoColaboracionRepository.count() + " items");
        }
    }
}