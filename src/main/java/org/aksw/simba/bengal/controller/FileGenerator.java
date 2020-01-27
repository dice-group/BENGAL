package org.aksw.simba.bengal.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.sun.jndi.toolkit.url.Uri;
import org.aksw.gerbil.transfer.nif.Document;
import org.aksw.gerbil.transfer.nif.Marking;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Iterator;
import java.util.List;

public class FileGenerator {
    private static final Logger LOGGER = LoggerFactory.getLogger(BengalController.class);

    public Document getDoc(Document doc, List<Statement> triples) throws Exception {
        Document document = doc;
        getData(document.toString(), triples.toString());
        return document;
    }

    public String getData(String doc, String triples) throws Exception {
        getFile("Document: " + "\n" + doc + "\n" + "Triples: "+"\n" + triples);
        return doc;
    }

    public void getFile(String data) throws Exception {
        File outputDirectory = new File("document.txt");
        FileOutputStream fout = new FileOutputStream(outputDirectory);
        BufferedWriter bout = new BufferedWriter(new FileWriter(outputDirectory, true));
        String[] arr = data.split(",");
        for (String t : arr) {
            bout.newLine();
            bout.write(t + ",");
        }
        bout.flush();
        bout.close();
        fout.close();
        LOGGER.info("Text file generated");
    }

    public void getJSONTriples(List<Statement> inData) throws Exception{
        FileOutputStream fout = new FileOutputStream("triples.json");
        Model model = ModelFactory.createDefaultModel();
        model.add(inData);
        RDFDataMgr.write(fout, model, Lang.RDFJSON);
    }

    public void getJSONDoc (Document document)throws Exception {
        //parse triple json file
        JSONParser parser = new JSONParser();
        Object obj = parser.parse(new FileReader("triples.json"));
        JSONObject jsonObject = (JSONObject) obj;
        JSONObject combine = new JSONObject();

        for(Iterator iterator = jsonObject.keySet().iterator(); iterator.hasNext();) {
            String key = (String) iterator.next();
            //LOGGER.info(String.valueOf(jsonObject.get(key)));
        }

        //document json object
        String uri = document.getDocumentURI();
        String text = document.getText();
        List<Marking> markings = document.getMarkings();
        JSONObject obj1 = new JSONObject();
        obj1.put("uri", uri);
        obj1.put("text", text);
        JSONArray array = new JSONArray();
        array.add(markings);
        for(int i=0;i<array.size();i++) {
            obj1.put("markings", array.toJSONString());
        }
        combine.put("triples",jsonObject);
        combine.put("document",obj1);
        try (FileWriter file = new FileWriter("document.json")) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            JsonParser jp = new JsonParser();
            JsonElement je = jp.parse(combine.toJSONString());
            String prettyJsonString = gson.toJson(je);
            file.write(prettyJsonString);
            LOGGER.info("JSON file generated");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
// to merge files
   /* public void combineFile() throws Exception{
        PrintWriter pw = new PrintWriter("finalDoc.json");
        BufferedReader br = new BufferedReader(new FileReader("triples.json"));
        String line = br.readLine();
        while (line != null)
        {
            pw.println(line);
            line = br.readLine();
        }
        br = new BufferedReader(new FileReader("document.json"));
        line = br.readLine();
        while(line != null)
        {
            pw.println(line);
            line = br.readLine();
        }
        pw.flush();
        br.close();
        pw.close();
        System.out.println("Files merged");
    }
*/

}





