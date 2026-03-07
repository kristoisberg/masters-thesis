You are a senior software developer with expertise in Java, jOOQ and SQL. Analyze the provided Java class and check for the following database design antipatterns, as defined by Bill Karwin:

- ID Required: Never identify this issue in classes representing views, as views cannot contain primary keys. If the class represents a table, always detect the antipattern, if the name of the primary key is just "id" (case-insensitive). Also detect the issue if a synthetic primary key column exists, even though another unique constraint exists, which is suitable as a primary key (the constraint is on columns, which are virtually immutable by nature), and which does not complicate foreign keys referencing the table too much. Only include the lines of the primary key column definition in the line range, do not include comments or anything else.
- Keyless Entry: A column, which refers to another table, is missing its foreign key. Never identify this issue in classes representing views, as views cannot contain foreign keys. Only report this issue if the "Keys" class provided at the end contains a primary key that this is appropriate for this column to refer to.
- Rounding Errors: Storing fixed-precision values in floating-point type columns, such as FLOAT and REAL, rather than using fixed-precision types like DECIMAL and NUMERIC.
- 31 Flavors: Specifying allowed values in the column definition, i.e. with a CHECK constraint or an ENUM type, rather than using a lookup table. Only include the lines of the column definition in the line range, do not include comments or anything else. Do not report the issue if the CHECK constraint is used to check the value for emptyness or against a range of values (including greater/lesser than comparisons).
- Beware of the Unknown: A special default value, such as an empty string, is used to mark a missing value, rather than NULL, and the special value does not hold a semantic meaning. A column, which can never be NULL in practice (e.g. it has a default value), is marked as NULLABLE.

If the file does not contain any antipatterns, leave the list of occurrences empty.

<analyzed_class>
FILE_CONTENTS
</analyzed_class>

<key_definitions_for_reference>
KEYS_CONTENTS
</key_definitions_for_reference>

<example>
<input>
...
59: /**
60:  * The column <code>world.city.ID</code>.
61:  */
62: public final TableField<CityRecord, Integer> ID = createField("ID", org.jooq.impl.SQLDataType.INTEGER.nullable(false).identity(true), this, "");
...
</input>
<output>
{
    "antipatternName": "ID Required",
    "linesRangeStart": 62,
    "linesRangeEnd": 62,
    "codeFragment": "public final TableField<CityRecord, Integer> ID = createField("ID", org.jooq.impl.SQLDataType.INTEGER.nullable(false).identity(true), this, "");"
    "reasoning": "antipattern - the primary key is called ID"
}
</output>
</example>

<example>
<input>
...
59: /**
60:  * The column <code>person.PERSON_ID</code>.
61:  */
62: public final TableField<PersonRecord, Long> PERSON_ID = createField("person_id", org.jooq.impl.SQLDataType.BIGINT.nullable(false).identity(true), this, "");
63: 
64: /**
65:  * The column <code>person.IDENTIFICATION_CODE</code>.
66:  */
67: public final TableField<PersonRecord, String> IDENTIFICATION_CODE = createField("identification_code", org.jooq.impl.SQLDataType.VARCHAR(11).nullable(false), this, "");
...
@Override
public List<UniqueKey<PersonRecord>> getKeys() {
    return Arrays.<UniqueKey<PersonRecord>>asList(Keys.KEY_PERSON_IDENTIFICATION_CODE, Keys.KEY_PERSON_PRIMARY);
}
...
Keys.java:
...
public static final UniqueKey<FileRecord> KEY_PERSON_IDENTIFICATION_CODE = Internal.createUniqueKey(Person.PERSON, "KEY_person_identification_code", Person.PERSON.IDENTIFICATION_CODE);
public static final UniqueKey<FileRecord> KEY_PERSON_PRIMARY = Internal.createUniqueKey(Person.PERSON, "KEY_person_PRIMARY", Person.PERSON.PERSON_ID);
...
</input>
<output>
{
"antipatternName": "ID Required",
"linesRangeStart": 62,
"linesRangeEnd": 62,
"codeFragment": "public final TableField<PersonRecord, Long> PERSON_ID = createField("person_id", org.jooq.impl.SQLDataType.BIGINT.nullable(false).identity(true), this, "");"
"reasoning": "antipattern - there is another suitable unique key"
}
</output>
</example>

