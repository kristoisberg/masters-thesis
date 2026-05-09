---
marp: true
theme: default
paginate: true
---

# Prevalence and Detection of SQL Antipatterns in Java Projects Using jOOQ for Database Access

**Kristo Isberg**
Supervisor: Erki Eessaar

Tallinn University of Technology
27.10.2025

<!--
Hi, I'm Kristo and my Master's thesis topic is Prevalence and Detection of SQL Antipatterns in Java Projects Using jOOQ for Database Access. My supervisor is Erki Eessaar, who is also with us today online.

My plan is to first go over what some of these terms in the title mean, what my topic is about, why it is relevant, and how it translates into my research goal. I have to say that I have a rought research process planned out, which I covered in my problem statement, but it has already changed a bit since then, so it is quite volatile, so I decided not to cover that part today.
-->

---

# What are (SQL) Antipatterns?

- "... a commonly occurring solution to a problem that generates decidedly negative consequences" (Brown et al., 1998)
- "... the most frequent missteps software developers naively make while using SQL" (Karwin, 2022)

<!--
SQL antipatterns are a class of issues in SQL database access code. They were first introduced by Bill Karwin in a similarly named book - this book right here.

They describe common solutions to common problems in databases, for example, related to database schema design or querying data. These solutions may solve the original problem in the short term, but in the long term the will cause more harm than good, so those solutions turn into problems themselves.

People might find those solutions from books, blog posts, Stackoverflow, ChatGPT, or they might come up with them themselves. Like the saying goes, great minds think alike. Same way, they sometimes come up with the same poor solutions.
-->

---

# Negative consequences? What kind?

- Performance
- Maintainability
- Portability
- Data integrity

<!--
They can have a negative impact on the database or the application in many ways. For example, the performance of the application. The maintainability of it, which makes maintenance more time-consuming and costly. The portability between different system architectures and database engines. And the integrity of the data.
-->

---

<style scoped>
section {
  align-content: start;
}
</style>

# An example: _Implicit Columns_

| id         | name                                             | curr                               | email             | birth_date                                     | uniid  |
| ---------- | ------------------------------------------------ | ---------------------------------- | ----------------- | ---------------------------------------------- | ------ |
| 5434&nbsp; | Kristo&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; | IVSM&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; | krisbe@taltech.ee | 1998-07-18&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; | krisbe |

```sql
SELECT * FROM student WHERE id = 5434
```

<!--
To give you one simple example of an antipattern, let's look at an antipattern called Implicit Columns.

Let's say that we are working with a student information system and the student table has the structure shown on the screen. To test this table out, you might run as query like this. Select everything from student where ID is this. What we're focussing on here is this part: "select everything".

In this case it's fine, this wildcard was added for the convenience of performing ad-hoc queries for testing things out, for example in DBeaver, PGAdmin and other similar environments.
-->

---

<style scoped>
section {
  align-content: start;
}
</style>

# An example: _Implicit Columns_

| id         | name                                             | curr                               | email             | birth_date                                     | phone          |
| ---------- | ------------------------------------------------ | ---------------------------------- | ----------------- | ---------------------------------------------- | -------------- |
| 5434&nbsp; | Kristo&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; | IVSM&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; | krisbe@taltech.ee | 1998-07-18&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; | 57880010&nbsp; |

```java
var statement = connection.createStatement();
var resultSet = statement.executeQuery("SELECT * FROM student WHERE id = 5434");

if (resultSet.next()) {
    var name = resultSet.getString(2);
    var birthDate = resultSet.getString(5);

    System.out.println(name + " was born on " + birthDate);
}
```

> Kristo was born on 1998-07-18

<!--
However, people often also do this in production applications, mostly just out of convenience and laziness. Let's demonstrate this with a little Java snippet. Here we select this student again by ID. If it exists, which it does, we fetch its name and birth date by the positions of the columns, and we print out the result. Looks good.
-->

---

<style scoped>
section {
  align-content: start;
}
</style>

# An example: _Implicit Columns_

| id (1)     | name (2)                                         | curr (3)                           | email (4)         | birth_date (5)                                 | phone (6)      |
| ---------- | ------------------------------------------------ | ---------------------------------- | ----------------- | ---------------------------------------------- | -------------- |
| 5434&nbsp; | Kristo&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; | IVSM&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; | krisbe@taltech.ee | 1998-07-18&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; | 57880010&nbsp; |

