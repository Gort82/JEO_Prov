# EO Data Provenance Protection via Self‑Recalibrated Watermarking (Java)

GitHub‑ready Java reference implementation aligned to:

**Pérez Gort, M. L., & Cortesi, A. (2026).** *Earth observation data provenance protection through self-recalibrated watermarking.* **GeoInformatica**, 30(1), 6. https://doi.org/10.1007/s10707-026-00566-2

This is a **readable, end‑to‑end prototype** that follows the paper’s *formal flow*:
dimension reduction → carrier selection via VPK/KF/KC → MSB/LSB synchronization/embedding → majority‑vote extraction →
runtime horizontal synchronization (self‑recalibration) via transmitter/receiver strings → C/S/R authenticity digests.

## What this version addresses (vs earlier Java prototype)

- **Fork hash node (Fig. 15)**: C/S/R digests are stored **per fact fork** (not in a generic per‑cell map).
- **facts_filter (Algorithm 3)**: implements the φ‑based selection more literally:
  - ε=0 selects candidates where `KF mod φ == 0` and `fV==1` and sets carrier flags;
  - ε=1 selects *active carriers* (`fC==1` and `cV==1`).
- **C/S/R digests (Algorithm 8)**: computed from:
  - `C`: concatenated ξ‑LSB fragments of nodes with `fV==1` and `fC==1`
  - `S`: concatenated β‑MSB fragments of nodes with `fV==1` and `fS==1`
  - `R`: concatenation of *excluded* nodes (`fV==0`)
  - plus BI/BO binding via an `__CELL__` R slot (as in the paper’s rationale that BI/BO are excluded from C,S).
- **Transmitter/receiver strings (Algorithm 6)**: deterministic Θ‑link generation with collusion avoidance.
- **Horizontal recursion (Algorithms 4–5)**: θ‑bounded recursion and Kθ/K0 handling to preserve meta‑mark positioning across moves.
- **Watermark layers**: +p / +a / +o signals supported (demo embeds +p; +a/+o hooks included).

## About “epochs”
Earlier prototypes used **epoch** as an engineering abstraction for “recalibration stages”.
In the paper’s terms, an epoch corresponds to a **horizontal synchronization (runtime recalibration) stage**:
marks are relocated due to repository evolution (insert/update events, tolerance/priority changes) while preserving operability and detectability.
This implementation drives recalibration through BI/BO transmitter/receiver links and a move limit **θ**.

## Build & run (Java 17, Maven)

```bash
mvn test
mvn -DskipTests package
java -jar target/eowm-java-0.2.0.jar demo --theta 2 --Theta 2
```

## CLI
- `demo` – end‑to‑end run (ingest → embed → map → horizontal sync → extract → similarity)
- `tamper` – apply simple tampering actions to show detection/impact (optional flags)

```bash
java -jar target/eowm-java-0.2.0.jar --help
```

## Project layout
- `eowm.repository.*` — cube, cells, forks (Fig. 15), BI/BO blocks, fact versions & flags
- `eowm.auth.*` — Algorithm 8 (C/S/R) + BI/BO R‑binding validation
- `eowm.wm.*` — watermark construction (+p/+a/+o), dimension reduction, vertical & horizontal sync (Algs 1–7)
- `eowm.integration.*` — synthetic ingest (replace with adapters to real EO repositories)
- `eowm.cli.*` — minimal CLI

## Stability policy: CI tests vs paper-style experiments

This repository separates:

- **Deterministic CI regression tests (`mvn test`)**: verify the implementation runs end-to-end, produces outputs of the correct shape,
  updates C/S/R digests on mutation, and detects BI/BO tampering via the R-binding. These tests are designed to be **stable** and
  not depend on probabilistic watermark recovery rates.
- **Paper-style experiments (manual run)**: similarity / detection performance depends on parameters (N, φ, Θ, θ), repository content,
  and vote coverage. These are provided as a separate runner (see `experiment` command) and are **not** enforced by CI.

This matches common practice for GitHub artifacts and is consistent with the paper: CI checks correctness/invariants, while performance
is evaluated by experiments.

## Run on your computer

Prereqs:
- Java 17+
- Maven 3.9+

Commands:
```bash
mvn test
mvn -DskipTests package

# End-to-end demo (ingest → embed → map → horizontal sync → extract)
java -jar target/eowm-java-0.2.0.jar demo --theta 2 --Theta 2

# Experiment runner (repeatable; prints similarity over multiple trials/seeds)
java -jar target/eowm-java-0.2.0.jar experiment --trials 10 --wbits 64 --theta 2 --Theta 2
```

Tip: if you want more reliable similarity, use **smaller wbits** (e.g., 32/64) and/or increase cell coverage (increase N values).

## Citation

If you use or extend this code, please cite the paper:

**Pérez Gort, M. L., & Cortesi, A. (2026).** *Earth observation data provenance protection through self‑recalibrated watermarking*. **GeoInformatica**, 30(1), 6. https://doi.org/10.1007/s10707-026-00566-2
