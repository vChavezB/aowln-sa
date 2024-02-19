package com.github.vchavezb.utilities;

import org.semanticweb.owlapi.annotations.HasPriority;
import org.semanticweb.owlapi.io.OWLOntologyDocumentSourceBase;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyIRIMapper;
import org.semanticweb.owlapi.util.SAXParsers;
import org.semanticweb.owlapi.util.ZipIRIMapper;
import org.semanticweb.owlapi.vocab.Namespaces;
import org.semanticweb.owlapi.vocab.OWLXMLVocabulary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.semanticweb.owlapi.util.CollectionFactory.createMap;
import static org.semanticweb.owlapi.util.OWLAPIPreconditions.checkNotNull;
import static org.semanticweb.owlapi.util.OWLAPIPreconditions.verifyNotNull;


/**
 * IRI Mapper inspired from the owlapi AutoIRIMapper with optional argment
 * to map owl:versionIRI xml element from an rdf. This allows for special use cases
 * where ontology is imported from a specific version. The downside of this is that
 * if the imported ontology does not have defined owl:versionIRI than it might take
 * sometime to load all IRIs defined in the rootDirectory.
 *
 * A mapper which given a root folder attempts to automatically discover and map files to
 * ontologies. The mapper is capable of mapping ontologies in RDF/XML, OWL/XML, Manchester OWL
 * Syntax, Functional Syntax and OBO (other serialisations are not supported). Zip and jar files
 * containing ontologies are supported, either as main argument to the constructor or as content of
 * the root folder.
 *
 * @author Matthew Horridge, The University Of Manchester, Bio-Health Informatics Group
 *          Victor Chavez, IAAM
 * @since 2.0.0
 */
@HasPriority(1)
public class IRIMapper extends DefaultHandler implements OWLOntologyIRIMapper, Serializable {

    private static final String ONTOLOGY_ELEMENT_FOUND_PARSING_COMPLETE =
            "Ontology element found, parsing complete.";
    private static final long serialVersionUID = 40000L;
    static final Pattern pattern = Pattern.compile("Ontology\\(<([^>]+)>");
    static final Pattern manPattern = Pattern.compile("Ontology:[\r\n ]*<([^>]+)>");
    private static final Logger LOGGER = LoggerFactory.getLogger(org.semanticweb.owlapi.util.AutoIRIMapper.class);
    private final Set<String> fileExtensions =
            new HashSet<>(Arrays.asList(".owl", ".xml", ".rdf", ".omn", ".ofn"));
    private boolean mapped;
    private final boolean recursive;
    private final Map<String, OntologyRootElementHandler> handlerMap = createMap();
    private final Map<IRI, IRI> ontologyIRI2PhysicalURIMap = createMap();
    private final Map<String, IRI> oboFileMap = createMap();
    private final String directoryPath;
    private transient File currentFile;
    private final boolean iriVersion;

    /**
     * Creates an auto-mapper which examines ontologies that reside in the specified root folder
     * (and possibly sub-folders).
     *
     * @param rootDirectory The root directory which should be searched for ontologies; this can
     *        also be a zip/jar file containing ontologies. If root is actually a folder, zip/jar
     *        files included in the folder are parsed for ontologies. The zip parsing is delegated
     *        to ZipIRIMapper.
     * @param recursive Sub directories will be searched recursively if {@code true}.
     */
    public IRIMapper(@Nonnull File rootDirectory, boolean recursive, boolean iriVersion) {
        directoryPath =
                checkNotNull(rootDirectory, "rootDirectory cannot be null").getAbsolutePath();
        this.recursive = recursive;
        this.iriVersion = iriVersion;
        mapped = false;
        /**
         * A handler to handle RDF/XML files. The xml:base (if present) is taken to be the ontology
         * URI of the ontology document being parsed.
         */
        handlerMap.put(Namespaces.RDF + "RDF", this::baseIRI);
        /**
         * A handler that can handle OWL/XML files as well as RDF/XML with an owl:Ontology element
         * is defined with a non empty rdf:about.
         */
        handlerMap.put(OWLXMLVocabulary.ONTOLOGY.toString(), this::ontologyIRI);
    }
    @Nullable
    protected IRI versionIRI(Attributes attributes) {
        // Extract the value of rdf:resource attribute
        String versionIRIValue = attributes.getValue("http://www.w3.org/1999/02/22-rdf-syntax-ns#resource");
        if (versionIRIValue != null) {
            return IRI.create(versionIRIValue);
        }
        return null;
    }

