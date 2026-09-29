import { describe, expect, it } from "vitest";
import { existsSync, readdirSync, readFileSync, statSync } from "node:fs";
import path from "node:path";

const CONTENT_DIR = path.resolve(__dirname, "..", "content", "docs");

function collectFiles(dir: string, predicate: (name: string) => boolean): string[] {
  let files: string[] = [];
  for (const entry of readdirSync(dir)) {
    const full = path.join(dir, entry);
    if (statSync(full).isDirectory()) {
      files = files.concat(collectFiles(full, predicate));
    } else if (predicate(entry)) {
      files.push(full);
    }
  }
  return files;
}

/** Maps a content file's path to the route Fumadocs serves it at. */
function routeForMdxFile(file: string): string {
  const relative = path.relative(CONTENT_DIR, file).replace(/\\/g, "/").replace(/\.mdx$/, "");
  if (relative === "index") return "/docs";
  if (relative.endsWith("/index")) return "/docs/" + relative.slice(0, -"/index".length);
  return "/docs/" + relative;
}

const mdxFiles = collectFiles(CONTENT_DIR, (name) => name.endsWith(".mdx"));
const metaFiles = collectFiles(CONTENT_DIR, (name) => name === "meta.json");
const validRoutes = new Set(mdxFiles.map(routeForMdxFile));

describe("docs content integrity", () => {
  it("finds the expected top-level pages", () => {
    expect(validRoutes.has("/docs")).toBe(true);
    expect(validRoutes.has("/docs/installation")).toBe(true);
    expect(validRoutes.has("/docs/quickstart")).toBe(true);
    expect(validRoutes.has("/docs/api-reference")).toBe(true);
    expect(validRoutes.has("/docs/webhooks")).toBe(true);
  });

  it("every internal /docs link in mdx content resolves to a real page", () => {
    // Covers both markdown links ([text](/docs/x)) and JSX attributes (href="/docs/x"),
    // since content here uses both (plain prose links and <Card href="..."> components).
    const linkPatterns = [/]\((\/docs[^\s)]*)\)/g, /href="(\/docs[^"]*)"/g];
    const broken: string[] = [];

    for (const file of mdxFiles) {
      const content = readFileSync(file, "utf8");
      for (const pattern of linkPatterns) {
        for (const match of content.matchAll(pattern)) {
          const target = match[1].split("#")[0].replace(/\/$/, "");
          if (!validRoutes.has(target)) {
            broken.push(`${path.relative(CONTENT_DIR, file)} -> ${match[1]}`);
          }
        }
      }
    }

    expect(broken).toEqual([]);
  });

  it("every meta.json 'pages' entry points at a file or folder that actually exists", () => {
    const missing: string[] = [];

    for (const metaFile of metaFiles) {
      const dir = path.dirname(metaFile);
      const meta = JSON.parse(readFileSync(metaFile, "utf8")) as { pages?: string[] };
      for (const page of meta.pages ?? []) {
        const asFile = path.join(dir, `${page}.mdx`);
        const asFolder = path.join(dir, page);
        if (!existsSync(asFile) && !existsSync(asFolder)) {
          missing.push(`${path.relative(CONTENT_DIR, metaFile)} -> "${page}"`);
        }
      }
    }

    expect(missing).toEqual([]);
  });
});