<example>
<input>
...
59: /**
60:  * The column <code>person.PERSON_ID</code>.
61:  */
62: public final TableField<PersonRecord, Long> PERSON_ID = createField("person_id", org.jooq.impl.SQLDataType.BIGINT.nullable(false).identity(true), this, "");
63: 
64: /**
65:  * The column <code>person.NAME</code>.
66:  */
67: public final TableField<PersonRecord, String> NAME = createField("name", org.jooq.impl.SQLDataType.VARCHAR(32).nullable(false), this, "");
...
@Override
public List<UniqueKey<PersonRecord>> getKeys() {
    return Arrays.<UniqueKey<PersonRecord>>asList(Keys.KEY_PERSON_NAME, Keys.KEY_PERSON_PRIMARY);
}
...
Keys.java:
...
public static final UniqueKey<FileRecord> KEY_PERSON_NAME = Internal.createUniqueKey(Person.PERSON, "KEY_person_name", Person.PERSON.NAME);
public static final UniqueKey<FileRecord> KEY_PERSON_PRIMARY = Internal.createUniqueKey(Person.PERSON, "KEY_person_PRIMARY", Person.PERSON.PERSON_ID);
...
</input>
<output>
(nothing - not an antipattern, the alternative key is too volatile)
</output>
</example>

<example>
<input>
...
40: /**
41:  * The column <code>person.PERSON_ID</code>.
42:  */
43: public final TableField<PersonRecord, Long> PERSON_ID = createField("person_id", org.jooq.impl.SQLDataType.BIGINT.nullable(false).identity(true), this, "");
44: 
45: /**
46:  * The column <code>person.FIRST_NAME</code>.
47:  */
48: public final TableField<PersonRecord, String> FIRST_NAME = createField("first_name", org.jooq.impl.SQLDataType.VARCHAR(50).nullable(false), this, "");
49: 
50: /**
51:  * The column <code>person.LAST_NAME</code>.
52:  */
53: public final TableField<PersonRecord, String> LAST_NAME = createField("last_name", org.jooq.impl.SQLDataType.VARCHAR(50).nullable(false), this, "");
54: 
55: /**
56:  * The column <code>person.DATE_OF_BIRTH</code>.
57:  */
58: public final TableField<PersonRecord, LocalDate> DATE_OF_BIRTH = createField("date_of_birth", org.jooq.impl.SQLDataType.LOCALDATE.nullable(false), this, "");
...
@Override
public List<UniqueKey<PersonRecord>> getKeys() {
    return Arrays.<UniqueKey<PersonRecord>>asList(Keys.KEY_PERSON_PRIMARY, Keys.KEY_PERSON_NAME_DOB_UNIQUE);
}
...
Keys.java:
...
public static final UniqueKey<PersonRecord> KEY_PERSON_PRIMARY = Internal.createUniqueKey(Person.PERSON, "KEY_person_PRIMARY", Person.PERSON.PERSON_ID);
public static final UniqueKey<PersonRecord> KEY_PERSON_NAME_DOB_UNIQUE = Internal.createUniqueKey(Person.PERSON, "KEY_person_name_dob_unique", Person.PERSON.FIRST_NAME, Person.PERSON.LAST_NAME, Person.PERSON.DATE_OF_BIRTH);
...
</input>
<output>
(nothing - not an antipattern, the alternative key would complicate foreign keys too much)
</output>
</example>

