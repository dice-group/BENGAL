/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.aksw.simba.bengal.paraphrasing;

import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import simplenlg.features.Feature;
import simplenlg.framework.NLGElement;
import simplenlg.framework.NLGFactory;
import simplenlg.lexicon.Lexicon;
import simplenlg.phrasespec.SPhraseSpec;
import simplenlg.realiser.english.Realiser;

/**
 *
 * @author DiegoMoussallem
 */
public class newParaphraser implements ParaphraseService{
    
    @Override
    public String paraphrase(String originalText) {
                    int value = 0;
                    Lexicon lexicon = Lexicon.getDefaultLexicon();
                    NLGFactory nlgFactory = new NLGFactory(lexicon);
                    Realiser realiser = new Realiser(lexicon);
                    MaxentTagger tagger = new MaxentTagger("models/english-left3words-distsim.tagger");
                    SPhraseSpec s = nlgFactory.createClause();
 
                    NLGElement s1 = nlgFactory.createSentence(originalText);
                    
                    
                    String[] sentences = originalText.split("[.]+");
                    int vSentences = 0;
                    for (int i = 0; i < sentences.length; i++) {
                        vSentences = vSentences + 1;
                        
                    String[] Phrase = sentences[i].split("[,;. ]+");
                    String[] Pos = sentences[i].split("[,;. ]+");
                    

                    for (int k = 0; k < Pos.length; k++) {
                        Pos[k] = tagger.tagString(Pos[k]);
                    }
                    
                    for (int k = 0; k < Pos.length; k++) {
                           if(Pos[k].contains("_V")){
                                value = k;
                           }
                    }
                    
                    s.setVerb(Phrase[value]);
                    
                            StringBuilder sb = new StringBuilder();
                                if (Phrase.length > 1) {
                                    sb.append(Phrase[0]);
                                        for (int w = 1; w < value; w++) {
                                           sb.append(" ").append(Phrase[w]); }
                    }
                       s.setSubject(sb.toString());
                       

                       
                            StringBuilder sc = new StringBuilder();
                                if (Phrase.length > 1) {
                                    sc.append(Phrase[value+1]);
                                        for (int z = value+2; z < Phrase.length; z++) {
                                            sc.append(" ").append(Phrase[z]); }
                    }
                       s.setObject(sc.toString());
       
        s.setFeature(Feature.PERFECT, true);
        s.setFeature(Feature.PASSIVE, true);
        String output = realiser.realiseSentence(s);
        
        String[] paraphraser = output.split("[,; ]+");
        int find = 0;
        
            for (int k = 0; k < paraphraser.length; k++) {
                if(paraphraser[k].equals("been")){
                    find++;
                    if(find>1){
                    paraphraser[k] = paraphraser[k].replace("been", "");
                }
                }
                if(paraphraser[k].equals("by")){
                    find++;
                    if(find>1){
                    paraphraser[k] = paraphraser[k].replace("by", "the");
                }
                }
                    }
            
            StringBuilder end = new StringBuilder();
                                if (paraphraser.length > 1) {
                                    end.append(paraphraser[0]);
                                        for (int z = 1; z < paraphraser.length; z++) {
                                            end.append(" ").append(paraphraser[z]); }
                    }
                       output = end.toString();
        
        sentences[i] = output;
        
                    }
                    for (int i = 0; i < sentences.length; i++) {
                        System.out.println("Paraphrased Text:");
                        System.out.println(sentences[i]);
                    }
                                StringBuilder paraphrased = new StringBuilder();
                                if (sentences.length > 1) {
                                    paraphrased.append(sentences[0]);
                                        for (int z = 1; z < sentences.length; z++) {
                                            paraphrased.append(" ").append(sentences[z]); }
                    }
                      String output = paraphrased.toString();
        return output;
    }
    }
