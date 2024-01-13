package com.github.vchavezb.utilities;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.AutoIRIMapper;
import org.semanticweb.owlapi.util.DefaultPrefixManager;
import org.swrlapi.core.*;
import org.swrlapi.factory.SWRLAPIFactory;
import org.swrlapi.factory.SWRLAPIInternalFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class OWLUtil {

    private OWLOntologyManager manager;
    private OWLOntology ontology;
    private DefaultPrefixManager prefixManager;
    private Set<SWRLAPIRule> allRules;

    /**
     * @brief Load prefixes from the ontology document to the swrl iri resolver
     * @details When creating a new iri resolver for the swrl engine
     *          the prefixes from the document must be loaded, otherwise
     *          only the default prefix will be visible.
     * @param iriResolver the iri resolver used for the swrl engine
     */
    private void loadPrefixes(IRIResolver iriResolver) {
        OWLDocumentFormat format = manager.getOntologyFormat(ontology);
        if (format.isPrefixOWLOntologyFormat()) {
            // this is the map you need
            Map<String, String> map = format.asPrefixOWLOntologyFormat().getPrefixName2PrefixMap();
            for (Map.Entry<String, String> entry : map.entrySet()) {
                String prefix = entry.getKey();
                String iri = entry.getValue();
                // do not include prefix :
                if (!Objects.equals(prefix, ":")) {
                    iriResolver.setPrefix(prefix, iri);
                }
            }
        }
    }

    private void loadPrefixes(PrefixManager prefixManager) {
        OWLDocumentFormat format = manager.getOntologyFormat(ontology);
        if (format.isPrefixOWLOntologyFormat()) {
            // this is the map you need
            Map<String, String> map = format.asPrefixOWLOntologyFormat().getPrefixName2PrefixMap();
            for (Map.Entry<String, String> entry : map.entrySet()) {
                String prefix = entry.getKey();
                String iri = entry.getValue();
                // do not include prefix :
                if (!Objects.equals(prefix, ":")) {
                    prefixManager.setPrefix(prefix, iri);
                }
            }
        }
    }
    public void loadOntology(String filepath, ArrayList<String> imports) {
        manager = OWLManager.createOWLOntologyManager();
        try {
            if (imports!=null){
                for (String importDir : imports) {
                    File importDirFile = new File(importDir);
                    if (importDirFile.exists()) {
                        AutoIRIMapper mapper = new AutoIRIMapper(importDirFile, true);
                        manager.getIRIMappers().add(mapper);
                    }
                }
            }
            ontology = manager.loadOntologyFromOntologyDocument(FileUtil.getInputStream(filepath));
        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
        }
        prefixManager = new DefaultPrefixManager(null, null, ontology.getOntologyID().getOntologyIRI().get().toString() + "#");
        loadPrefixes(prefixManager);
        allRules = getSWRLRuleEngine().getSWRLRules();
    }

    public void setOntology(OWLOntology ontology) {
        manager = OWLManager.createOWLOntologyManager();
        this.ontology = ontology;
        prefixManager = new DefaultPrefixManager(null, null, ontology.getOntologyID().getOntologyIRI().get().toString() + "#");
        allRules = getSWRLRuleEngine().getSWRLRules();

    }

    public OWLOntology getOntology() {
        return ontology;
    }

    public PrefixManager getPrefixManager() {
        return prefixManager;
    }

    public SWRLRuleEngine getSWRLRuleEngine() {
        IRIResolver iriResolver = SWRLAPIFactory.createIRIResolver(prefixManager.getDefaultPrefix());
        loadPrefixes(iriResolver);
        SWRLRuleEngine ruleEngine = SWRLAPIFactory.createSWRLRuleEngine(ontology, iriResolver);
        return ruleEngine;
    }

    public SWRLRuleRenderer getRuleRenderer() {
        IRIResolver iriResolver = SWRLAPIFactory.createIRIResolver(prefixManager.getDefaultPrefix());
        SWRLRuleRenderer swrlRuleRenderer = SWRLAPIInternalFactory.createSWRLRuleRenderer(ontology, iriResolver);
        return swrlRuleRenderer;
    }

    public ArrayList<SWRLAPIRule> getAllRules() {
        return new ArrayList<SWRLAPIRule>(allRules);
    }

    public SWRLAPIRule getSWRLRule(String ruleName) {
        ArrayList<SWRLAPIRule> allRules = getAllRules();
        for (SWRLAPIRule rule : allRules) {
            if (rule.getRuleName().equals(ruleName)) return rule;
        }
        return null;
    }

}
