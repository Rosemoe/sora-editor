# TextMate bracket AST

This implementation follows VS Code's [bracket pair colorization design](https://code.visualstudio.com/blogs/2021/09/29/bracket-pair-colorization), checked against the actual sources under `src/vs/editor/common/model/bracketPairsTextModelPart/bracketPairsTree` and the upstream bracket colorizer tests. The reference checkout supplied for this rewrite reports version 1.138.0. Its `parser.ts` SHA-256 is `183ff30e8ba1e211687cf7e07b52dcd74f0232b70c6c014f757ec1794da6f462`.

## Source correspondence

| VS Code | Sora |
| --- | --- |
| `ast.ts`, `length.ts`, `smallImmutableSet.ts` | `BracketAst.kt`: relative line/column lengths, immutable node types and opener-ID sets |
| `concat23Trees.ts` | `BracketAst.kt`: equal-height runs and persistent 2–3 concatenation |
| `parser.ts`, `nodeReader.ts` | `BracketAst.kt`: recursive descent, ancestor recovery and monotonic subtree reader |
| `beforeEditPositionMapper.ts` | `BracketEdit`: mapping around one processed analyzer edit |
| `tokenizer.ts`, `brackets.ts` | `BracketTokenizer.kt`: token-aware scanning, greedy delimiters, shared closers and word boundaries |
| `bracketPairsTree.ts` range traversal | `BracketQuery.kt`: range-pruned queries and TextMate-only depth/invalid metadata |
| Text/token event integration | `TokenChanges.kt`, `TextMateBracketsProvider.kt`, `TextMateAnalyzer.java` |

The three adapted core files retain VS Code's original Microsoft/MIT header. The MIT license is included in the brackets directory as `LICENSE-vscode.txt`. Sora-specific integration files retain the Rosemoe/LGPL header.

## What makes an update incremental

A node stores its relative length, not an absolute document offset. Moving an unaffected subtree therefore does not rewrite its descendants. Pair bodies and top-level sequences are balanced 2–3 trees; each list has two or three children of equal list height. Pair nesting is separate from list height.

Before peeking at the tokenizer, the parser maps its new position into the old tree and asks the monotonic node reader for the longest reusable node. Reuse requires:

- The node does not intersect or end at the next edit. A touching edit can extend a delimiter, such as `end` to `ending`.
- Its missing-opener set does not intersect the current ancestor opener set.
- It is not an incomplete pair or a list ending in an incomplete pair.
- Moving it into the new context does not exceed the parser's nesting limit.

A cache hit advances the tokenizer by the node's entire length. The tokenizer does not visit that subtree's characters. Concatenation copies only the outer path of the taller list, preserving old roots for concurrent UI queries. Equal-height runs are built in linear time instead of appending every leaf individually.

An ancestor's closer terminates an incomplete child without consuming the closer. For `{(}{}`, the first `{` matches the first `}`, `(` is incomplete, and the final `{}` returns to depth zero. Unopened closers are kept in the AST and summarized by their opener IDs. This is necessary when adding an opener changes a formerly unexpected closer into an ancestor's closing token.

## Two distinct causes of rescanning

**Text chunk misalignment.** Bounded text nodes are necessary to avoid rescanning a large bracket-free region. However, simply producing another fixed-size chunk after inserting a character shifts every later chunk boundary. The new parser then repeatedly misses the old node starts. A regression test initially measured 150,241 character inspections after one insertion into 300,000 plain characters. The tokenizer now stops text at the next mapped old-node boundary, allowing subsequent subtrees to be reused. Delimiters themselves are never split at that boundary.

**Retokenization is not always a token-type change.** Sora retokenizes the edited line even when its code/comment/string classification has not changed. Invalidating the whole line unconditionally defeats reuse on long lines. `TokenChanges` compares normalized code/non-code runs before the analyzer replaces that line's spans. It compares the unaffected prefix and the shifted suffix around a text edit; the inserted region is already invalidated by the text edit. Syntax-color changes alone do not invalidate bracket nodes. A real code/non-code change expands the reparsed range. Explicit token-only notifications remain supported as same-length replacement edits.

These are separate from TextMate grammar tokenization, which can still process a full line. Reducing bracket scans does not remove the cost of grammar tokenization.

## Analyzer ownership and publication

UI insert/delete methods only queue analyzer work. They never scan the analyzer's shadow text.

For each analyzer message:

1. Update the shadow text and report that exact edit before replacing its spans.
2. Retokenize affected lines and compare bracket-relevant token classifications.
3. Parse the union of the text edit and actual token changes, reusing the previous immutable root.
4. Publish the root with the source document version and UTF-16 text length.

Queries return no result while the UI text and the published snapshot disagree. Length is checked as well as version because the deletion and insertion halves of `Content.replace` can share a version. Abandoned analyzer threads cannot publish into a new run. Enabling, disabling, resetting and rerunning do not leave listeners on an active shadow document.

Unlike VS Code's general edit queue, Sora delivers one shadow edit followed by its complete span update. The affected span interval and text edit can therefore be combined into one replacement interval; a multi-edit composer and two independent edit queues are unnecessary. This may conservatively reparse unchanged text between disjoint token-type changes in that message.

VS Code's second, tokenless tree only exists to prevent flicker during initial background tokenization. Sora initializes this provider after initial tokenization completes, corresponding to VS Code's already-tokenized initialization path. It does not maintain two permanent copies of the AST.

## Queries and rendering

Range queries prune off-screen list subtrees and accumulate nesting along the visited path. No shared cursor/range cache is involved. `levelOfEqualBracketType` and `invalid` remain TextMate metadata. Editor's `PairedBracket` does not expose them.

Ordinary bracket pairs remain available for cursor matching even when `colorizedBracketPairs` is explicitly empty. By default, `<`/`>` pairs are excluded from colorization, as in VS Code. Word delimiters use boundaries and cannot cross a non-code token region; adjacent code spans may contain one delimiter. Closers may close more than one opening type.

The existing style patch API draws valid colorized pairs. Incomplete/unopened bracket metadata is retained internally but is not exported as rainbow patches by the generic paired-bracket API. This adapter does not implement VS Code's separate unexpected-bracket color. Style patches include requested sticky lines, are deduplicated, and are sorted before insertion into `SparseStylePatches`. No bracket-specific renderer is added.

Nesting is capped at 150 pairs to bound recursion, matching the effective guard in the reference parser. Work can legitimately grow when changing a comment/string affects the rest of the file or when malformed nesting prevents reuse. No constant-time claim is made for those cases.

## Evidence and reproducibility

The tests include:

- 128 fixtures generated by executing the supplied VS Code parser, including malformed nesting.
- 500 deterministic insert/delete operations comparing incremental and fresh parses, checking old snapshots and 2–3 invariants; includes multiline text and CRLF.
- 150 edits through the real Java TextMate analyzer, checking each result against a fresh parse of the current tokenized shadow.
- Large bracket-dense and bracket-free inputs, including actual analyzer insertions with scan/reuse assertions.
- Queued multiline edits, comment propagation, token-only changes, reruns, enable/disable, version mismatch and compound replacements.
- Style patch ordering, viewport filtering, deduplication and sticky-line queries.

Observed bracket scanner work for one character inserted at the middle of a line:

| Real analyzer input | Resulting length | Characters inspected | Subtrees reused |
| --- | ---: | ---: | ---: |
| Java class with 20,000 `{}` pairs | 60,025 | 6 | 19 |
| Plain identifier text | 100,001 | 257 | 9 |

These are deterministic work counters, not wall-clock benchmarks or Android frame-time measurements. Full device rendering has not been benchmarked.

Run tests from the repository root:

```sh
./gradlew :editor:testDebugUnitTest :language-textmate:testDebugUnitTest
```

Regenerate the independent upstream fixtures using a VS Code source checkout (the generator only imports upstream parsing code; it does not use the Kotlin implementation):

```sh
VSCODE_REFERENCE_ROOT=/path/to/vscode
npx --yes --package=esbuild esbuild language-textmate/src/test/reference/vscode-bracket-oracle.ts --bundle --platform=node --format=esm --alias:vscode-source="$VSCODE_REFERENCE_ROOT/src" --outfile=/tmp/vscode-bracket-oracle.mjs
node /tmp/vscode-bracket-oracle.mjs > language-textmate/src/test/resources/vscode-bracket-oracle.json
```

The production brackets package shrank from 3,826 Kotlin lines in `7141b7d0` (the old AST implementation) to 576 lines in this rewrite, including comments and license headers. The intervening stack-based version in `4c24e9b7` still had 3,416 lines including its unused AST files. Tests, upstream fixtures and this document are excluded from these production-code counts.