    @Nullable
    protected IRI ontologyIRI(Attributes attributes) {
        String ontURI = attributes.getValue(Namespaces.OWL.toString(), "ontologyIRI");
        if (ontURI == null) {
            ontURI = attributes.getValue("ontologyIRI");
        }
        if (ontURI == null) {
            ontURI = attributes.getValue(Namespaces.RDF.toString(), "about");
        }
        if (ontURI == null) {
            return null;
        }
        return IRI.create(ontURI);
    }

    @Nullable
    protected IRI baseIRI(Attributes attributes) {
        String baseValue = attributes.getValue(Namespaces.XML.toString(), "base");
        if (baseValue == null) {
            return null;
        }
        return IRI.create(baseValue);
    }

    /**
     * @param tok token
     * @return IRI without quotes (&lt; and &gt;)
     */
    static IRI unquote(String tok) {
        String substring = tok.substring(1, tok.length() - 1);
        assert substring != null;
        return IRI.create(substring);
    }

    protected File getDirectory() {
        return new File(directoryPath);
    }

    /**
     * The mapper only examines files that have specified file extensions. This method returns the
     * file extensions that cause a file to be examined.
     *
     * @return A {@code Set} of file extensions.
     */
    public Set<String> getFileExtensions() {
        return new HashSet<>(fileExtensions);
    }

    /**
     * Sets the extensions of files that are to be examined for ontological content. (By default the
     * extensions are, owl, xml and rdf). Only files that have the specified extensions will be
     * examined to see if they contain ontologies.
     *
     * @param extensions the set of extensions
     */
    public void setFileExtensions(Set<String> extensions) {
        fileExtensions.clear();
        fileExtensions.addAll(extensions);
    }

    /**
     * Gets the set of ontology IRIs that this mapper has found.
     *
     * @return A {@code Set} of ontology (logical) URIs
     */
    public Set<IRI> getOntologyIRIs() {
        if (!mapped) {
            mapFiles();
        }
        return new HashSet<>(ontologyIRI2PhysicalURIMap.keySet());
    }

    /**
     * update the map.
     */
    public void update() {
        mapFiles();
    }

    @Override
    public IRI getDocumentIRI(IRI ontologyIRI) {
        if (!mapped) {
            mapFiles();
        }
        if (ontologyIRI.toString().endsWith(".obo")) {
            String path = ontologyIRI.toURI().getPath();
            if (path != null) {
                int lastSepIndex = path.lastIndexOf('/');
                String name = path.substring(lastSepIndex + 1, path.length());
                IRI documentIRI = oboFileMap.get(name);
                if (documentIRI != null) {
                    return documentIRI;
                }
            }
        }
        return ontologyIRI2PhysicalURIMap.get(ontologyIRI);
    }

    private void mapFiles() {
        mapped = true;
        ontologyIRI2PhysicalURIMap.clear();
        processFile(getDirectory());
    }

