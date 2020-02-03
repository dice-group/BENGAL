package org.aksw.simba.bengal.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.aksw.gerbil.transfer.nif.Document;
import org.aksw.gerbil.transfer.nif.Marking;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class FileGenerator {
    File file = new File("document.txt");
    FileWriter fr;
    JSONArray jsonArray = new JSONArray();

    {
        try {
            fr = new FileWriter(file, false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    BufferedWriter br = new BufferedWriter(fr);

    public void getTriples(List<Statement> triples) throws IOException {
        JSONObject triplesObj = new JSONObject();
        Model model = ModelFactory.createDefaultModel();
        model.add(triples);
        RDFDataMgr.write(br, model, Lang.RDFJSON);
        br.newLine();
        for(int i=0;i<triples.size();i++){
            Statement statement = triples.get(i);
            Resource sub = statement.getResource();
            Property prop = statement.getPredicate();
            RDFNode obj = statement.getObject();
            triplesObj.put("subject",sub);
            triplesObj.put("predicate",prop);
            triplesObj.put("object",obj);
            jsonArray.add(triplesObj);
        }

    }

    public void getDocument(Document document) throws Exception {

        br.write(document.toString());
        br.newLine();
        String uri = document.getDocumentURI();
        String text = document.getText();
        List<Marking> markings = document.getMarkings();
        JSONObject docObj = new JSONObject();
        JSONArray docArray = new JSONArray();
        docObj.put("uri",uri);
        docObj.put("text",text);
        docArray.add(markings);
        for(int i=0;i<docArray.size();i++) {
            docObj.put("markings", docArray.toJSONString());
        }
        jsonArray.add(docObj);
        jsonDoc(jsonArray);

    }
    public void jsonDoc(JSONArray array){
        JSONArray jsonArray1 = new JSONArray();
        jsonArray1.add(array.toJSONString());
        try (FileWriter file = new FileWriter("document.json")) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            JsonParser jp = new JsonParser();
            JsonElement je = jp.parse(jsonArray1.toJSONString());
            String prettyJsonString = gson.toJson(je);
            file.write(prettyJsonString);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
