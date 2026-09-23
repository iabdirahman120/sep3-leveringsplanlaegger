# Projektforslag, SEP3

**Arbejdstitel:** Leveringsplanlægger: leveringskoordinering for en forhandler med flere lagre
**Dato:** 2026-09-23
**Gruppe:** [navne udfyldes]
**Vejledere:** [udfyldes]

---

## 1. Problem og baggrund

En dansk forhandler af hvidevarer leverer fra flere lagre (fx København, Aarhus, Odense) med egne chauffører. Koordineringen sker i dag i delte Microsoft To Do-lister: én afkrydsning pr. levering, ingen historik, ingen ejer, intet bevis for hvem der modtog varen.

Konsekvenserne er konkrete. Når en levering mislykkes (ingen hjemme, adgang nægtet), forsvinder historikken når opgaven oprettes igen. Disponenten kan ikke se belastning pr. chauffør eller pr. lager. Ingen kan bagefter svare på "hvem flyttede den ordre, og hvornår".

Domænet er inspireret af et reelt behov, som et gruppemedlem har arbejdet med. Systemet i dette projekt bygges fra bunden i Java og C#. Ingen eksisterende kode genbruges, kun problemforståelsen, datamodellen og forretningsreglerne.

## 2. Aktører

| Aktør | Rolle | Behov |
|-------|-------|-------|
| Disponent (admin) | Kontorpersonale. Opretter ordrer, tildeler chauffør, overvåger alle leveringer, kan overtage en ordre når chaufføren ikke kan | Desktop-dashboard med overblik over alle lagre |
| Chauffør | Ansat, knyttet til ét lager. Ser kun egne leveringer, flytter status, registrerer modtager | Telefonvenlig visning, få tryk |
| Kunde | Privat husstand, virksomhed eller byggeplads. Modtager varen | Ikke bruger af systemet. Kundetypen modelleres, fordi den styrer leveringsvindue og kontaktfelter |

## 3. Funktionalitet (MVP)

Det der skal virke end-to-end og vises i demovideoen:

1. Login med to roller og forskellige visninger.
2. Opret ordre med kunde, lager, varelinjer og leveringsvindue. Lagerbeholdning tjekkes, ordren afvises hvis der ikke er varer nok.
3. Tildel chauffør. Kun chauffører fra ordrens lager kan vælges.
4. Chaufføren flytter ordren gennem statusforløbet fra sin telefon og registrerer bevis for levering (modtagers navn).
5. Tidslinje pr. ordre: hver ændring logges med aktør og tidsstempel. Loggen kan ikke redigeres.
6. Dashboard for disponenten: forsinkede leveringer, utildelte ordrer, belastning pr. chauffør og pr. lager.
7. Deaktivér chauffør: adgang lukkes, aktive sessioner slettes, tildelte ordrer frigives.

### Statusmaskine

Én overgangstabel, defineret ét sted og håndhævet på serveren. Brugerfladen viser kun de knapper brugeren må trykke på, men serveren tjekker igen før der skrives.

| Fra | Til | Admin | Chauffør (egen ordre) |
|-----|-----|:-----:|:---------------------:|
| pending | assigned | ja | nej |
| pending | cancelled | ja | nej |
| assigned | picked_up | ja | ja |
| assigned | pending | ja | nej |
| assigned | cancelled | ja | nej |
| picked_up | in_transit | ja | ja |
| picked_up | failed | ja | ja |
| in_transit | delivered | ja | ja |
| in_transit | failed | ja | ja |
| failed | assigned | ja | nej |
| failed | cancelled | ja | nej |
| delivered, cancelled | alt | nej | nej |

`failed` er ikke en sluttilstand. En mislykket levering er normal i hvidevarelogistik og skal kunne planlægges igen uden at ordrens historik går tabt.

## 4. Afgrænsning

Uden for scope, navngivet så grænsen er tydelig:

- Kundenotifikationer (SMS, e-mail) og offentlig sporingsside.
- Ruteoptimering og kort. Ordren har adresse og leveringsvindue, rækkefølgen på chaufførens dag er manuel.
- Integration til ERP eller webshop. Ordrer oprettes i systemet.
- Returlogistik. En mislykket levering registreres og kan omplanlægges, men varens vej tilbage til lageret følges ikke.
- Nulstilling af adgangskode via e-mail, mørkt tema og andre bekvemmeligheder.

## 5. Arkitektur

Tre processer og én database. Alle skrivninger går gennem samme vej.