<example>
<input>
...
61: /**
62:  * The column <code>PUBLIC.DEPARTMENT.ORG_ID</code>.
63:  */
64: public final TableField<DepartmentRecord, Long> ORG_ID = createField(DSL.name("ORG_ID"), SQLDataType.BIGINT.nullable(false), this, "");
...
Keys.java:
...
public static final UniqueKey<OrgRecord> CONSTRAINT_1 = Internal.createUniqueKey(Org.ORG, DSL.name("CONSTRAINT_1"), new TableField[] { Org.ORG.ID }, true);
...
(no DEPARTMENT foreign key referencing CONSTRAINT_1 available)
</input>
<output>
{
"antipatternName": "Keyless Entry",
"linesRangeStart": 64,
"linesRangeEnd": 64,
"codeFragment": "public final TableField<DepartmentRecord, Long> ORG_ID = createField(DSL.name("ORG_ID"), SQLDataType.BIGINT.nullable(false), this, "");"
"reasoning": "antipattern - foreign key missing with suitable target available"
}
</output>
</example>

<example>
<input>
...
81: /**
82:  * The column <code>PUBLIC.DEPARTMENT.CREATED_BY</code>.
83:  */
84: public final TableField<DepartmentRecord, Long> CREATED_BY = createField(DSL.name("CREATED_BY"), SQLDataType.BIGINT, this, "");
...
Keys.java:
...
public static final UniqueKey<UserRecord> UX_USER_ID = Internal.createUniqueKey(User.USER, DSL.name("UX_USER_ID"), new TableField[] { User.USER.ID }, true);
...
</input>
<output>
{
"antipatternName": "Keyless Entry",
"linesRangeStart": 84,
"linesRangeEnd": 84,
"codeFragment": "public final TableField<DepartmentRecord, Long> CREATED_BY = createField(DSL.name("CREATED_BY"), SQLDataType.BIGINT, this, "");"
"reasoning": "antipattern - foreign key missing with suitable target available"
}
</output>
</example>

<example>
<input>
...
50: /**
51:  * The column <code>public.scenario.plan_id</code>.
52:  */
53: public final TableField<ScenarioRecord, Long> PLAN_ID = createField(DSL.name("plan_id"), SQLDataType.BIGINT.nullable(false), this, "");
...
Keys.java:
...
(no foreign key on the PLAN_ID column, but also no primary key for a table related to plans available)
...
</input>
<output>
(nothing - not an antipattern, suitable primary key not available)
</output>
</example>

<example>
<input>
...
79: /**
80:  * The column <code>world.country.SurfaceArea</code>.
81:  */
82: public final TableField<CountryRecord, Double> SURFACEAREA = createField("SurfaceArea", org.jooq.impl.SQLDataType.FLOAT.nullable(false).defaultValue(org.jooq.impl.DSL.inline("0.00", org.jooq.impl.SQLDataType.FLOAT)), this, "");
...
</input>
<output>
{
"antipatternName": "Rounding Errors",
"linesRangeStart": 82,
"linesRangeEnd": 82,
"codeFragment": "public final TableField<CountryRecord, Double> SURFACEAREA = createField("SurfaceArea", org.jooq.impl.SQLDataType.FLOAT.nullable(false).defaultValue(org.jooq.impl.DSL.inline("0.00", org.jooq.impl.SQLDataType.FLOAT)), this, "");"
"reasoning": "antipattern - pretty much every use of an imprecise floating point type is incorrect"
}
</output>
</example>

<example>
<input>
...
121: /**
122:  * The column <code>public.film.rating</code>.
123:  */
124: public final TableField<FilmRecord, MpaaRating> RATING = createField(DSL.name("rating"), SQLDataType.VARCHAR.defaultValue(DSL.field(DSL.raw("'G'::mpaa_rating"), SQLDataType.VARCHAR)).asEnumDataType(MpaaRating.class), this, "");
...
</input>
<output>
{
"antipatternName": "31 Flavors",
"linesRangeStart": 124,
"linesRangeEnd": 124,
"codeFragment": "public final TableField<FilmRecord, MpaaRating> RATING = createField(DSL.name("rating"), SQLDataType.VARCHAR.defaultValue(DSL.field(DSL.raw("'G'::mpaa_rating"), SQLDataType.VARCHAR)).asEnumDataType(MpaaRating.class), this, "");"
"reasoning": "antipattern - use of an ENUM to restrict value options"
}
</output>
</example>

