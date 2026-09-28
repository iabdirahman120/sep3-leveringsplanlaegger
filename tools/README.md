# tools

`render-diagram.mjs` laver en PDF ud fra en HTML-fil med mermaid-diagrammer.

```
node tools/render-diagram.mjs "$PWD/docs/arkitektur/kilde-1-systemarkitektur.html" \
                              "$PWD/docs/arkitektur/1-systemarkitektur.pdf"
```

Scriptet starter Chrome, venter til mermaid har tegnet diagrammet, og printer
siden til A4. Tilføj `landscape` som tredje argument for liggende format.

Kræver puppeteer-core installeret globalt og Google Chrome i /Applications.
