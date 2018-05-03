# BENGAL
Automatic Generation of Benchmarks for Entity Recognition and Linking

This project aims at delivering a framework for generating bechmarking data sets from structure data to the Named Entity Recognition (NER) and Disambiguation (NED a.k.a Entity Linking and Wikification) problems.

We hope you will enjoy using BENGAL!

### Support and Feedback
If you need help or you have questions do not hesitate to write an email to  <a href="mailto:michael.roeder@uni-paderborn.de"> Michael Röder</a>. Or use the issue tracker in the right sidebar.

### Running BENGAL

### How to run
```
1) Download the surface forms file which is available at ``https://hobbitdata.informatik.uni-leipzig.de/bengal/en_surface_forms.tsv.zip``.
2) Download the WordNet dictionary which is available at ``http://wordnetcode.princeton.edu/3.0/WordNet-3.0.tar.gz``.
3) After extracting these files upload them into ``https://github.com/dice-group/BENGAL/tree/master/data``. Note that only the ``dict`` of WordNet is necessary. 
4) mvn clean install
```

###Portuguese Version

The Portuguese version can be found at ``https://github.com/dice-group/BENGAL/tree/portuguese``.