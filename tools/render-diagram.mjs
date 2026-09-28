import puppeteer from "/opt/homebrew/lib/node_modules/puppeteer-core/lib/puppeteer/puppeteer-core.js";

const [htmlPath, pdfPath, orientation] = process.argv.slice(2);

const browser = await puppeteer.launch({
  executablePath: "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome",
  headless: false,
  args: ["--no-sandbox"],
});

const page = await browser.newPage();
await page.goto("file://" + htmlPath, { waitUntil: "networkidle0" });

await page.waitForFunction("window.mermaidDone === true", { timeout: 40000 });
await page.waitForFunction(
  "document.querySelectorAll('.mermaid svg').length >= 1", { timeout: 40000 });

await page.pdf({
  path: pdfPath,
  format: "A4",
  landscape: orientation === "landscape",
  printBackground: true,
  margin: { top: "15mm", bottom: "15mm", left: "14mm", right: "14mm" },
});

await browser.close();
console.log("PDF skrevet: " + pdfPath);
