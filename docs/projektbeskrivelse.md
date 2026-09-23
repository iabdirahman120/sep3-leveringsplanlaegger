# Projektbeskrivelse, SEP3

**Titel:** Leveringsplanlægger, leveringskoordinering for en forhandler med flere lagre
**Uddannelse:** VIA University College, Software Engineering, 3. semester
**Gruppe:** _[navne og studienumre udfyldes]_
**Vejledere:** _[udfyldes]_
**Dato:** 2026-09-23
**Kildekode:** https://github.com/iabdirahman120/sep3-leveringsplanlaegger

Dette dokument beder om godkendelse af domænet, jævnfør krav 2. Den fulde
arkitekturbegrundelse ligger i `projektforslag-vejledere.md`, og kritikken der
formede den i `second-opinion-architecture.md`.

---

## 1. Problemet

En dansk forhandler af hvidevarer leverer fra flere lagre med egne chauffører.
Koordineringen sker i dag i delte To Do-lister: én afkrydsning pr. levering.

Det giver tre konkrete problemer:

- **Ingen historik.** Når en levering mislykkes, fordi ingen er hjemme eller
  adgangen nægtes, oprettes opgaven bare igen, og forsøget forsvinder.
- **Intet ejerskab.** Ingen kan bagefter svare på, hvem der flyttede en ordre
  og hvornår, eller hvem der modtog varen.
- **Intet overblik.** Disponenten kan ikke se belastning pr. chauffør eller
  pr. lager, og opdager først en forsinkelse, når kunden ringer.

Domænet er hentet fra et reelt behov, som et gruppemedlem har arbejdet med.
Systemet bygges fra bunden i Java og C#. Vi genbruger problemforståelsen,
datamodellen og forretningsreglerne, men ingen kode.

## 2. Aktører

| Aktør | Rolle | Behov |
|---|---|---|
| Disponent | Kontorpersonale. Opretter ordrer, tildeler chauffører, overvåger alle lagre | Dashboard til desktop |
| Chauffør | Ansat, knyttet til ét lager. Ser kun egne leveringer | Telefonvenlig visning, få tryk |
| Kunde | Modtager varen. Ikke bruger af systemet | Modelleres, fordi kundetypen styrer leveringsvindue og kontaktfelter |

## 3. Det systemet skal kunne

Følgende skal virke hele vejen igennem og vises i demovideoen:

1. Login med to roller og hver sin visning
2. Opret ordre med kunde, lager, varelinjer og leveringsvindue. Lageret tjekkes,
   og ordren afvises, hvis der ikke er varer nok
3. Tildel chauffør. Kun chauffører fra ordrens eget lager kan vælges
4. Chaufføren flytter ordren gennem statusforløbet fra telefonen og registrerer
   modtagerens navn
5. Tidslinje pr. ordre. Hver ændring logges med aktør og tidsstempel, og loggen
   kan ikke redigeres
6. Dashboard: forsinkede leveringer, utildelte ordrer, belastning pr. chauffør
7. Deaktivér chauffør. Adgangen lukkes, sessioner slettes, og tildelte ordrer
   frigives

### Statusforløbet

Syv tilstande med én overgangstabel, der findes ét sted og håndhæves på serveren.
Brugerfladen viser kun de knapper, brugeren må trykke på, men serveren tjekker
igen, før der skrives.

```
pending -> assigned -> picked_up -> in_transit -> delivered
                            |            |
                            +---> failed <+
                                    |
                                    +---> assigned (planlægges igen)

pending, assigned og failed kan desuden gå til cancelled.
delivered og cancelled er slutstationer.
```

`failed` er med vilje ikke en sluttilstand. En mislykket levering er normal i
hvidevarelogistik og skal kunne planlægges igen, uden at ordrens historik
går tabt.

## 4. Afgrænsning

Uden for scope, navngivet så grænsen er tydelig:

- Kundenotifikationer via SMS eller e-mail, og offentlig sporingsside
- Ruteoptimering og kort. Rækkefølgen på chaufførens dag er manuel
- Integration til ERP eller webshop. Ordrer oprettes i systemet
- Returlogistik. En mislykket levering registreres, men varens vej tilbage
  til lageret følges ikke
- Nulstilling af adgangskode via e-mail, mørkt tema og lignende bekvemmeligheder

## 5. Arkitektur

Tre processer og én database. Alle skrivninger går gennem samme vej.

```
Klient           Blazor WebAssembly (C#), admin-shell og driver-shell
   |             REST + SignalR
App-server       ASP.NET Core (C#), cookie og HMAC, rate limit, KPI'er
   |             gRPC
Domæneserver     Spring Boot (Java), statusmaskine, ejerskab, lagerregler, audit
   |             JPA
Database         PostgreSQL i Docker
```

Java ejer databasen, inklusive brugere og sessioner. C# ejer session-cookien,
REST-kontrakten, validering af input og dashboard-tallene.

### Tre beslutninger vi vil forsvare