<example>
<input>
...
68: /**
69:  * The column <code>pasto.tipo</code>.
70:  */
71: public final TableField<PastoRecord, String> TIPO = createField(DSL.name("tipo"), SQLDataType.CLOB.nullable(false), this, "");
...
126: @Override
127: public List<Check<PastoRecord>> getChecks() {
128:     return Arrays.asList(
129:         Internal.createCheck(this, DSL.name(""), "tipo in ('colazione', 'pranzo', 'cena', 'spuntino')", true)
130:     );
131: }
...
</input>
<output>
{
"antipatternName": "31 Flavors",
"linesRangeStart": 124,
"linesRangeEnd": 124,
"codeFragment": "public final TableField<PastoRecord, String> TIPO = createField(DSL.name("tipo"), SQLDataType.CLOB.nullable(false), this, "");"
"reasoning": "antipattern - use of an CHECK constraint to restrict value options"
}
</output>
</example>

<example>
<input>
...
72: /**
73:  * The column <code>public.dragon.name</code>.
74:  */
75: public final TableField<DragonRecord, String> NAME = createField(DSL.name("name"), SQLDataType.VARCHAR.nullable(false), this, "");
...
84: /**
85:  * The column <code>public.dragon.age</code>.
86:  */
87: public final TableField<DragonRecord, Integer> AGE = createField(DSL.name("age"), SQLDataType.INTEGER.nullable(false), this, "");
...
215: @Override
216: public List<Check<DragonRecord>> getChecks() {
217:    return Arrays.asList(
218:        Internal.createCheck(this, DSL.name("dragon_age_check"), "((age > 0))", true),
219:        Internal.createCheck(this, DSL.name("dragon_name_check"), "(((name)::text <> ''::text))", true)
220:    );
221: }
...
</input>
<output>
(nothing - neither of the CHECK constraints validate the value against a predetermined set of values (emptiness check doesn't count either))
</output>
</example>

<example>
<input>
...
59: /**
60:  * The column <code>world.country.Code</code>.
61:  */
62: public final TableField<CountryRecord, String> CODE = createField("Code", org.jooq.impl.SQLDataType.CHAR(3).nullable(false).defaultValue(org.jooq.impl.DSL.inline("", org.jooq.impl.SQLDataType.CHAR)), this, "");
...
</input>
<output>
{
"antipatternName": "Beware of the Unknown",
"linesRangeStart": 62,
"linesRangeEnd": 62,
"codeFragment": "public final TableField<CountryRecord, String> CODE = createField("Code", org.jooq.impl.SQLDataType.CHAR(3).nullable(false).defaultValue(org.jooq.impl.DSL.inline("", org.jooq.impl.SQLDataType.CHAR)), this, "");"
"reasoning": "antipattern - use of a special non-business value to represent a missing value, rather than null"
}
</output>
</example>

<example>
<input>
...
77: /**
78:  * The column <code>public.link.updatetime</code>.
79:  */
80: public final TableField<LinkRecord, LocalDateTime> UPDATETIME = createField(DSL.name("updatetime"), SQLDataType.LOCALDATETIME(6).defaultValue(DSL.field("now()", SQLDataType.LOCALDATETIME)), this, "");
...
</input>
<output>
{
"antipatternName": "Beware of the Unknown",
"linesRangeStart": 80,
"linesRangeEnd": 80,
"codeFragment": "public final TableField<LinkRecord, LocalDateTime> UPDATETIME = createField(DSL.name("updatetime"), SQLDataType.LOCALDATETIME(6).defaultValue(DSL.field("now()", SQLDataType.LOCALDATETIME)), this, "");"
"reasoning": "antipattern - a column, which can not be null in practice, is nullable"
}
</output>
</example>

<example>
<input>
...
93: /**
94:  * The column <code>texera_db.workflow_executions.name</code>.
95:  */
96: public final TableField<WorkflowExecutionsRecord, String> NAME = createField(DSL.name("name"), org.jooq.impl.SQLDataType.VARCHAR(128).nullable(false).defaultValue(org.jooq.impl.DSL.inline("Untitled Execution", org.jooq.impl.SQLDataType.VARCHAR)), this, "");
...
</input>
<output>
(nothing - the default value has a semantic meaning)
</output>
</example>
