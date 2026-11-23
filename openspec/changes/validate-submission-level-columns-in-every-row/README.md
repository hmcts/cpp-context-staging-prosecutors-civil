# validate-submission-level-columns-in-every-row

Validate `prosecutingAuthority` and `hearingDetails.*` on every row of an uploaded complaints CSV file - each row's values must be present, well-formed and the same as row 1's - while still sending only row 1's values forward once the file passes validation.
