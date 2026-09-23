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
