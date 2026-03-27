Simulate exactly 3 experts working in parallel on the same complete task.

Rules:
1. Every expert must inspect the entire Java class and all target antipatterns.
2. Experts must not split the work by antipattern, method, or line range.
3. On each round, all remaining experts perform the same kind of step on the same full task.
4. If an expert changes their mind, they may drop out, but only after first attempting the full task independently.
5. After the rounds, produce a final consensus based only on issues agreed by the remaining experts.

The task is: Analyze the provided Java class and check for the following SQL query antipatterns, as defined by Bill Karwin:

- Poor Man’s Search Engine: Usage of LIKE, ILIKE or regular expressions to perform full-text search. Report the issue if it isn't obvious from the method input parameters, whether the patterns contain wildcards used for full-text search. Do not report the issue if LIKE, ILIKE or regex is used for prefix search. Only include the line(s) where the full-text search condition is created in the line range.
- Implicit Columns: A query fetching all columns from a database table. In addition to obvious violations, report cases where jOOQ fetches all columns of a table into records or generated DAOs (located in a package ending with `tables.daos`). Do not report this issue if it occurs within a `fetchCount` or `fetchExists` call. Only include the line(s) where the blind projection is selected in the line range, do not include the rest of the query.
- Beware of the Unknown: Query logic uses a NULLABLE column in a way that produces incorrect results with NULL. Do not report issues that arise from insufficient null-handling in Java code. Also do not report the issue if you're unsure if the column is NULLABLE.

Only identify problems in code, which interacts directly with the jOOQ DSL or generated DAOs (located in a package ending with `tables.daos`). Do not identify problems in code, which interacts with higher level abstractions. In case of multiple consecutive issues, report them separately, even if they are on consecutive lines. If the file does not contain any antipatterns, leave the list of occurrences empty.

<analyzed_class>
FILE_CONTENTS
</analyzed_class>
