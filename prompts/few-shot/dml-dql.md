You are a senior software developer with expertise in Java, jOOQ and SQL. Analyze the provided Java class and check for the following SQL query antipatterns, as defined by Bill Karwin:

- Poor Man’s Search Engine: Usage of LIKE, ILIKE or regular expressions to perform full-text search. Report the issue if it isn't obvious from the method input parameters, whether the patterns contain wildcards used for full-text search. Do not report the issue if LIKE, ILIKE or regex is used for prefix search. Only include the line(s) where the full-text search condition is created in the line range.
- Implicit Columns: A query fetching all columns from a database table. In addition to obvious violations, report cases where jOOQ fetches all columns of a table into records or generated DAOs (located in a package ending with `tables.daos`). Do not report this issue if it occurs within a `fetchCount` or `fetchExists` call. Only include the line(s) where the blind projection is selected in the line range, do not include the rest of the query.
- Beware of the Unknown: Query logic uses a NULLABLE column in a way that produces incorrect results with NULL. Do not report issues that arise from insufficient null-handling in Java code. Also do not report the issue if you're unsure if the column is NULLABLE.

Only identify problems in code, which interacts directly with the jOOQ DSL or generated DAOs (located in a package ending with `tables.daos`). Do not identify problems in code, which interacts with higher level abstractions. In case of multiple consecutive issues, report them separately, even if they are on consecutive lines. If the file does not contain any antipatterns, leave the list of occurrences empty.

<analyzed_class>
FILE_CONTENTS
</analyzed_class>

<example>
<input>
...
20: @Transactional(readOnly = true)
21: public List<String> getTagSuggestionsForImage(String category, String tag, UUID imageId) {
22:    UUID repoId = getRepoId(imageId);
23:
24:    return
25:            dsl.select(Tables.IMAGE_TAGS.TAG)
26:                    .from(Tables.IMAGE_TAGS)
27:                    .where(Tables.IMAGE_TAGS.REPOSITORY_ID.eq(repoId))
28:                    .and(Tables.IMAGE_TAGS.TAG.likeIgnoreCase("%"+tag+"%"))
29:                    .and(Tables.IMAGE_TAGS.TAG_CATEGORY.likeIgnoreCase("%"+category+"%"))
30:                    .and(Tables.IMAGE_TAGS.IMAGE_ID.notEqual(imageId))
31:                    .limit(5)
32:                    .fetch(Tables.IMAGE_TAGS.TAG);
33: }
...
</input>
<output>
{
"antipatternName": "Poor Man’s Search Engine",
"linesRangeStart": 28,
"linesRangeEnd": 28,
"codeFragment": ".and(Tables.IMAGE_TAGS.TAG.likeIgnoreCase("%"+tag+"%"))",
"reasoning": "antipattern - use of ILIKE for full-text search"
}, {
"antipatternName": "Poor Man’s Search Engine",
"linesRangeStart": 29,
"linesRangeEnd": 29,
"codeFragment": ".and(Tables.IMAGE_TAGS.TAG_CATEGORY.likeIgnoreCase("%"+category+"%"))",
"reasoning": "antipattern - use of ILIKE for full-text search"
}
</output>
</example>

<example>
<input>
(tag and category are passed in from outside the class)
...
20: @Transactional(readOnly = true)
21: public List<String> getTagSuggestionsForImage(String category, String tag, UUID imageId) {
22:    UUID repoId = getRepoId(imageId);
23:
24:    return
25:            dsl.select(Tables.IMAGE_TAGS.TAG)
26:                    .from(Tables.IMAGE_TAGS)
27:                    .where(Tables.IMAGE_TAGS.REPOSITORY_ID.eq(repoId))
28:                    .and(Tables.IMAGE_TAGS.TAG.likeIgnoreCase(tag))
29:                    .and(Tables.IMAGE_TAGS.TAG_CATEGORY.likeIgnoreCase(category))
30:                    .and(Tables.IMAGE_TAGS.IMAGE_ID.notEqual(imageId))
31:                    .limit(5)
32:                    .fetch(Tables.IMAGE_TAGS.TAG);
33: }
...
</input>
<output>
{
"antipatternName": "Poor Man’s Search Engine",
"linesRangeStart": 28,
"linesRangeEnd": 28,
"codeFragment": ".and(Tables.IMAGE_TAGS.TAG.likeIgnoreCase("%"+tag+"%"))",
"reasoning": "antipattern - use of ILIKE, not able to figure out from the class if it's used for full-text search"
}, {
"antipatternName": "Poor Man’s Search Engine",
"linesRangeStart": 29,
"linesRangeEnd": 29,
"codeFragment": ".and(Tables.IMAGE_TAGS.TAG_CATEGORY.likeIgnoreCase("%"+category+"%"))",
"reasoning": "antipattern - use of ILIKE, not able to figure out from the class if it's used for full-text search"
}
</output>
</example>

