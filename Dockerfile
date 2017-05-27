FROM java

ADD models /bengal/models
ADD dict /bengal/dict
ADD en_surface_forms.tsv /bengal/en_surface_forms.tsv

ADD src/main/resources/config/bengal.properties /bengal/src/main/resources/config/bengal.properties

ADD target/bengal-1.0.0-SNAPSHOT.jar /bengal/bengal.jar

WORKDIR /bengal

CMD java -cp bengal.jar org.aksw.simba.bengal.controller.BengalController $ENDPOINT $SELECTOR $OUTPUT $SEED $DOCUMENTS $WAITING_TIME $CLASSES