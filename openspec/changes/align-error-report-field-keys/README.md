# align-error-report-field-keys

**Status: in progress - decisions agreed 2026-11-20 (see `proposal.md` > Decisions); implementation done and verified
(unit + integration tests pass).**

Make the `Field` column of the submission error report CSV use one naming style, whichever
validation produced the error. Schema errors (raised in stagingcivil) show CSV template column
names; business errors (raised by prosecutioncasefile) are now translated to the same column names
for complaints-file uploads. Defendant- and offence-column schema errors are now reported as
defendant errors, like prosecutioncasefile's.

Follow-up to CAD-1528 / CAD-1966 (commit `e6f00f0`, branch
`dev/CAD-1966-schema-errors-in-error-report`).