<example>
<input>
62: @Override
63: public List<Link> findByIdIn(List<Long> ids) {
64:     return context
65:         .select().from(LINK).where(LINK.ID.in(ids)).fetchInto(Link.class);
66: }
</input>
<output>
{
"antipatternName": "Implicit Columns",
"linesRangeStart": 65,
"linesRangeEnd": 65,
"codeFragment": ".select().from(LINK).where(LINK.ID.in(ids)).fetchInto(Link.class);",
"reasoning": "antipattern - select().from(LINK) fetches all columns from the table"
}
</output>
</example>

<example>
<input>
65: private Void preValidateParams(ReplicationParam.Duplicate duplicate) {
66:     boolean exists = dslContext.fetchExists(dslContext.selectFrom(IMAGE).where(IMAGE.ID.eq(duplicate.imageId())));
67: 
68:     if (!exists) {
69:         throw new ImageReplicationUserException("Image with ID " + duplicate.imageId() + " does not exist");
70:     }
71: 
72:     return null;
73: }
</input>
<output>
(nothing - the selectFrom, which by itself would fetch a record with all columns, is wrapped in a fetchExists)
</output>
</example>

<example>
<input>
...
import com.example.onboardingservice.jooq.tables.daos.OnboardingDocumentsDao;
...
private final OnboardingDocumentsDao documentsDao;
...
47: @Override
48: public List<DocumentDto> getDocuments() {
49:     return documentsDao.findAll().stream()
50:             .map(it -> DocumentDto.of(it.getKey(), it.getFilename()))
51:             .toList();
52: }
...
</input>
<output>
{
"antipatternName": "Implicit Columns",
"linesRangeStart": 49,
"linesRangeEnd": 49,
"codeFragment": "documentsDao.findAll()",
"reasoning": "antipattern - generated DAOs fetch all columns from the table"
}
</output>
</example>

<example>
<input>
...
import com.example.onboardingservice.business.OnboardingDocumentsDao;
...
private final OnboardingDocumentsDao documentsDao;
...
47: @Override
48: public List<DocumentDto> getDocuments() {
49:     return documentsDao.findAll().stream()
50:             .map(it -> DocumentDto.of(it.getKey(), it.getFilename()))
51:             .toList();
52: }
...
</input>
<output>
(nothing - the DAO is not generated and is analysed separately)
</output>
</example>

<example>
<input>
...
34: public Author afficherAuteur(Integer id){
35:      return dslContext.selectFrom(AUTHOR).where(AUTHOR.ID.eq(id)).fetchSingleInto(Author.class);
36: }
...
50: public void modifierAuteur(AuthorVo author){
51:     Author _author = this.afficherAuteur(author.getId().intValue());
52:
53:     dslContext.update(AUTHOR)
54:             .set(AUTHOR.FIRST_NAME,author.getFirstName())
55:             .set(AUTHOR.LAST_NAME, author.getLastName())
56:             .where(AUTHOR.ID.eq(author.getId().intValue())).execute();
57: }
...
</input>
<output>
(nothing - while there is a potential nullability issue here, it's caused by the Java code)
</output>
</example>

<example>
<input>
...
72: private Condition getIdCondition(UUID id) {
73:     if (id == null) {
74:         return TASK.TASK_TYPE_ID.eq(TASK.TASK_TYPE_ID); // 1==1
75:     } else {
76:         return TASK.TASK_TYPE_ID.eq(id);
77:     }
78: }
...
</input>
<output>
(nothing - assume that TASK_TYPE_ID is non-nullable)
</output>
</example>