```java
var statement = connection.createStatement();
var resultSet = statement.executeQuery("SELECT * FROM student WHERE id = 5434");

if (resultSet.next()) {
    var name = resultSet.getString(2);
    var birthDate = resultSet.getString(5);

    System.out.println(name + " was born on " + birthDate);
}
```

> Kristo was born on 1998-07-18

<!--
This works since each of the columns are implicitly assigned an position by the database. As you can see, name is at position two, and birth date is at position five. These positions are based on the order in which these columns are physically located on the disk for each row, or in other words, the order in which these columns were created.

But now, let's say that I'm refactoring this table. I want to normalise it a bit more, moving curriculums into a separate table.
-->

---

<style scoped>
section {
  align-content: start;
}
</style>

# An example: _Implicit Columns_

| id (1)     | name (2)                                         | email (3)         | birth_date (4)                                 | phone (5)      | curr_id (6) |
| ---------- | ------------------------------------------------ | ----------------- | ---------------------------------------------- | -------------- | ----------- |
| 5434&nbsp; | Kristo&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; | krisbe@taltech.ee | 1998-07-18&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; | 57880010&nbsp; | 264         |

```java
var statement = connection.createStatement();
var resultSet = statement.executeQuery("SELECT * FROM student WHERE id = 5434");

if (resultSet.next()) {
    var name = resultSet.getString(2);
    var birthDate = resultSet.getString(5);

    System.out.println(name + " was born on " + birthDate);
}
```

> Kristo was born on 57880010

<!--
I would need to create a new column, referring to the new table, which is added to the end. And I would need to drop the curriculum column, previously at position three.

And now, the position for each column after that changes, and suddenly, as far as the application is concerned, my birth date is now my phone number. The code that was previously working, is now displaying an incorrect result. This is clearly degrading the maintainability of the application.

I know that fetching columns by their positions is not that common these days, but this same thing also applies to inserting rows without specifying the list of column names that are being filled.

And there is another downside to this. The application only requires the name and birth date. However, it is currently selecting four other values, which causes extra work for the database. In this simple example, this mostly manifests in more data being sent over the network to the application. With larger columns like long texts or binary objects, the database might actually have to load more data from the disk. If joins were involved, some joins methods would be slower because of this.

One study on mobile application databases found that fixing this antipattern improved the performance of queries by more than 25 percent and also lowered the power usage of them by more than 25 percent. And the fix, really, is quite simple.
-->

---

# An example: _**Explicit** Columns_

| id (1)     | name (2)                                         | email (3)         | birth_date (4)                                 | phone (5)      | curr_id (6) |
| ---------- | ------------------------------------------------ | ----------------- | ---------------------------------------------- | -------------- | ----------- |
| 5434&nbsp; | Kristo&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; | krisbe@taltech.ee | 1998-07-18&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; | 57880010&nbsp; | 264         |

```java
var statement = connection.createStatement();
var resultSet = statement.executeQuery("SELECT name, birth_date FROM student WHERE id = 5434");

if (resultSet.next()) {
    var name = resultSet.getString(1);
    var birthDate = resultSet.getString(2);

    System.out.println(name + " was born on " + birthDate);
}
```

> Kristo was born on 1998-07-18

<!--
Just specify the damn column names.
-->

---

# What is jOOQ?

- Open-source Java library for interacting with SQL databases
- Highly popular, especially in industrial projects
- Integrates SQL into Java as a domain-specific language
- Leverages code generation heavily
- Provides strong type safety

<!--
And now, what about jOOQ? jOOQ, which stands for Java Object Oriented Querying, is an open-source library for performing database interactions in Java. Compared to Object-Relational Mapping tools or ORMs, it offers much more flexibility, since it doesn't abstract away SQL, but rather integrates SQL into Java as an embedded domain-specific language or DSL.

This makes it very popular in industrial projects, and it is often preferred for the most complex and critical use cases, where ORMs are not able to perform efficiently, or even at all.

It is also highly type safe, and to achieve that, it leverages code generation heavily.
-->

---

# _Implicit Columns_ in jOOQ

```java
var student = dslContext.select(DSL.asterisk())
    .from(STUDENT)
    .where(STUDENT.ID.eq(5434))
    .fetchOne();

var name = student.getName();
var birthDate = student.getBirthDate();

System.out.println(name + " was born on " + birthDate);
```

<!--
To illustrate that, here's the code from before, but using jOOQ. Everything is represented using the jOOQ DSL, no plain SQL in sight. No magic strings, everything is type safe. The types of the fields here, name and birth date, are determined by the jOOQ code generator by looking at the actual database schema, the types of the columns, and generating code based on that.
-->

