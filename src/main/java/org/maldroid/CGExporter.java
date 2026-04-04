package org.maldroid;

import it.uniroma1.dis.wsngroup.gexf4j.core.EdgeType;
import it.uniroma1.dis.wsngroup.gexf4j.core.Gexf;
import it.uniroma1.dis.wsngroup.gexf4j.core.Graph;
import it.uniroma1.dis.wsngroup.gexf4j.core.Mode;
import it.uniroma1.dis.wsngroup.gexf4j.core.Node;
import it.uniroma1.dis.wsngroup.gexf4j.core.data.AttributeClass;
import it.uniroma1.dis.wsngroup.gexf4j.core.data.AttributeList;
import it.uniroma1.dis.wsngroup.gexf4j.core.data.AttributeType;
import it.uniroma1.dis.wsngroup.gexf4j.core.impl.GexfImpl;
import it.uniroma1.dis.wsngroup.gexf4j.core.impl.StaxGraphWriter;
import it.uniroma1.dis.wsngroup.gexf4j.core.impl.data.AttributeListImpl;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

public class CGExporter {
    private Gexf gexf;
    private Graph graph;
    private AttributeList attrList;

    public CGExporter() {
        this.gexf = new GexfImpl();
        this.graph = this.gexf.getGraph();
        this.gexf.getMetadata().setCreator("MalDroid-Analyzer").setDescription("App method invoke graph");
        this.gexf.setVisualization(true);
        this.graph.setDefaultEdgeType(EdgeType.DIRECTED).setMode(Mode.STATIC);
        this.attrList = new AttributeListImpl(AttributeClass.NODE);
        this.graph.getAttributeLists().add(attrList);
        this.attrList.createAttribute("0", AttributeType.STRING, "codeArray");
    }

    public void exportMIG(String graphName, String storeDir) {
        String outPath = storeDir + "/" + graphName + ".gexf";
        StaxGraphWriter graphWriter = new StaxGraphWriter();
        File f = new File(outPath);
        try (Writer out = new FileWriter(f, false)) {
            graphWriter.writeToStream(this.gexf, out, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Node getNodeByID(String id) {
        for (Node node : this.graph.getNodes())
            if (node.getId().equals(id)) return node;
        return null;
    }

    public void linkNodeByID(String sourceID, String targetID) {
        Node sourceNode = getNodeByID(sourceID);
        Node targetNode = getNodeByID(targetID);
        if (sourceNode == null || targetNode == null) return;
        if (!sourceNode.hasEdgeTo(targetID))
            sourceNode.connectTo(sourceID + "-->" + targetID, "", EdgeType.DIRECTED, targetNode);
    }

    public void createNode(String id) {
        if (getNodeByID(id) != null) return;
        Node node = this.graph.createNode(id);
        node.setLabel(id);
    }
}
