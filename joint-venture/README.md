# joint venture

Was will ich hier bauen?
Eine Stammdatenverwaltung mit CQRS. Warum? Weil es untypisch ist und ich trotzdem ausprobieren möchte, ob es gut funktioniert ;)

Todos:
- akka-persistence mit MongoDB
    - Casbah Treiber (Scala) anstatt Standard MongoDB Treiber (Java)?
    - Serialisierung der Events / des EventStore in JSON/BSON Format?
- akka-http anstatt SparkJava für REST Frontend?

- Auslesen aus DB unter Berücksichtung der noch ausstehenden Events (Priority-Lane?)
- Beim Lesen von Usern auf der DB werden alle noch fehlenden Events dynamisch angewendet (Reihenfolge?)

- Tests mit simulierter Last implementieren

- Clustering
- Backup-Master

- Visualisierung der Events (Timeline?)

- Microservices, 2 frontal (user & ?), 1 noch dahinter (?)
- jeder frontale Microservice mit eigenem (Rest) Toplevel Pfad, zB /user/
- zykl. Referenz zwischen Microservices erlaubt?
- Microservices mehrfach starten (für mehr Worker, Cluster)

- Events auf Bus/MQ ablegen und von anderen (dahinter?) Microservice verarbeiten

- fertig verarbeitete Events im Master freigeben (Speicher), aber dennoch kennen (da persistiert)?
- Ausblick: GUI

https://docs.mongodb.org/getting-started/java/indexes/
http://mongodb.github.io/mongo-java-driver/3.1/driver-async/reference/
http://mongodb.github.io/mongo-java-driver-reactivestreams/1.2/getting-started/quick-tour-primer/

http://letitcrash.com/post/29044669086/balancing-workload-across-nodes-with-akka-2
