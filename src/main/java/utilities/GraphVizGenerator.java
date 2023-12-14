package utilities;

import guru.nidi.graphviz.attribute.Attributes;
import guru.nidi.graphviz.attribute.Label;
import guru.nidi.graphviz.attribute.Shape;
import guru.nidi.graphviz.attribute.Style;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import model.EdgeTypeEnum;
import model.GraphListsForViz;
import model.NodeConnection;
import model.NodeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import guru.nidi.graphviz.model.Factory;
//import guru.nidi.graphviz.model.Label;
import guru.nidi.graphviz.model.Link;
import guru.nidi.graphviz.model.LinkSource;
import guru.nidi.graphviz.model.LinkTarget;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.model.MutableNode;
import javax.imageio.ImageIO;
import guru.nidi.graphviz.model.Factory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class GraphVizGenerator {

    private static final String CLASS = "Class";
    private static final String PROPERTY = "Property";
    private static final String BICOLLECTION = "BuiltInCollection";
    private static final String VARIABLE = "Variable";
    private static final int PIXEL_PER_NODE = 250;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public boolean produceImage(GraphListsForViz graphListsForViz, File out) {
        NodeInfo[] nodes = graphListsForViz.getNodes();
        ArrayList<MutableNodeExt> mutableNodes = new ArrayList<>();
        MutableGraph graph =  Factory.mutGraph().setDirected(true);
        for (NodeInfo node : nodes) {
            MutableNode mutNode = Factory.mutNode(node.getCaption());
            switch (node.getType()) {
                case "Class":
                    mutNode.add((Attributes) Shape.RECTANGLE);
                    break;
                case "Property":
                    mutNode.add((Attributes) Shape.ELLIPSE);
                    break;
                case "BuiltInCollection":
                    mutNode.add((Attributes) Shape.DIAMOND);
                    break;
                case "Variable":
                    mutNode.add((Attributes) Shape.TRAPEZIUM);
                    break;
            }
            mutableNodes.add(new MutableNodeExt(mutNode, node.getType()));
        }
        NodeConnection[] connections = graphListsForViz.getConnections();
        for (NodeConnection connection : connections) {
            String childCaption = connection.getChild().getCaption();
            String parentCaption = connection.getParent().getCaption();
            MutableNodeExt child = null, parent = null;
            for (MutableNodeExt mutableNodeExt : mutableNodes) {
                if (mutableNodeExt.getNode().name().toString().equals(childCaption)) {
                    child = mutableNodeExt;
                    continue;
                }
                if (mutableNodeExt.getNode().name().toString().equals(parentCaption))
                    parent = mutableNodeExt;
            }
            if (child != null && parent != null) {
                Link childLinkTo = child.getNode().linkTo();
                if ((parent.getType().equals("Property") || child.getType().equals("Property")) && connection.getType().equals(EdgeTypeEnum.ObjectProperty))
                    childLinkTo = childLinkTo.with((Attributes) Style.DASHED);
                if (child.getType().equals("Class") && parent.getType().equals("Property") && connection.getType().equals(EdgeTypeEnum.Normal)) {
                    childLinkTo = childLinkTo.with((Attributes) Style.SOLID);
                } else if ((parent.getType().equals("Property") || parent.getType().equals("BuiltInCollection")) && child.getType().equals("Variable") &&
                        connection.getLabel() != null) {
                    childLinkTo = childLinkTo.with((Attributes) Label.of(connection.getLabel()));
                }
                parent.getNode().addLink((LinkTarget) childLinkTo);
                graph.add((LinkSource) parent.getNode());
            }
        }
        if (connections.length == 0)
            for (MutableNodeExt node : mutableNodes)
                graph.add((LinkSource) node.getNode());
        int imageWidth = nodes.length * 250;
        BufferedImage image = Graphviz.fromGraph(graph).width(imageWidth).render(Format.PNG).toImage();
        try {
            out.createNewFile();
            ImageIO.write(image, "png", out);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

}
