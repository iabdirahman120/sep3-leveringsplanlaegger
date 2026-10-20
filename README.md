# Leveringsplanlægger

SEP3-semesterprojekt, VIA Software Engineering, 3. semester.

Leveringskoordinering for en hvidevareforhandler med flere lagre. Disponenter
opretter ordrer og tildeler chauffører, chauffører flytter status fra telefonen
og registrerer hvem der modtog varen, og hver ændring logges med aktør og
tidsstempel.

## Arkitektur

Tre processer og én database. Alle skrivninger går gennem samme vej.

```
Blazor WebAssembly (C#)
        |  REST + SignalR
ASP.NET Core app-server (C#)
        |  gRPC
Spring Boot domæneserver (Java)
        |  JPA
PostgreSQL
```

Java ejer databasen inklusive brugere og sessioner, og håndhæver statusmaskine,
ejerskab og lagerregler i samme transaktion som skrivningen. C# ejer
session-cookien, rate limit, REST-kontrakten og dashboard-tallene.

Begrundelserne står i `docs/projektforslag-vejledere.md` afsnit 5 og 7.

## Sådan starter du det (proof of concept)

Proof of concept er et gående skelet: én tynd funktion, der går gennem alle
tre processer. Klienten viser leveringerne, henter de tilladte overgange fra
Java og skifter status på én ordre.

### Forudsætninger

| Værktøj | Version |
|---|---|
| Java (JDK) | 21 eller nyere |
| Maven | 3.9 |
| .NET SDK | 10 |
| PostgreSQL | 16 eller nyere, via Docker eller lokalt |

### 1. Database

Med Docker:

```bash
docker compose -f docker/docker-compose.yml up -d
```

Uden Docker, mod en lokal PostgreSQL på port 5432:

```bash
psql -d postgres -c "CREATE ROLE delivery LOGIN PASSWORD 'delivery'"
createdb -O delivery delivery
```

Tabellerne oprettes af Hibernate, første gang domæneserveren starter.
Demodata er opdigtede og lægges ind, hvis tabellen er tom.

### 2. Domæneserver (Java, gRPC på port 9090)

```bash
cd domain
mvn spring-boot:run
```

Klar når loggen skriver `gRPC-server lytter på port 9090`.

### 3. App-server og klient (C#, http://localhost:5049)

```bash
cd bff
dotnet run
```

App-serveren hoster Blazor-klienten. Åbn http://localhost:5049 i browseren.

### Porte

| Proces | Port | Protokol |
|---|---|---|
| Blazor-klient og app-server | 5049 | HTTP, REST |
| Domæneserver | 9090 | gRPC |
| PostgreSQL | 5432 | |

### REST-kontrakt

| Metode | Sti | Svar |
|---|---|---|
| GET | `/api/orders` | Alle ordrer med status og version |
| GET | `/api/orders/{id}/transitions` | Tilladte overgange, spurgt hos Java |
| POST | `/api/orders/{id}/transition` | Body `{ "toStatus": "ASSIGNED", "expectedVersion": 0 }` |

Fejl fra Java oversættes til HTTP: ulovlig overgang giver 422, forældet
version giver 409, ukendt ordre giver 404.

### Test

```bash
cd domain
mvn test
```

Enhedstesten gennemgår hele overgangstabellen uden database.

### Det mangler efter proof of concept

Login og sessioner, roller (disponent og chauffør), opret ordre, tildel
chauffør, leveringsbevis, dashboard og docker-compose for alle tre processer.

## Mapper

| Mappe | Sprog | Indhold |
|---|---|---|
| `client/` | C# | Blazor WebAssembly, admin-shell og driver-shell |
| `bff/` | C# | ASP.NET Core app-server, REST og SignalR |
| `domain/` | Java | Spring Boot domæneserver, gRPC og JPA |
| `proto/` | | Delte gRPC-kontrakter. Skrives før implementering |
| `docker/` | | docker-compose med PostgreSQL |
| `docs/` | | Krav, projektbeskrivelse og projektforslag |


## Kom i gang

```
docker compose -f docker/docker-compose.yml up -d
```

## Regler i projektet

- Både Java og C# skal bruges. Det er krav 5 og kan ikke laves om.
- Statusmaskine, ejerskabstjek og lagerregler ligger i Java. De dubleres ikke i C#.
- Ingen kode genbruges fra `reference/`. Kun idé, datamodel og forretningsregler.
- Kontrakt først: `.proto` og REST-kontrakten skrives før implementering.
- Fravalgt og genoptages ikke: RabbitMQ, GraphQL, .NET MAUI, SMS og e-mail,
  ruteoptimering, ERP-integration. Begrundelser i
  `docs/second-opinion-architecture.md` afsnit 3, 4 og 8.

## Proces

Scrum med sprints på to uger. Backlog, sprint backlog og burndown føres som
GitHub Issues og GitHub Projects i dette repo, så proces og kode ligger samme sted.

## Leverancer

| Fil | Indhold |
|---|---|
| `github.txt` | Link til dette repo (krav 14) |
| `demo.txt` | Link til demovideo, højst 3 minutter (krav 15) |

Alle 15 krav står i `docs/sep3-krav.md`.

## Referencemateriale

Dokumentationen for det eksisterende system i samme domæne er ZNG's materiale
og ligger derfor **ikke** i dette repo. Den findes kun lokalt i
`~/Downloads/sep3-reference-lokal/jlr-architecture`.

Kun idé, datamodel og forretningsregler genbruges. Ingen kode.
