# align-error-report-field-keys

**Status: on hold - waiting for a decision (see `proposal.md` > Decisions needed).**

Make the `Field` column of the submission error report CSV use one naming style, whichever
validation produced the error. Today schema errors (raised in stagingcivil) show CSV template
column names, while business errors (raised by prosecutioncasefile) show prosecutioncasefile's
internal field names, which do not appear anywhere in the user's uploaded file.

Follow-up to CAD-1528 / CAD-1966 (commit `e6f00f0`, branch
`dev/CAD-1966-schema-errors-in-error-report`).