```
+---------------------------+        REST + SignalR        +---------------------------+
|  Klient (C#)              | <--------------------------> |  App-server / BFF (C#)    |
|  Blazor WebAssembly       |                              |  ASP.NET Core             |
|  admin-shell (desktop)    |                              |  cookie + HMAC, rate limit|
|  driver-shell (telefon)   |                              |  REST-kontrakt, SignalR   |
+---------------------------+                              |  DTO-validering, KPI'er   |
                                                           +-------------+-------------+
                                                                         |  gRPC
                                                                         v
                                                           +---------------------------+
                                                           |  Domæneserver (Java)      |
                                                           |  Spring Boot, gRPC        |
                                                           |  statusmaskine, ejerskab, |
                                                           |  lagerregler, audit,      |
                                                           |  users + sessions         |
                                                           |  @Transactional, @Version |
                                                           +-------------+-------------+
                                                                         |  JDBC / JPA
                                                                         v
                                                           +---------------------------+
                                                           |  PostgreSQL (Docker)      |
                                                           +---------------------------+
```

| Komponent | Sprog og teknologi | Ansvar |
|-----------|--------------------|--------|
| Klient | C#, Blazor WebAssembly, hostet af app-serveren | Admin-dashboard til desktop, chaufførvisning til telefon. To shells i én app |
| App-server (BFF) | C#, ASP.NET Core | Session-cookie med HMAC, rate limit på login, REST-API mod klienten, SignalR-push af ændringer til åbne dashboards, validering af input, aggregering af dashboard-tal, grov rollekontrol pr. endpoint |
| Domæneserver | Java, Spring Boot, gRPC, Spring Data JPA | Ejer databasen inkl. brugere og sessioner. Login-verifikation, statusmaskine, ejerskabstjek, lagermatch ved tildeling, audit-log, transaktioner og optimistisk låsning |
| Database | PostgreSQL i Docker | Ti tabeller: warehouses, users, sessions, customers, products, inventory, orders, order_items, order_events, delivery_proofs. CHECK-constraints på lager og leveringsbevis, versionskolonne på orders |

**Netværk:** REST og SignalR mellem browser og C#-serveren. gRPC mellem C#-serveren og Java-serveren. Krav 7 opfyldes af REST og gRPC alene, SignalR er et tillæg.

**Strækmål:** gRPC server-streaming fra Java til C#, så ændringer i order_events flyder live til dashboards uden polling.

### Tre designbeslutninger vi vil forsvare

1. **Statusmaskinen ligger i Java, i samme transaktion som skrivningen.** Afgørelsen "må denne bruger flytte denne ordre" er kun gyldig på det øjebliksbillede der skrives mod. C# henter tilladte overgange via gRPC og tegner knapper ud fra dem. Ingen overgangstabel dubleres i C#.
2. **Optimistisk låsning hele vejen.** Klienten tegnede knapper på en version af ordren. Versionen sendes med til Java. Er ordren ændret i mellemtiden, afvises skrivningen, gRPC svarer ABORTED, og klienten beder brugeren genindlæse. Databasens CHECK-constraints er bagstopper mod oversalg.
3. **Sessioner er en tabel, ikke et JWT.** Når en admin deaktiverer en chauffør, skal chaufføren være logget ud ved næste request. Det kan et signeret token uden opslag ikke levere. Java ejer sessionstabellen, C# ejer cookien, og Java udleder selv hvem der handler ud fra session-id i gRPC-metadata.

## 6. Kravdækning

| Krav | Dækning |
|------|---------|
| 4 Distribueret system | Tre processer, kan køre på hver sin maskine, docker-compose til udvikling |
| 5 Java og C# | Java: domæneserver. C#: klient og app-server |
| 6 Server-til-server | C#-serveren kalder Java-serveren over gRPC |
| 7 To netværksteknologier | REST og gRPC, plus SignalR |
| 8 GUI | Blazor, to visninger (admin på desktop, chauffør på telefon) |
| 9 Database | PostgreSQL |
| 10 Teknologidiskussion | Se afsnit 7 |

## 7. Teknologivalg og alternativer (krav 10)