**Statusmaskinen ligger i Java, i samme transaktion som skrivningen.**
Afgørelsen "må denne bruger flytte denne ordre" gælder kun det øjebliksbillede,
der skrives mod. C# henter de tilladte overgange over gRPC og tegner knapper ud
fra svaret. Overgangstabellen dubleres ikke.

**Optimistisk låsning hele vejen.** Klienten tegnede knapper på en bestemt
version af ordren. Versionen følger med tilbage til Java. Er ordren ændret
undervejs, afvises skrivningen, og brugeren bliver bedt om at genindlæse.
Databasens CHECK-constraints er bagstopper mod oversalg.

**Sessioner er en tabel, ikke et JWT.** Når en disponent deaktiverer en
chauffør, skal chaufføren være logget ud ved næste kald. Det kan et signeret
token uden databaseopslag ikke levere.

## 6. Teknologivalg og alternativer

| Beslutning | Valgt | Fravalgt | Hvorfor |
|---|---|---|---|
| Klient til server | REST og SignalR | GraphQL, rå WebSocket | Forespørgslerne er faste. GraphQL løser et problem, vi ikke har |
| Server til server | gRPC | REST, RabbitMQ, sockets | Kontrakt først med .proto, typede stubs i begge sprog. En broker tilføjer drift uden at løse et behov |
| Brugerflade | Blazor WebAssembly hostet af API'et | Blazor Server, .NET MAUI | REST-hoppet bliver ægte. Blazor Server kræver konstant WebSocket, hvilket er skrøbeligt i en varevogn |
| Database | PostgreSQL | SQLite | Ægte interaktive transaktioner og moden JPA-dialekt |
| Samtidighed | Optimistisk låsning | Pessimistisk lås | Beskytter også mod at klienten handlede på et forældet billede |
| Regler | I Java, i transaktionen | Dubleret i C# | Én kilde til sandhed |

Gruppen har dokumentation for et kørende system i samme domæne, bygget
serverløst på Cloudflare. Dets kendte begrænsninger, blandt andet manglende
interaktive transaktioner, bruges i rapporten som alternativer med målte
konsekvenser i stedet for hypoteser.

## 7. Metode og plan

**Scrum** med sprints på to uger. Product backlog, sprint backlog og burndown
føres som GitHub Issues og GitHub Projects i det repo, `github.txt` peger på,
så proces og kode ligger samme sted.

| Sprint | Mål |
|---|---|
| 1 | `.proto` og REST-kontrakt skrevet. docker-compose kører. Skema oprettet |
| 2 | Gående skelet: login og ordreliste virker end-to-end gennem alle tre processer |
| 3 til 5 | Lodrette skiver: én funktion gennem alle lag ad gangen |
| 6 | Rapport, demovideo og aflevering |

**Definition of Done:** enhedstest af hele overgangstabellen uden database,
integrationstest af gRPC-kontrakten, og e2e-test af de tre vigtigste flows.

## 8. Kravdækning

| Krav | Hvordan |
|---|---|
| 1 Metode | Scrum, artefakter i GitHub Projects |
| 2 Domæne | Dette dokument |
| 3 Versionskontrol | Git fra første commit, se link øverst |
| 4 Distribueret | Tre processer, hver sin maskine mulig |
| 5 Java og C# | Java: domæneserver. C#: klient og app-server |
| 6 Server til server | C#-serveren kalder Java over gRPC |
| 7 To netværksteknologier | REST og gRPC, plus SignalR |
| 8 GUI | Blazor, to visninger |
| 9 Database | PostgreSQL |
| 10 Teknologidiskussion | Afsnit 6 |
| 11 til 15 | Se afsnit 9 |

## 9. Leverancer

| Leverance | Indhold |
|---|---|
| Rapport | 8 til 12 sider ekskl. bilag. Arkitektur på højst tre sider |
| Kildekode | Hele repoet som .zip |
| `github.txt` | Link til repoet, som også rummer Scrum-artefakterne |
| `demo.txt` | Link til video på højst tre minutter |

Videoen viser: login i begge roller, opret og tildel ordre, chauffør leverer fra
telefonen, tidslinje, deaktivering, og til sidst samtidighedstesten, hvor to
browsere trykker på samme ordre, den ene afvises, tidslinjen viser præcis én
hændelse, og logs fra begge servere står side om side.

## 10. Risici

| Risiko | Modtræk |
|---|---|
| Tre kodebaser integreres først til sidst | Gående skelet i sprint 2, derefter lodrette skiver |
| Regler driver fra hinanden i C# og Java | Overgangstabellen findes kun i Java |
| Domænet ender som simpel CRUD | Statusmaskine, ejerskab, låsning og audit er kernen |
| Scope vokser | Afsnit 4 er kontrakten. Strækmål først når alt i afsnit 3 er i videoen |

## 11. Spørgsmål til vejlederne

1. Godkendes domænet som beskrevet i afsnit 1 til 4?
2. Tæller SignalR som en netværksteknologi i krav 7? Kravet er opfyldt af
   REST og gRPC uanset svaret.
3. Er Blazor WebAssembly hostet af C#-API'et acceptabelt som klient i krav 4,
   når den kører i browseren som separat proces?
4. Er én PostgreSQL tilstrækkelig til krav 9?