---

# SQL Antipatterns in Java

- Prevalent in Java projects
  - _Implicit Columns_ affects every 50th query
- Remain unfixed longer than traditional code smells
- Two tools capable of statically detecting SQL Antipatterns in Java
  - None capable of detecting from jOOQ DSL

<!--
How do these two things fit together? SQL antipatterns in Java have been researched to an extent. As mentioned, there has been a study on how much SQL antipatterns impact the performance and power consumption of mobile applications. There has also been research on how prevalent they are in Java projects. The results of this have been quite interesting. For example, the Implicit Columns antipattern impacts two queries out of every one hundred, which is quite a significant number. Also, SQL antipatterns tend to remain unfixed for a longer time than traditional Java code smells, and often they never get fixed at all.

The authors of that study speculate that the reason for these results could be that developers aren't aware of SQL antipatterns, or they don't see fixing them as high enough priority. Both of these reasons could be due to a lack of tooling that could detect them early in the development, when fixing them is still a trivial effort.

A couple of tools have been developed to statically detect SQL antipatterns in Java code. They seem to work just fine, but for us, they have one large limitation. They are designed to find plain SQL queries in the Java source code, and then perform the detection on those. But as previously shown, most queries in projects using jOOQ are created using the jOOQ DSL, therefore they aren't in the form of plain SQL in the source code, and such detectors can not do anything in this case.
-->

---

# Research Goal

- Development of tools for detecting SQL antipatterns in database access code using jOOQ
  - Different paradigms: static analysis and language models
- Studying the prevalence of SQL antipatterns in jOOQ

<!--
Therefore, as SQL antipatterns can have a large negative impact, they are found to be prevalent in Java applications, and they can also occur in applications using jOOQ, there is a need for a detection tool for that.

Hence, the main goal of this Master's thesis is the development of tools for detecting SQL antipatterns in database access code using jOOQ. We will be exploring two different paradigms for that, traditional static analysis and language models, and seeing, which one of them is more suitable for the task in which circumstances.

Another, secondary goal is to study the prevalence of SQL antipatterns in projects using jOOQ to see how it compares to projects using plain SQL. This can be useful both to developers looking to avoid indulging in antipatterns, as well as the maintainers of jOOQ to make changes in their part, for example improve the documentation of dangerous APIs or deprecate them in favour of safer ones.

To achieve this, I haveprepared five research questions, which I will go through next.
-->

---

# Research Questions: Prevalence

- **RQ1:** How prevalent are SQL antipatterns in existing Java projects that use jOOQ?
- **RQ2:** How can SQL antipatterns manifest in Java projects that use jOOQ?

<!--
First of all, we have two questions about the prevalence of SQL antipatterns. These arise from the fact that SQL antipatterns in jOOQ are an unstudied area and there are no references on how SQL antipatterns manifest in jOOQ. And to be able to detect them, we need that knowledge, so we produce the reference ourselves.

The two research questions are:
- How prevalent are SQL antipatterns in existing Java projects that use jOOQ?
and:
- How can SQL antipatterns manifest in Java projects that use jOOQ?
-->

---

# Research Questions: Detection

- **RQ3:** How accurate are language models in detecting SQL antipatterns in Java projects that use jOOQ?
- **RQ4:** How accurate is traditional static analysis in detecting SQL antipatterns in Java projects that use jOOQ?
- **RQ5:** What is the best way to detect SQL antipatterns in Java projects that use jOOQ?

<!--
And once we know how they occur in jOOQ, we can start detecting them. We will build two SQL antipattern detection tools for jOOQ. One of them will utilise language models, and the other one traditional static analysis based on a pre-existing static analysis framework. We evaluate the results both quantitatively and qualitatively.

The questions answered by quantitative evaluation are:
- How accurate are language models in detecting SQL antipatterns in Java projects that use jOOQ?
and:
- How accurate is traditional static analysis in detecting SQL antipatterns in Java projects that use jOOQ?

And the question answered by qualitative evaluation is:
- What is the best way to detect SQL antipatterns in Java projects that use jOOQ?

The answer to that question will not be a simple this or that, but rather a framework for picking the most suitable method based on the requirements. When we were considering which method to use, we realized that inevitably there will be a question on why not the other one, and trying to answer that made us realize that both methods will likely produce competent results and it depends more on what the specific user values and this way we can have a more nuanced answer to that question.
-->

---

# Thank you for listening!

<!--
Thank you for listening. I'm looking forward to your questions.
-->
