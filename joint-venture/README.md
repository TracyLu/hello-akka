# joint venture

Was will ich hier bauen?
Eine Stammdatenverwaltung mit CQRS. Warum? Weil es untypisch ist und ich trotzdem ausprobieren möchte, ob es gut funktioniert ;)

Details:
- REST API zum Hinzufügen, Auslesen, Ändern und Löschen (CRUD) von Personen
- Hinzufügen, Ändern und Löschen via Command zu Akka Actors
- Command in (persistiertes) Event umwandeln und auf Eventbus ablegen
- Worker lauschen auf Eventbus zur Verarbeitung der Events
- Auslesen aus DB unter Berücksichtung der noch ausstehenden Events (Priority-Lane?)
- Clustering