    private void processFile(File f) {
        if (f.isHidden()) {
            return;
        }
        // if pointed directly at a zip file, map it
        parseIfExtensionSupported(f);
        File[] files = f.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file.isDirectory() && recursive) {
                processFile(file);
            } else {
                parseIfExtensionSupported(file);
            }
        }
    }

    protected void parseIfExtensionSupported(File file) {
        String name = file.getName();
        int lastIndexOf = name.lastIndexOf('.');
        if (lastIndexOf < 0) {
            // no extension for the file, nothing to do
            return;
        }
        String extension = name.substring(lastIndexOf);
        if (".zip".equalsIgnoreCase(extension) || ".jar".equalsIgnoreCase(extension)) {
            try {
                ZipIRIMapper mapper = new ZipIRIMapper(file, "jar:" + file.toURI() + "!/");
                mapper.oboMappings().forEach(e -> oboFileMap.put(e.getKey(), e.getValue()));
                mapper.iriMappings()
                        .forEach(e -> ontologyIRI2PhysicalURIMap.put(e.getKey(), e.getValue()));
            } catch (IOException e) {
                // if we can't parse a file, then we can't map it
                LOGGER.debug("Exception reading file", e);
            }

        } else if (".obo".equalsIgnoreCase(extension)) {
            oboFileMap.put(name, IRI.create(file));
        } else if (".ofn".equalsIgnoreCase(extension)) {
            parseFSSFile(file);
        } else if (".omn".equalsIgnoreCase(extension)) {
            parseManchesterSyntaxFile(file);
        } else if (fileExtensions.contains(extension.toLowerCase())) {
            parseFile(file);
        }
    }

    /**
     * Search first 100 lines for FSS style Ontology(&lt;IRI&gt; ...
     *
     * @param file the file to parse
     */
    private void parseFSSFile(File file) {
        try (InputStream input = new FileInputStream(file);
             Reader reader = new InputStreamReader(input, "UTF-8");
             BufferedReader br = new BufferedReader(reader)) {
            String line = "";
            Matcher m = pattern.matcher(line);
            int n = 0;
            while ((line = br.readLine()) != null && n++ < 100) {
                m.reset(line);
                if (m.matches()) {
                    String group = m.group(1);
                    assert group != null;
                    addMapping(IRI.create(group), file);
                    break;
                }
            }
        } catch (IOException e) {
            // if we can't parse a file, then we can't map it
            LOGGER.debug("Exception reading file", e);
        }
    }

    private void parseFile(File file) {
        try (FileInputStream in = new FileInputStream(file);
             BufferedInputStream delegate = new BufferedInputStream(in);
             InputStream is = OWLOntologyDocumentSourceBase.wrap(delegate);) {
            currentFile = file;
            // Using the default expansion limit. If the ontology IRI cannot be
            // found before 64000 entities are expanded, the file is too
            // expensive to parse.
            SAXParsers.initParserWithOWLAPIStandards(null, "64000").parse(is, this);
        } catch (SAXException e) {
            // Exceptions thrown to halt parsing early when the ontology IRI is found
            // should not be logged because they are not actual errors, only a performance hack.
            if (!Objects.equals(ONTOLOGY_ELEMENT_FOUND_PARSING_COMPLETE, e.getMessage())) {
                LOGGER.debug("SAX Exception reading file", e);
            }
        } catch (IOException e) {
            // if we can't parse a file, then we can't map it
            LOGGER.debug("IO Exception reading file", e);
        }
    }

    private void parseManchesterSyntaxFile(File file) {
        try (FileInputStream input = new FileInputStream(file);
             InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8);
             BufferedReader br = new BufferedReader(reader)) {
            // Ontology: <URI>
            String line = br.readLine();
            while (line != null) {
                if (parseManLine(file, line) != null) {
                    return;
                }
                line = br.readLine();
            }
        } catch (IOException e) {
            // if we can't parse a file, then we can't map it
            LOGGER.debug("Exception reading file", e);
        }
    }

    @Nullable
    private IRI parseManLine(File file, String line) {
        Matcher matcher = manPattern.matcher(line);
        if (matcher.matches()) {
            IRI iri = IRI.create(matcher.group(1));
            addMapping(iri, file);
            return iri;
        }
        return null;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes)
            throws SAXException {
        String tag = uri + localName;
        OntologyRootElementHandler handler = handlerMap.get(tag);
        if (handler != null) {
            IRI ontologyIRI = handler.handle(checkNotNull(attributes));
            if (ontologyIRI != null && currentFile != null) {
                addMapping(ontologyIRI, verifyNotNull(currentFile));
            }
        }
        // Do not throw completion exception if owl:versionIRI will also be mapped
        if (tag.equals("http://www.w3.org/2002/07/owl#Ontology") && !iriVersion) {
            throw new SAXException(ONTOLOGY_ELEMENT_FOUND_PARSING_COMPLETE);
        }
        if (tag.equals("http://www.w3.org/2002/07/owl#versionIRI")) {
            IRI ontologyIRI = IRI.create(attributes.getValue(0));
            addMapping(ontologyIRI, verifyNotNull(currentFile));
            throw new SAXException(ONTOLOGY_ELEMENT_FOUND_PARSING_COMPLETE);
        }
    }

    /**
     * @param ontologyIRI ontology
     * @param file file
     */
    protected void addMapping(@Nonnull IRI ontologyIRI, @Nonnull File file) {
        ontologyIRI2PhysicalURIMap.put(ontologyIRI, IRI.create(file));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("AutoIRIMapper: (");
        sb.append(ontologyIRI2PhysicalURIMap.size()).append(" ontologies)\n");
        ontologyIRI2PhysicalURIMap.forEach((k, v) -> sb.append("    ").append(k.toQuotedString())
                .append(" -> ").append(v).append('\n'));
        return sb.toString();
    }

    /**
     * A simple interface which extracts an ontology IRI from a set of element attributes.
     */
    @FunctionalInterface
    private interface OntologyRootElementHandler extends Serializable {

        /**
         * Gets the ontology IRI.
         *
         * @param attributes The attributes which will be examined for the ontology IRI.
         * @return The ontology IRI or {@code null} if no ontology IRI could be found.
         */
        IRI handle(Attributes attributes);
    }
}