| Beslutning | Valgt | Alternativer overvejet | Begrundelse |
|------------|-------|------------------------|-------------|
| Klient til server | REST + SignalR | GraphQL, rå WebSocket | Dashboardets forespørgsler er faste, GraphQL løser et problem vi ikke har. SignalR giver push uden egen protokol |
| Server til server | gRPC | REST, RabbitMQ, rå TCP-sockets | Kontrakt-først med .proto, binær serialisering, typede stubs i både C# og Java. Broker fravalgt: alle skrivninger går gennem én C#-proces, så asynkron kø tilføjer drift (Docker, idempotens, dead-letter) uden at løse et behov |
| GUI | Blazor WebAssembly hostet af API'et | Blazor Server, .NET MAUI, Razor Pages | REST-hoppet bliver ægte og synligt. Blazor Server kræver konstant WebSocket, hvilket er skrøbeligt i en varevogn. MAUI kræver iOS-provisioning og en tredje pipeline |
| Database | PostgreSQL | SQLite, Cloudflare D1 | Ægte interaktive transaktioner, CHECK-constraints, moden JPA-dialekt. SQLite serialiserer skrivere på fillås |
| Samtidighed | Optimistisk låsning (version) | Pessimistisk lås (SELECT FOR UPDATE), guarded batches | Pessimistisk lås beskytter kun inde i Java, ikke mod at klienten handlede på et forældet billede |
| Sessioner | Tabel i databasen | JWT | Øjeblikkelig udlogning ved deaktivering |
| Placering af regler | Java, i transaktionen | I C#, dubleret i begge | Én kilde til sandhed, Java-API'et kan ikke misbruges udenom |

Gruppen har adgang til dokumentation for et kørende system i samme domæne, bygget som serverløs monolit på Cloudflare med D1. Dets dokumenterede begrænsninger (ingen interaktive transaktioner, eventual consistency i KV, loft på PBKDF2-iterationer) bruges i rapporten som alternativer med kendte konsekvenser, ikke som hypoteser.

## 8. Metode

- **Scrum** med sprints på to uger. Product backlog, sprint backlog og burndown føres som GitHub Issues og GitHub Projects i det repo, som `github.txt` peger på, så proces og kode ligger samme sted.
- **Kontrakt først.** `.proto`-filen og REST-kontrakten skrives i sprint 1, før implementering.
- **Gående skelet** senest uge 3: login og ordreliste virker end-to-end gennem alle tre processer. Derefter lodrette skiver (én feature gennem alle lag ad gangen).
- **Definition of Done:** enhedstest af statusmaskinen (hele overgangstabellen uden database), integrationstest af gRPC-kontrakten, e2e-test af de tre vigtigste flows.

## 9. Risici

| Risiko | Sandsynlighed | Modtræk |
|--------|---------------|---------|
| Tre kodebaser plus .proto plus Docker, integration først til sidst | Høj | Gående skelet i uge 3, lodrette skiver, docker-compose fra dag ét |
| Regler dubleres i C# og Java og driver fra hinanden | Middel | Overgangstabellen findes kun i Java. C# spørger over gRPC |
| Domænet ender som simpel CRUD | Middel | Statusmaskine, ejerskab, optimistisk låsning og audit er kernen, ikke formularerne |
| Blazor WebAssembly og cookie-auth driller | Middel | Hostet af samme origin som API'et, så HttpOnly-cookie virker uden CORS |
| Scope vokser (notifikationer, kort, RabbitMQ) | Middel | Afsnit 4 er kontrakten. Strækmål først når MVP er i videoen |

## 10. Leverancer

| Leverance | Indhold |
|-----------|---------|
| Rapport | 8 til 12 sider ekskl. bilag. Arkitekturafsnit på højst tre sider med diagrammerne ovenfor. Teknologidiskussion jf. afsnit 7 |
| Kildekode | Hele repoet som .zip |
| github.txt | Link til repoet, som også rummer Scrum-artefakter |
| demo.txt | Video på højst 3 minutter. Viser: login begge roller, opret og tildel ordre, chauffør leverer fra telefon, tidslinje, deaktivering, og samtidighedstesten: to browsere trykker på samme ordre, én afvises, tidslinjen viser præcis én hændelse, logs fra begge servere side om side |
| Formalia | "Formalities Criteria For Upload Of SEP" følges |

## 11. Spørgsmål til vejlederne

1. Godkendes domænet som beskrevet i afsnit 1 til 4?
2. Tæller SignalR som en netværksteknologi i krav 7, eller skal vi regne det som del af REST-laget? Vi opfylder kravet med REST og gRPC uanset.
3. Er Blazor WebAssembly hostet af C#-API'et acceptabelt som "klient" i krav 4, når den kører i browseren som separat proces? Alternativet er Blazor Server i egen proces.
4. Er der krav til hvor mange forskellige databaser eller database-teknologier der forventes, eller er én PostgreSQL tilstrækkelig?
