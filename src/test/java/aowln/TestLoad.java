package aowln;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import controller.AOWLNServiceFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.swrlapi.core.SWRLAPIRule;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;

import static org.junit.Assert.*;

public class TestLoad {
    private AOWLNServiceFacade facade;
    File tmpFolder = null;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());


    @BeforeClass
    public static void setUpClass() {

    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
        tmpFolder = new File("tmp" + new Date().getTime());
        tmpFolder.mkdir();
    }

    private static void deleteFiles(File folder){
        String[]entries = folder.list();
        for(String s: entries){
            File currentFile = new File(folder.getPath(),s);
            if(currentFile.isDirectory()){
                deleteFiles(currentFile);
            }
            else{
                currentFile.delete();
            }
        }
        folder.delete();
    }
    @After
    public void tearDown() {
        deleteFiles(tmpFolder);
    }
    @org.junit.Test
    public void loadTest() {
        this.facade = new AOWLNServiceFacade();
        ArrayList<SWRLAPIRule> swrlrules = this.facade.getOntologyRules("simple.ttl");

        assertEquals(
                "Test swrl rules loaded size",
                swrlrules.size(),
                2
        );
        for (int i = 0; i < swrlrules.size(); i++) {
            SWRLAPIRule rule = swrlrules.get(i);
            boolean res = this.facade.produceRuleImages(tmpFolder.toPath(),"rule_"+i+"_"+rule.getRuleName(),rule);
            assertTrue ("Test swrl rules image created", res);
        }
    }
}


