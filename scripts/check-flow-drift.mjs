// Java port of the same idea as BookMyShowAgent's scripts/check-flow-drift.mjs:
// each .feature file's first line pins a sha256 of the flows/*.md it was written
// from. If the flow changes and the feature doesn't, this fails the build.
//
// Cucumber-JVM has no bddgen equivalent - .feature files are hand-written from the
// flow, not generated - so this check is what keeps them honest.
//
// Usage: node scripts/check-flow-drift.mjs [--write]
import { createHash } from 'node:crypto';
import { readFileSync, readdirSync, writeFileSync } from 'node:fs';
import { join, basename } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = fileURLToPath(new URL('..', import.meta.url));
const flowsDir = join(root, 'flows');
const featuresDir = join(root, 'src/test/resources/features');
const write = process.argv.includes('--write');

const sha256 = (text) => createHash('sha256').update(text).digest('hex');

let drifted = [];

for (const flowFile of readdirSync(flowsDir).filter((f) => f.endsWith('.md'))) {
  const name = basename(flowFile, '.md');
  const featureFile = join(featuresDir, `${name}.feature`);
  const flowSha = sha256(readFileSync(join(flowsDir, flowFile), 'utf8'));

  let featureContent;
  try {
    featureContent = readFileSync(featureFile, 'utf8');
  } catch {
    console.error(`MISSING feature for flow: ${name} (expected ${featureFile})`);
    drifted.push(name);
    continue;
  }

  const match = featureContent.match(/# flow-sha: ([a-f0-9]{64})/);
  if (!match) {
    console.error(`${name}.feature has no "# flow-sha:" header line - cannot verify`);
    drifted.push(name);
    continue;
  }

  if (match[1] !== flowSha) {
    if (write) {
      const updated = featureContent.replace(/# flow-sha: [a-f0-9]{64}.*/, `# flow-sha: ${flowSha}`);
      writeFileSync(featureFile, updated);
      console.log(`Blessed: ${name}.feature -> ${flowSha}`);
    } else {
      console.error(`DRIFT: ${name}.md changed but ${name}.feature was not regenerated`);
      drifted.push(name);
    }
  } else {
    console.log(`OK: ${name}`);
  }
}

if (!write && drifted.length > 0) {
  console.error(`\n${drifted.length} feature file(s) out of date with their flow. Update them, then:`);
  console.error('  node scripts/check-flow-drift.mjs --write');
  process.exit(1);
}
