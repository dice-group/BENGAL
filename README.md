# BENGAL

Automatic Generation of Benchmarks for Entity Recognition and Linking

This project aims at delivering a framework for generating bechmarking data sets from structure data to the Named Entity Recognition (NER) and Disambiguation (NED a.k.a Entity Linking and Wikification) problems.

We hope you will enjoy using BENGAL!

### Support and Feedback

If you need help or you have questions do not hesitate to use the issue tracker.

### Setting up BENGAL

1. Download the surface forms file which is available at `https://hobbitdata.informatik.uni-leipzig.de/bengal/en_surface_forms.tsv.zip`.
2. Download the WordNet dictionary which is available at `http://wordnetcode.princeton.edu/3.0/WordNet-3.0.tar.gz`.
3. After extracting these files upload them into `https://github.com/dice-group/BENGAL/tree/master/data`. Note that only the `dict` folder of WordNet is necessary. 
4. mvn clean install
5. to run the program from cli use the jar file with the postfix `exec`.

### Executing BENGAL

Example Usage:
```
java -cp ./target/BENGAL-1.0-SNAPSHOT-exec.jar org.aksw.simba.bengal.controller.BengalController -st sym
```
CLI Options:
```
 -mns,--minsentence <arg>       Minimum number of sentences, default: 3
 -mxs,--maxsentence <arg>       Maximum number of sentences, default: 10
 -n,--numberofdocuments <arg>   Number of documents, default: 1
 -o,--onlyobjectprops           Use only object properties
 -pp,--paraphrase               Use Paraphrasing
 -pr,--pronouns                 Use Pronouns
 -sd,--seed <arg>               Number of Seeds, default: Current System
                                time in milliseconds
 -se,--sparqlendpoint <arg>     Sparql Endpoint, default:
                                http://dbpedia.org/sparql
 -sf,--surfaceforms             Use Surface-Forms
 -st,--selectortype <arg>       Selector Type ('star', 'hybrid', 'path',
                                'sym' or 'summary')
 -wt,--waittime <arg>           Wait time between documents in
                                milliseconds, default: 500
```
### Portuguese Version

The Portuguese version can be found at <a href=https://github.com/dice-group/BENGAL/tree/portuguese>click here</a>.
