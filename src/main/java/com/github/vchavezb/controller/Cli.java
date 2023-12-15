package com.github.vchavezb.controller;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.swrlapi.core.SWRLAPIRule;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class Cli {
    public static void main(String[] args) throws IOException {
        Logger logger = LoggerFactory.getLogger(Cli.class);
        ArgumentParser parser = ArgumentParsers.newFor("AOWLN Standalone").build()
                .defaultHelp(true)
                .description("Aided Owl Notation generation for SWRL");
        parser.addArgument("ontology").help("Ontology input file with swrl rules");
        parser.addArgument("outputDir").help("Output directory for rules");
        parser.addArgument("-name","-n")
                .help("Concatenate the rule name (i.e., rdfs:label)\n"+
                        "to each rule. Example rule_1_MyRuleNameFromLabel.png\n"+
                        "default [true]")
                .type(Boolean.class);
        parser.setDefault("name",false);
        Namespace ns = null;
        try {
            ns = parser.parseArgs(args);
        } catch (ArgumentParserException e) {
            parser.handleError(e);
            System.exit(1);
        }
        String ontologyPath = ns.get("ontology").toString();
        String outputPath = ns.get("outputDir").toString();

        AOWLNServiceFacade facade;
        facade = new AOWLNServiceFacade();
        logger.info("Loading ontology "+ ontologyPath);
        ArrayList<SWRLAPIRule> swrlRules = facade.getOntologyRules(ontologyPath);
        File outDir = new File(outputPath);
        if (!outDir.exists()) {
            if (!outDir.mkdir()) {
                logger.error("Could not create output directory");
            }
        }
        logger.info("Creating rules");
        for (int rule_idx = 0; rule_idx < swrlRules.size(); rule_idx++) {
            SWRLAPIRule rule = swrlRules.get(rule_idx);
            String image_name = "rule_"+(rule_idx+1);
            if (ns.getBoolean("name")) {
                image_name+="_"+rule.getRuleName();
            }
            boolean res = facade.produceRuleImage(outDir.toPath(), image_name, rule);
        }

    }
}