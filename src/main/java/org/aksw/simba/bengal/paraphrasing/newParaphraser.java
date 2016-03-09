/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.aksw.simba.bengal.paraphrasing;

import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import simplenlg.framework.NLGFactory;
import simplenlg.lexicon.Lexicon;
import simplenlg.phrasespec.SPhraseSpec;
import simplenlg.realiser.english.Realiser;

/**
 *
 * @author aksw
 */
public class newParaphraser {
    
    public static void main(String args[]) throws Exception {
        
                          int value = 0;
                    Lexicon lexicon = Lexicon.getDefaultLexicon();
                    NLGFactory nlgFactory = new NLGFactory(lexicon);
                    Realiser realiser = new Realiser(lexicon);
                    MaxentTagger tagger = new MaxentTagger("models/english-left3words-distsim.tagger");
                     SPhraseSpec s = nlgFactory.createClause();
 
                    String sample = "Tartar's mouth country is Azerbaijan."
                            //+ "Azerbaijan's leader is Ilham Aliyev."
                           // + "Ilham Aliyev's successor is Artur Rasizade."
                           // + "Artur Rasizade's party is New Azerbaijan Party."
                            + "New Azerbaijan Party's headquarter is Baku.";
                    
                    //NLGElement s1 = nlgFactory.createSentence(sample);
                    
                    
                    String results = null;
                    String[] sentences = sample.split("\\.");
                    String[] paraphrases = sample.split("\\.");
                    String[] Phrase = null;
                    String[] Pos = null;
                    
                    for (int i = 0; i < sentences.length; i++) {
                    String tagged = tagger.tagString(sentences[i]);
                    System.out.println(tagged);
                    paraphrases[i] = tagged;
                    Phrase = sentences[i].split("[,;. ]+");
                    Pos = paraphrases[i].split("[,;. ]+");
                    }
 
                    for (int k = 0; k < Pos.length; k++) {
                           if(Pos[k].contains("_V")){
                                value = k;
                           }
                   
                    System.out.println(value);
                    System.out.println(Phrase[4]);
                    
                    s.setVerb(Phrase[value]);
                    
                            StringBuilder sb = new StringBuilder();
                                if (Phrase.length > 1) {
                                    sb.append(Phrase[0]);
                                        for (int w = 1; w < value; w++) {
                                            sb.append(" ").append(Phrase[w]); }
                    }
                       s.setSubject(sb.toString());
                       
                        //System.out.println(s.getVerb());
                       
                            StringBuilder sc = new StringBuilder();
                                if (Phrase.length > 1) {
                                    sc.append(Phrase[value+1]);
                                        for (int z = value+2; z < Phrase.length; z++) {
                                            sc.append(" ").append(Phrase[z]); }
                    }
                       s.setObject(sc.toString());
                    
                      // System.out.println(s.getObject());
                       
                       
        
         //String s1 = "The monkey was chased by Mary.";
        
       // s2.setFeature(Feature.TENSE, Tense.PRESENT); //Tense Exchange
        
         // s.setFeature(Feature.CONJUNCTION, s1);
        //s2.setFeature(Feature.NEGATED, true); //To negate the sentences based on VERBs.
       
        //s2.setFeature(Feature.PERFECT, true);
        //s.setFeature(Feature.PASSIVE, true);
        
        String output = realiser.realiseSentence(s);
       // System.out.println(output);
        
        
       
                }
    }
}
