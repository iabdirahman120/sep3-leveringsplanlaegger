# SEP3: Leveringsplanlægger i Java + C#

Denne mappe rummer planlægningsmaterialet til et SEP3-semesterprojekt (VIA, Software Engineering, 3. semester). Der er endnu ingen kode, kun krav, projektforslag, en arkitektur-second-opinion og referencedokumenter fra et eksisterende system i samme domæne.

## Læs først

1. `docs/sep3-krav.md`: de 15 krav fra VIA. De vinder over alt andet.
2. `docs/projektforslag-vejledere.md`: det aktuelle forslag (arkitektur, afgrænsning, teknologivalg).
3. `docs/second-opinion-architecture.md`: hvorfor forslaget ser ud som det gør.
4. `reference/jlr-architecture/`: dokumentation for det eksisterende Next.js-system, som domænet er lånt fra.

## Hårde regler

- Systemet skal bruge **både Java og C#**. Java: domæneserver (Spring Boot, gRPC, JPA). C#: Blazor-klient og ASP.NET Core app-server.
- **Genbrug ingen kode** fra `reference/`. Kun idé, datamodel og forretningsregler. `order-status.ts.txt` er læsestof.
- Statusmaskine, ejerskabstjek og lagerregler ligger i **Java**, i samme transaktion som skrivningen. Dubler dem ikke i C#.
- Fravalgt og skal ikke foreslås igen: RabbitMQ, GraphQL, .NET MAUI, SMS/e-mail-notifikationer, ruteoptimering, ERP-integration. Begrundelser står i second opinion afsnit 3, 4 og 8.
- Påstande om det eksisterende system skal henvise til fil og afsnit i `reference/jlr-architecture/`. Kan noget ikke findes der, skriv UVERIFICERET.

## Sprog og form

- Svar på dansk, medmindre brugeren skriver engelsk.
- Ingen tankestreger og ingen dobbelt-bindestreg i tekst der skal afleveres. Brug komma eller kolon.
- Krav-numre (1 til 15) refererer til rækkerne i `docs/sep3-krav.md`.

## Når der kommer kode

Foreslået repo-layout, ikke oprettet endnu:

```
client/     C#, Blazor WebAssembly
bff/        C#, ASP.NET Core (REST, SignalR, cookie, gRPC-klient)
domain/     Java, Spring Boot (gRPC-server, JPA, PostgreSQL)
proto/      delte .proto-kontrakter
docker/     docker-compose med PostgreSQL
```

Kontrakt først: `.proto` og REST-kontrakt skrives før implementering. Gående skelet (login + ordreliste gennem alle tre processer) før features.
