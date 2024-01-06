package com.github.vchavezb.controller;

import com.github.vchavezb.model.*;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.SWRLAtom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.swrlapi.core.SWRLAPIRule;
import org.swrlapi.core.SWRLRuleRenderer;
import com.github.vchavezb.utilities.AOWLNEngine;
import com.github.vchavezb.utilities.GraphVizGenerator;
import com.github.vchavezb.utilities.OWLUtil;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class AOWLNServiceFacade {
    private OWLUtil owlUtil;
    private AOWLNEngine aowlnEngine;
    private ArrayList<SWRLAPIRule> allRules;
    private SWRLRuleRenderer ruleRenderer;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final String engineFile = "aowln-image-engine.jar";
    private final String engineEnv = "AOWLN_ENGINE_PATH";
    private boolean isLinux() {
        return System.getProperty("os.name").toLowerCase().contains("linux");
    }

    public AOWLNServiceFacade() {
        this.owlUtil = new OWLUtil();
        this.aowlnEngine = new AOWLNEngine();
    }

    /**
     * Load the ontology and obtain the list of SWRL Rules
     * @param file String path to ontology
     * @return
     */
    public ArrayList<SWRLAPIRule> getOntologyRules(String file){
        // Create a SWRL rule engine using the SWRLAPI
        this.owlUtil.loadOntology(file);
        aowlnEngine.setPrefixManager(this.owlUtil.getPrefixManager());
        allRules = owlUtil.getAllRules();
        this.ruleRenderer = owlUtil.getRuleRenderer();
        ArrayList<String> SWRLRulesAsString = new ArrayList<>();
        for (SWRLAPIRule swrlapiRule : allRules) {
            SWRLRulesAsString.add(swrlapiRule.getRuleName() + ": " + ruleRenderer.renderSWRLRule(swrlapiRule));
        }
        return allRules;
    }

    public ArrayList<SWRLAPIRule> getOntologyRules(OWLOntology ontology) {
        owlUtil.setOntology(ontology);
        allRules = owlUtil.getAllRules();
        this.ruleRenderer = owlUtil.getRuleRenderer();
        ArrayList<String> SWRLRulesAsString = new ArrayList<>();
        for (SWRLAPIRule swrlapiRule : allRules) {
            SWRLRulesAsString.add(swrlapiRule.getRuleName() + ": " + ruleRenderer.renderSWRLRule(swrlapiRule));
        }
        return allRules;
    }

    /**
     * Produce an image from an SWRLRule
     * @param OutDir Directory output of the image
     * @param base_name Base name used for the rule. Two images will be generated with the format
     *                      base_name-head.png
     *                      base_name-body.png
     * @param swrlRule SWRL Api rule
     * @return True if success
     */
    public boolean produceRuleImage(Path OutDir, String base_name, SWRLAPIRule swrlRule) {
        HashSet<SWRLAtom> body = new HashSet<SWRLAtom>(swrlRule.getBody());
        HashSet<SWRLAtom> head = new HashSet<SWRLAtom>(swrlRule.getHead());
        ArrayList<CustomSWRLAtom> bodyTree = aowlnEngine.createSWRLAtomsForTree(body);
        ArrayList<CustomSWRLAtom> headTree = aowlnEngine.createSWRLAtomsForTree(head);

        //Remove Dependency for AOWLN Convention: By creating necessary concepts
        List<ClassAtomCustom> bodyClasses = new ArrayList<>();
        List<ObjectPropertyAtomCustom> bodyOP = new ArrayList<>();
        List<DataPropertyAtomCustom> bodyDP = new ArrayList<>();
        List<ClassAtomCustom> headClasses = new ArrayList<>();
        List<ObjectPropertyAtomCustom> headOP = new ArrayList<>();
        List<DataPropertyAtomCustom> headDP = new ArrayList<>();

        for (CustomSWRLAtom swrlAtom : bodyTree) {
            if (swrlAtom instanceof ClassAtomCustom) {
                bodyClasses.add((ClassAtomCustom) swrlAtom);
            } else if (swrlAtom instanceof DataPropertyAtomCustom) {
                bodyDP.add((DataPropertyAtomCustom) swrlAtom);
            } else if (swrlAtom instanceof ObjectPropertyAtomCustom) {
                bodyOP.add((ObjectPropertyAtomCustom) swrlAtom);
            }
        }
        for (CustomSWRLAtom swrlAtom : headTree) {
            if (swrlAtom instanceof ClassAtomCustom) {
                headClasses.add((ClassAtomCustom) swrlAtom);
            } else if (swrlAtom instanceof DataPropertyAtomCustom) {
                headDP.add((DataPropertyAtomCustom) swrlAtom);
            } else if (swrlAtom instanceof ObjectPropertyAtomCustom) {
                headOP.add((ObjectPropertyAtomCustom) swrlAtom);
            }
        }

        List<String> keysToCheck = new ArrayList();
        for (ObjectPropertyAtomCustom op : bodyOP) {
            keysToCheck.add(op.getFirstArgument());
            keysToCheck.add(op.getLastArgument());
        }
        for (DataPropertyAtomCustom dp : bodyDP) {
            keysToCheck.add(dp.getFirstArgument());
        }

        //create default concepts for body
        for (String key : keysToCheck) {
            if (!checkIfConceptExists(key, bodyClasses)) {
                ClassAtomCustom defaultClassAtom = new ClassAtomCustom(key, "Thing\n(" + key + ")");
                bodyClasses.add(0, defaultClassAtom);
                bodyTree.add(0, defaultClassAtom);
            }
        }

        keysToCheck = new ArrayList<>();
        for (ObjectPropertyAtomCustom op : headOP) {
            keysToCheck.add(op.getFirstArgument());
            keysToCheck.add(op.getLastArgument());
        }
        for (DataPropertyAtomCustom dp : headDP) {
            keysToCheck.add(dp.getFirstArgument());
        }

        //create default concepts for head
        for (String key : keysToCheck) {
            if (!checkIfConceptExists(key, headClasses)) {
                if (!checkIfConceptExists(key, bodyClasses)) {
                    ClassAtomCustom defaultClassAtom = new ClassAtomCustom(key, "Thing\n(" + key + ")");
                    headClasses.add(0, defaultClassAtom);
                    headTree.add(0, defaultClassAtom);
                } else {
                    //getClassFrom Body
                    ClassAtomCustom conceptFromBody = findConceptByKey(key, bodyClasses);
                    headClasses.add(0, conceptFromBody);
                    headTree.add(0, conceptFromBody);
                }
            }
        }

        GraphListsForViz vizListBody = aowlnEngine.megaAlgorithmus(bodyTree);
        GraphListsForViz vizListHead = aowlnEngine.megaAlgorithmus(headTree);
        GraphVizGenerator graphVizGenerator = new GraphVizGenerator();

        File body_img = Paths.get(OutDir.toString(), base_name + "-"+"body" + ".svg").toFile();
        boolean res = graphVizGenerator.produceImage(vizListBody, body_img);
        if (!res) {
            return false;
        }
        logger.info("Generated SWRL Rule [Body]\nRule: " +
                    swrlRule.getBody().toString()+"\nPath: "+body_img);
        File head_img = Paths.get(OutDir.toString(), base_name + "-"+"head" + ".svg").toFile();
        res = graphVizGenerator.produceImage(vizListHead, head_img);
        if (!res) {
            return false;
        }
        logger.info("Generated SWRL Rule [Head]\nRule: " +
                swrlRule.getHead().toString()+"\nPath: "+head_img);
        return true;
    }

    public List<String> getRulesAsStrings(ArrayList<SWRLAPIRule> rules) {
        ArrayList<String> SWRLRulesAsStrings = new ArrayList<>();
        for (SWRLAPIRule swrlapiRule : rules) {
            SWRLRulesAsStrings.add(swrlapiRule.getRuleName() + ": " + ruleRenderer.renderSWRLRule(swrlapiRule));
        }
        return SWRLRulesAsStrings;
    }

    private boolean checkIfConceptExists(String key, List<ClassAtomCustom> ruleFragment) {
        boolean exist = false;

        for (ClassAtomCustom c : ruleFragment) {
            if (c.getKey().equals(key)) {
                exist = true;
            }
        }
        return exist;
    }

    private ClassAtomCustom findConceptByKey(String key, List<ClassAtomCustom> ruleFragment) {
        ClassAtomCustom relevant = null;
        for (ClassAtomCustom c : ruleFragment) {
            if (c.getKey().equals(key)) {
                relevant = c;
            }
        }
        return relevant;
    }
}
